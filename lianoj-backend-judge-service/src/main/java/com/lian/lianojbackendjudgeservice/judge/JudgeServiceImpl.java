package com.lian.lianojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lian.lianojbackendjudgeservice.judge.strategy.JudgeContext;
import com.lian.lianojbackendjudgeservice.judge.strategy.JudgeManager;
import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;
import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import com.lian.lianojbackendmodel.model.vo.enums.JudgeInfoMessageEnum;
import com.lian.lianojbackendmodel.model.vo.enums.QuestionSubmitStatusEnum;
import com.lian.lianojbackendserviceclient.service.QuestionFeignClient;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author lian
 * @title JudgeServiceImpl
 * @date 2025/1/14 10:28
 * @description 判题服务实现
 *
 * <p>并发与可靠性设计（E01/E07/E08）：
 * <ul>
 *   <li>分布式锁（E08）：doJudge 全程持有"提交id"维度的 Redisson 可重入锁，
 *       多实例部署时同一提交同时只有一个实例在判（锁防并发）；</li>
 *   <li>状态机抢占（E01）：锁内仍执行 grabJudgeStatus 条件更新（锁防并发、状态机防重复，双保险）；</li>
 *   <li>僵尸恢复（E07）：定时任务扫描卡在"判题中"的提交，条件重置后重新入队；</li>
 *   <li>管理端重判（E07）：已完成（成功/失败）的提交可由管理员重新入队。</li>
 * </ul>
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    /** 判题消息的交换机与路由键（与 question-service 的 MyMessageProducer 保持一致） */
    private static final String CODE_EXCHANGE = "code_exchange";

    private static final String CODE_ROUTING_KEY = "my_routingKey";

    /** 分布式锁等待时间：0 表示不等待，抢不到锁立即失败（该提交已被其他实例处理） */
    private static final long LOCK_WAIT_SECONDS = 0L;

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${codesandbox.type:example}")
    private String type;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        RLock lock = redissonClient.getLock("judge:lock:" + questionSubmitId);
        boolean locked = false;
        try {
            // E08 分布式锁：不等待（抢不到立即放弃）。
            // 注意必须用"两参 tryLock"（waitTime, unit）——它不指定 leaseTime，
            // Redisson 看门狗才会启动（默认 30 秒租约、每 10 秒自动续期），
            // 判题超过 30 秒时锁不会静默过期；三参 tryLock 显式传 leaseTime 会禁用看门狗。
            try {
                locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题加锁被中断");
            }
            if (!locked) {
                // 锁被其他实例持有 = 该提交正在被并行处理，本条消息按重复消息丢弃
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该提交正在被其他判题实例处理");
            }
            return doJudgeInternal(questionSubmitId);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();   // 只释放自己持有的锁（isHeldByCurrentThread 防误删）
            }
        }
    }

    /**
     * 判题内部流程（必须在持有分布式锁的前提下调用）
     */
    private QuestionSubmit doJudgeInternal(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目信息不存在");
        }
        // 2）抢占判题权（幂等核心）：
        //    条件更新 status（仅"等待中"→"判题中"），以影响行数判定是否抢占成功。
        //    MQ 至少一次投递可能让同一提交的消息到达多次，重复消息在此处必然抢占失败，
        //    直接抛出异常由消费者 nack 丢弃，从源头防止重复判题、重复计数。
        boolean grabbed = questionFeignClient.grabJudgeStatus(questionSubmitId);
        if (!grabbed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中或已完成");
        }
        // 3）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = questionSubmit.getCode();
        String language = questionSubmit.getLanguage();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class); // JSON字符串转对象
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList()); // 获取输入用例
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder() // 构建执行代码请求
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest); // 调用沙箱执行代码
        // 4）沙箱错误分支（E09 语义细分）：
        //    status=2 沙箱内部错误（如命中黑名单）→ "危险操作"；
        //    status=4 编译失败 → "编译错误"。
        //    两者都没有可用的执行输出，直接置 FAILED 结束，不进入用例比对。
        Integer sandboxStatus = executeCodeResponse.getStatus();
        if (Integer.valueOf(ExecuteCodeResponse.STATUS_SANDBOX_ERROR).equals(sandboxStatus)) {
            log.warn("提交 {} 被沙箱拒绝: {}", questionSubmitId, executeCodeResponse.getMessage());
            return writeFailResult(questionSubmitId, JudgeInfoMessageEnum.DANGEROUS_OPERATION);
        }
        if (Integer.valueOf(ExecuteCodeResponse.STATUS_COMPILE_ERROR).equals(sandboxStatus)) {
            log.info("提交 {} 编译失败: {}", questionSubmitId, executeCodeResponse.getMessage());
            return writeFailResult(questionSubmitId, JudgeInfoMessageEnum.COMPILE_ERROR);
        }
        // 4b）用户代码超出时限：按 OJ 惯例判"超时"（status=SUCCEED + 文案），不计通过数、不进入比对
        if (Integer.valueOf(ExecuteCodeResponse.STATUS_TIME_LIMIT).equals(sandboxStatus)) {
            log.info("提交 {} 判定超时", questionSubmitId);
            QuestionSubmit tleUpdate = new QuestionSubmit();
            tleUpdate.setId(questionSubmitId);
            tleUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
            JudgeInfo tleInfo = new JudgeInfo();
            tleInfo.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            if (executeCodeResponse.getJudgeInfo() != null) {
                tleInfo.setTime(executeCodeResponse.getJudgeInfo().getTime());
                tleInfo.setMemory(executeCodeResponse.getJudgeInfo().getMemory());
            }
            tleUpdate.setJudgeInfo(JSONUtil.toJsonStr(tleInfo));
            boolean tleUpdateResult = questionFeignClient.updateQuestionSubmitById(tleUpdate);
            if (!tleUpdateResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提交信息失败");
            }
            return questionFeignClient.getQuestionSubmitById(questionSubmitId);
        }
        List<String> outputList = executeCodeResponse.getOutputList();  // 获取输出结果
        // 5）根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext(); // 判题上下文
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        // E10：把沙箱引用传入上下文——SPJ 特殊判题的比对器需要再次调用沙箱执行校验程序
        judgeContext.setCodeSandbox(codeSandbox);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext); // 执行判题
        // 6）修改数据库中的判题结果
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提交信息失败");
        }
        // 7）通过数原子自增：判定结果为 Accepted 才计数（提交数已在提交时计数）
        if (JudgeInfoMessageEnum.ACCEPTED.getValue().equals(judgeInfo.getMessage())) {
            questionFeignClient.increaseAcceptedNum(questionId);
        }
        // 8）返回判题结果
        return questionFeignClient.getQuestionSubmitById(questionSubmitId);
    }

    /**
     * 写回失败结果（沙箱错误/编译错误共用）：status=FAILED + 判定文案，随后返回最新提交
     */
    private QuestionSubmit writeFailResult(long questionSubmitId, JudgeInfoMessageEnum messageEnum) {
        QuestionSubmit failUpdate = new QuestionSubmit();
        failUpdate.setId(questionSubmitId);
        failUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        JudgeInfo failJudgeInfo = new JudgeInfo();
        failJudgeInfo.setMessage(messageEnum.getValue());
        failUpdate.setJudgeInfo(JSONUtil.toJsonStr(failJudgeInfo));
        boolean update = questionFeignClient.updateQuestionSubmitById(failUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提交信息失败");
        }
        return questionFeignClient.getQuestionSubmitById(questionSubmitId);
    }

    @Override
    public boolean rejudge(long questionSubmitId) {
        // 仅允许"已完成"（成功/失败）的提交重判；判题中的提交由僵尸恢复任务负责
        boolean reset = questionFeignClient.resetFinishedJudgeToWaiting(questionSubmitId);
        if (!reset) {
            // 条件更新影响行数=0：提交不存在或正处于"判题中"
            return false;
        }
        publishJudgeMessage(questionSubmitId);
        log.info("管理端重判: 提交 {} 已重新入队", questionSubmitId);
        return true;
    }

    @Override
    public int recoverZombieJudges(int beforeMinutes) {
        int recovered = 0;
        // ① 僵尸判题：卡在"判题中"且超时 → 条件重置回"等待中" → 重新入队
        List<Long> zombieIds = questionFeignClient.listZombieQuestionSubmitIds(beforeMinutes);
        if (zombieIds != null) {
            for (Long questionSubmitId : zombieIds) {
                // 条件重置（WHERE status=1 AND updateTime 超时）：多实例部署时只有一个任务实例能重置成功，
                //    且重置后 updateTime 刷新，即使提交被快速重新抢占也不会被重复扫描到
                boolean reset = questionFeignClient.resetZombieJudge(questionSubmitId, beforeMinutes);
                if (reset) {
                    publishJudgeMessage(questionSubmitId);
                    recovered++;
                    log.warn("僵尸判题恢复: 提交 {} 超过 {} 分钟无进展, 已重新入队", questionSubmitId, beforeMinutes);
                }
            }
        }
        // ② 等待中悬挂（E07 补强）：提交卡在"等待中"（消息投递失败/丢失）→ 直接重新入队。
        //    幂等安全：即使提交此刻恰好被并发处理，grabJudgeStatus 抢占语义也会正确拦截重复
        List<Long> waitingIds = questionFeignClient.listWaitingJudgeIds(beforeMinutes);
        if (waitingIds != null) {
            for (Long questionSubmitId : waitingIds) {
                publishJudgeMessage(questionSubmitId);
                recovered++;
                log.warn("等待中悬挂恢复: 提交 {} 超过 {} 分钟无消息, 已重新入队", questionSubmitId, beforeMinutes);
            }
        }
        return recovered;
    }

    /**
     * 判题消息重新投递（E07 重判/僵尸恢复共用）
     */
    private void publishJudgeMessage(long questionSubmitId) {
        try {
            rabbitTemplate.convertAndSend(CODE_EXCHANGE, CODE_ROUTING_KEY, String.valueOf(questionSubmitId));
        } catch (Exception e) {
            // 消息发布失败时提交已重置为"等待中"，不会悬死——可由下一次重判/人工兜底
            log.error("判题消息重新投递失败: questionSubmitId={}", questionSubmitId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题消息投递失败，请稍后重试");
        }
    }
}

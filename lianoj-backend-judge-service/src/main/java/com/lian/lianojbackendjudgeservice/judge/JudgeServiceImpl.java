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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lian
 * @title JudgeServiceImpl
 * @date 2025/1/14 10:28
 * @description 判题服务实现
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;


    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
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
        // 4）沙箱内部错误（如代码命中危险关键字黑名单）：
        //    此时没有可用的执行输出，直接判定为"危险操作"并结束，不进入用例比对。
        //    注意用 Integer.equals 比较（status 可能缺省为 null，== 拆箱会 NPE）
        if (Integer.valueOf(ExecuteCodeResponse.STATUS_SANDBOX_ERROR).equals(executeCodeResponse.getStatus())) {
            log.warn("提交 {} 被沙箱拒绝: {}", questionSubmitId, executeCodeResponse.getMessage());
            QuestionSubmit dangerUpdate = new QuestionSubmit();
            dangerUpdate.setId(questionSubmitId);
            dangerUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            JudgeInfo dangerJudgeInfo = new JudgeInfo();
            dangerJudgeInfo.setMessage(JudgeInfoMessageEnum.DANGEROUS_OPERATION.getValue());
            dangerUpdate.setJudgeInfo(JSONUtil.toJsonStr(dangerJudgeInfo));
            boolean dangerUpdateResult = questionFeignClient.updateQuestionSubmitById(dangerUpdate);
            if (!dangerUpdateResult) {
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
        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        return questionSubmitResult;
    }
}

package com.lian.lianojbackendjudgeservice.judge.strategy;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;
import com.lian.lianojbackendmodel.model.dto.question.JudgeConfig;
import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import com.lian.lianojbackendmodel.model.vo.enums.JudgeInfoMessageEnum;
import com.lian.lianojbackendjudgeservice.judge.strategy.comparator.ExactOutputComparator;
import com.lian.lianojbackendjudgeservice.judge.strategy.comparator.OutputComparator;
import com.lian.lianojbackendjudgeservice.judge.strategy.comparator.SpjOutputComparator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author lian
 * @title DefaultJudgeStrategy
 * @date 2025/1/14 10:50
 * @description 通用判题策略（E09/E10/E06 重构后，全语言唯一生效的策略）
 *
 * <p>E09 重构说明：语言差异（编译/运行方式）已下沉到沙箱侧 LanguageConfigRegistry，
 * 判题比对逻辑本与语言无关，因此原 Default/JavaLanguage 双策略收敛为本类一个通用策略；
 * JavaLanguageJudgeStrategy 保留为空实现以维持 JudgeManager 的分派扩展点（演进而非推倒）。
 *
 * <p>相对教程原版的三处增强：
 * <ol>
 *   <li>E06：memory 空安全（原生沙箱无内存统计，不再拆箱 NPE）；</li>
 *   <li>E09：语言时限系数——解释型语言（python）按 2.0 放宽时限判定，
 *       替代教程写死的"Java 扣除 500ms"经验值（保留该扣除，语义不变）；</li>
 *   <li>E10：比对器策略化（精确比对 / SPJ 特殊判题）+ ACM/OI 两种赛制计分。</li>
 * </ol>
 */
@Slf4j
public class DefaultJudgeStrategy implements JudgeStrategy {

    /** JVM 系语言的进程启动开销扣除（教程经验值，保留） */
    private static final long JVM_PROGRAM_TIME_COST = 500L;

    /**
     * 语言时限系数（E09，与沙箱侧 LanguageConfigRegistry.timeLimitFactor 对应）：
     * 解释型语言执行慢，题目时限按系数放宽后再比较
     */
    private static final Map<String, Double> LANGUAGE_TIME_FACTOR =
            new java.util.HashMap<String, Double>() {{
                put("python", 2.0);
            }};

    /** 判题配置默认值 */
    private static final String DEFAULT_SPJ_MODE = "standard";
    private static final String SPJ_MODE_SPECIAL = "special";
    private static final String DEFAULT_JUDGE_MODE = "acm";
    private static final String JUDGE_MODE_OI = "oi";

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        // 获取代码沙箱执行所得的判题信息（E06：memory 可能缺失，空安全兜底）
        JudgeInfo sandboxJudgeInfo = judgeContext.getJudgeInfo();
        long memory = Optional.ofNullable(sandboxJudgeInfo.getMemory()).orElse(0L);
        long time = Optional.ofNullable(sandboxJudgeInfo.getTime()).orElse(0L);
        List<String> inputList = judgeContext.getInputList();   // 获取题目的输入
        List<String> outputList = judgeContext.getOutputList(); // 获取代码沙箱执行的结果
        Question question = judgeContext.getQuestion();
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();

        // 判题配置容错：JSON 解析失败/字段缺失时按"无限制"处理，
        // 避免拆箱 NPE 使提交卡死并被僵尸任务无限重投（P1 审查修复）
        JudgeConfig judgeConfig = Optional.ofNullable(
                JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class)).orElseGet(JudgeConfig::new);
        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 沙箱输出数量与用例数不一致：用户程序中途崩溃/输出被截断 → Wrong Answer
        if (outputList.size() != inputList.size()) {
            return finish(judgeInfoResponse, JudgeInfoMessageEnum.WRONG_ANSWER,
                    judgeConfig, judgeCaseList, 0, false);
        }

        // E10 比对器策略化：standard=精确比对；special=SPJ 校验程序
        String spjMode = StrUtil.blankToDefault(judgeConfig.getSpjMode(), DEFAULT_SPJ_MODE);
        OutputComparator comparator = SPJ_MODE_SPECIAL.equals(spjMode)
                ? new SpjOutputComparator(judgeContext.getCodeSandbox(), question.getSpjCode())
                : new ExactOutputComparator();

        // E10 赛制：acm（默认）全对才算通过；oi 按用例分值累加部分得分
        String judgeMode = StrUtil.blankToDefault(judgeConfig.getJudgeMode(), DEFAULT_JUDGE_MODE);
        boolean scoringEnabled = judgeCaseList.stream().anyMatch(judgeCase -> judgeCase.getScore() != null);
        long totalScore = judgeCaseList.stream()
                .mapToLong(judgeCase -> Optional.ofNullable(judgeCase.getScore()).orElse(0L))
                .sum();

        long score = 0;
        boolean allPass = true;
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            boolean pass = comparator.accept(judgeCase, outputList.get(i));
            if (pass) {
                score += Optional.ofNullable(judgeCase.getScore()).orElse(0L);
            } else {
                allPass = false;
                // ACM 赛制首错即止（后续用例不再消耗资源比对）；OI 赛制继续累计部分分
                if (!JUDGE_MODE_OI.equals(judgeMode)) {
                    break;
                }
            }
        }

        if (!allPass) {
            // ACM：任一用例错即 0 分；OI：已通过用例的部分分
            return finish(judgeInfoResponse, JudgeInfoMessageEnum.WRONG_ANSWER,
                    judgeConfig, judgeCaseList, score, false);
        }

        // 资源限制判定（在比对之后：只有全部用例跑完，time/memory 统计才完整可信）；
        // 限制字段缺失（null）时跳过对应判定，避免拆箱 NPE。
        // 内存单位口径（R18 修订 R2）：judgeConfig.memoryLimit 单位是 **MB**（教程默认 1000
        // 即 1000MB，与前端做题页"内存限制 1000 MB"的展示一致）；沙箱上报的
        // judgeInfo.memory 单位是 KB（容器 memory_stats.usage/1024）——比较时先换算：
        // memory(KB) > memoryLimit(MB) × 1024 才判 MLE。
        // 若按"limit 直按 KB 解释"，默认值 1000=1MB 连 JVM 都装不下，
        // Docker 模式下所有 Java 提交必然 MLE（真机验证结论，1596KB>1000 即触发）。
        Long needMemoryLimit = judgeConfig.getMemoryLimit();
        Long needTimeLimit = judgeConfig.getTimeLimit();
        if (needMemoryLimit != null && memory > needMemoryLimit * 1024) {
            return finish(judgeInfoResponse, JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED,
                    judgeConfig, judgeCaseList, totalScore, true);
        }
        // E09 语言系数：解释型语言放宽时限；JVM 系语言扣除进程启动开销
        double factor = LANGUAGE_TIME_FACTOR.getOrDefault(questionSubmit.getLanguage(), 1.0);
        long effectiveTime = "java".equals(questionSubmit.getLanguage())
                ? Math.max(0L, time - JVM_PROGRAM_TIME_COST) : time;
        if (needTimeLimit != null && effectiveTime > needTimeLimit * factor) {
            return finish(judgeInfoResponse, JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED,
                    judgeConfig, judgeCaseList, totalScore, true);
        }

        // 通过上述判断，判题结果为正确
        return finish(judgeInfoResponse, JudgeInfoMessageEnum.ACCEPTED,
                judgeConfig, judgeCaseList, totalScore, true);
    }

    /**
     * 统一封装判题结果的组装：
     * message = 判定文案；score 按赛制与"是否启用了用例分值"回填（未启用计分时保持 null，兼容历史数据）
     *
     * @param allPass        是否全部用例通过（资源超限时该值应传 true，视为"答案本身正确"）
     * @param achievedScore  本次判题累计得分
     */
    private JudgeInfo finish(JudgeInfo judgeInfoResponse, JudgeInfoMessageEnum messageEnum,
                             JudgeConfig judgeConfig, List<JudgeCase> judgeCaseList,
                             long achievedScore, boolean allPass) {
        boolean scoringEnabled = judgeCaseList.stream().anyMatch(judgeCase -> judgeCase.getScore() != null);
        String judgeMode = StrUtil.blankToDefault(judgeConfig.getJudgeMode(), DEFAULT_JUDGE_MODE);
        judgeInfoResponse.setMessage(messageEnum.getValue());
        if (scoringEnabled) {
            long finalScore;
            if (messageEnum == JudgeInfoMessageEnum.ACCEPTED) {
                finalScore = achievedScore;                                  // 全对：满分
            } else if (JUDGE_MODE_OI.equals(judgeMode) && allPass) {
                finalScore = achievedScore;                                  // OI 资源超限：已有部分分
            } else if (JUDGE_MODE_OI.equals(judgeMode)) {
                finalScore = achievedScore;                                  // OI 答案错误：已通过用例的分
            } else {
                finalScore = 0;                                              // ACM 错误：0 分
            }
            judgeInfoResponse.setScore(finalScore);
        }
        return judgeInfoResponse;
    }
}

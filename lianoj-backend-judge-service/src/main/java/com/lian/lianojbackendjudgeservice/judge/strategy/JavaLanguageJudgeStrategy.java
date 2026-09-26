package com.lian.lianojbackendjudgeservice.judge.strategy;

/**
 * @author lian
 * @title JavaLanguageJudgeStrategy
 * @date 2025/1/14 10:50
 * @description Java 语言判题策略（E09 后已并入通用策略）
 *
 * <p>本类历史上承载"Java 特有判题逻辑"（空值兜底 + JVM 启动开销扣除）。
 * E09 重构后：空值兜底与 JVM 开销扣除已并入 {@link DefaultJudgeStrategy}（所有语言统一生效），
 * 语言差异的真正载体是沙箱侧的 LanguageConfigRegistry——本类保留为
 * JudgeManager 语言分派骨架的<b>扩展点示例</b>：未来确需"仅针对某语言"的比对差异时，
 * 在这里覆写对应逻辑即可，JudgeManager 的分派结构无需变动（演进而非推倒）。
 *
 * @author lian
 */
@Deprecated
public class JavaLanguageJudgeStrategy extends DefaultJudgeStrategy {
}

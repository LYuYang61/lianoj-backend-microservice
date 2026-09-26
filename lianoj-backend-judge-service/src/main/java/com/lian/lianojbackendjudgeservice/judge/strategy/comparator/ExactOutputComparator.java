package com.lian.lianojbackendjudgeservice.judge.strategy.comparator;

import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;

/**
 * 精确比对器（默认）：用户输出与期望输出完全一致才算通过
 *
 * <p>与教程原版逐条 equals 的行为保持一致（含行尾空白敏感的严格语义）。
 *
 * @author lian
 */
public class ExactOutputComparator implements OutputComparator {

    @Override
    public boolean accept(JudgeCase judgeCase, String actualOutput) {
        return judgeCase.getOutput().equals(actualOutput);
    }
}

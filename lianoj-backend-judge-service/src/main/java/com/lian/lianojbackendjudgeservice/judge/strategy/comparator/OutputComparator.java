package com.lian.lianojbackendjudgeservice.judge.strategy.comparator;

import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;

/**
 * 用例输出比对器（E10 判题域抽象的第二次策略化）
 *
 * <p>语言策略（DefaultJudgeStrategy）关心"哪种语言怎么跑"，
 * 比对器关心"一个用例算不算通过"——两者解耦后，精确比对与 SPJ 特殊判题
 * 可以在不改动判题主流程的情况下各自演进。
 *
 * @author lian
 */
public interface OutputComparator {

    /**
     * 判定单个用例是否通过
     *
     * @param judgeCase    题目用例（含期望输出；SPJ 模式下还承载用例输入）
     * @param actualOutput 沙箱执行得到的用户程序实际输出
     * @return true = 该用例通过
     */
    boolean accept(JudgeCase judgeCase, String actualOutput);
}

package com.lian.lianojbackendmodel.model.dto.question;

import lombok.Data;

/**
 * @author lian
 * @title JudgeCase
 * @date 2025/1/10 10:39
 * @description 题目用例
 */
@Data
public class JudgeCase {

        /**
        * 输入用例
        */
        private String input;

        /**
        * 输出用例
        */
        private String output;

        /**
        * 该用例分值（E10 OI 赛制用，可空）：
        * 未配置分值时按"全部通过=Accepted、任一错误=WA"的 ACM 语义处理；
        * 配置了任一用例分值即进入计分模式（JudgeInfo.score 返回总分）
        */
        private Long score;
}

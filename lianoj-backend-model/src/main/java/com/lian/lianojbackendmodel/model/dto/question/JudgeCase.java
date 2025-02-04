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
}

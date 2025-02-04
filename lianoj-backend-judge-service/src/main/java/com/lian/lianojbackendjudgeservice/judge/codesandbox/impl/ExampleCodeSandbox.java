package com.lian.lianojbackendjudgeservice.judge.codesandbox.impl;


import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.vo.enums.JudgeInfoMessageEnum;
import com.lian.lianojbackendmodel.model.vo.enums.QuestionSubmitStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author lian
 * @title ExampleCodeSandbox
 * @date 2025/1/13 23:05
 * @description 示例代码沙箱
 */
@Slf4j
public class ExampleCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(inputList);
        executeCodeResponse.setMessage("测试执行成功");
        executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
        judgeInfo.setMemory(1000L);
        judgeInfo.setTime(1000L);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }
}

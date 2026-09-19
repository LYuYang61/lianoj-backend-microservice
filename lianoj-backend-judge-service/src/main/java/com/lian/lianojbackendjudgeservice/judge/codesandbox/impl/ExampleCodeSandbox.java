package com.lian.lianojbackendjudgeservice.judge.codesandbox.impl;


import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.vo.enums.JudgeInfoMessageEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author lian
 * @title ExampleCodeSandbox
 * @date 2025/1/13 23:05
 * @description 示例代码沙箱（不做真实执行，直接回显输入，用于联调判题流程）
 */
@Slf4j
public class ExampleCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(inputList);
        executeCodeResponse.setMessage("测试执行成功");
        // 注意：必须使用沙箱执行状态常量（而非提交状态枚举）——
        // 两套枚举取值撞号（提交"成功"=2 与沙箱错误 STATUS_SANDBOX_ERROR=2）会导致
        // 判题服务把示例沙箱的正常结果误判为"沙箱内部错误"
        executeCodeResponse.setStatus(ExecuteCodeResponse.STATUS_SUCCESS);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getValue());
        judgeInfo.setMemory(1000L);
        judgeInfo.setTime(1000L);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }
}

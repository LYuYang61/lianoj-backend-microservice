package com.lian.lianojbackendjudgeservice.judge.codesandbox.impl;


import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lian
 * @title ThirdPartyCodeSandbox
 * @date 2025/1/13 23:06
 * @description 第三方代码沙箱
 */
@Slf4j
public class ThirdPartyCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("执行第三方代码沙箱");
        return new ExecuteCodeResponse();
    }
}

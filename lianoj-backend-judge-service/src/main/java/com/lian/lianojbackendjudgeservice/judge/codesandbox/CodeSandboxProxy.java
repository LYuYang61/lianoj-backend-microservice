package com.lian.lianojbackendjudgeservice.judge.codesandbox;

import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lian
 * @title CodeSandboxProxy
 * @date 2025/1/13 23:22
 * @description 代码沙箱代理
 */
@Slf4j
public class CodeSandboxProxy implements CodeSandbox{
    private final CodeSandbox codeSandbox;


    public CodeSandboxProxy(CodeSandbox codeSandbox) {
        this.codeSandbox = codeSandbox;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("代码沙箱请求信息：" + executeCodeRequest.toString());
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        log.info("代码沙箱响应信息：" + executeCodeResponse.toString());
        return executeCodeResponse;
    }
}

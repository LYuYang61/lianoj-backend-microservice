package com.lian.lianojbackendjudgeservice.judge.codesandbox;


import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;

/**
 * @author lian
 * @title CodeSandbox
 * @date 2025/1/13 23:00
 * @description 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}

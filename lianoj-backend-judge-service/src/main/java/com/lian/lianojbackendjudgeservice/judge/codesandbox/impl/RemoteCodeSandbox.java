package com.lian.lianojbackendjudgeservice.judge.codesandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author lian
 * @title RemoteCodeSandbox
 * @date 2025/1/13 23:06
 * @description 远程代码沙箱
 */
@Slf4j
public class RemoteCodeSandbox implements CodeSandbox {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        String url = "http://localhost:8090/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest); // 将请求体转为json字符串
        String responseStr = HttpUtil.createPost(url)
                .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)   // 定义鉴权请求头和密钥
                .body(json)                                   // 设置请求体
                .execute()                                    // 执行请求
                .body();                                      // 获取响应体
        if (StringUtils.isBlank(responseStr)) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "executeCode remoteSandbox error, message = " + responseStr);
        }
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
    }
}

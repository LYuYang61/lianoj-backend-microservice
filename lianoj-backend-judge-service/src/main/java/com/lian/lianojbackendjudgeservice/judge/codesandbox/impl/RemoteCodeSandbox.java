package com.lian.lianojbackendjudgeservice.judge.codesandbox.impl;

import cn.hutool.crypto.SecureUtil;
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
 *
 * <p>鉴权协议（E02，与沙箱端 SignUtils 对应）：
 * 请求头 X-Timestamp = 毫秒时间戳；
 * 请求头 X-Sign      = md5( secretKey + "." + 请求体原文 + "." + timestamp )。
 * 注意必须对"即将发送的 JSON 字符串"计算签名并原样发送该字符串，
 * 保证签名与请求体字节级一致。
 */
@Slf4j
public class RemoteCodeSandbox implements CodeSandbox {

    /**
     * 共享密钥：与沙箱端 sandbox.auth.secretKey 保持一致
     * （后续接入配置中心统一管理，见总体计划 E17）
     */
    private static final String SIGN_SECRET = "secretKey";

    /**
     * 签名请求头名称（与沙箱端 SignUtils.HEADER_TIMESTAMP / HEADER_SIGN 保持一致；
     * 沙箱与判题服务无共享模块，协议常量双端各自定义，修改时必须同步）
     */
    private static final String HEADER_TIMESTAMP = "X-Timestamp";

    private static final String HEADER_SIGN = "X-Sign";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String url = "http://localhost:8090/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest); // 将请求体转为json字符串（签名即基于此原文）
        long timestamp = System.currentTimeMillis();
        String sign = SecureUtil.md5(SIGN_SECRET + "." + json + "." + timestamp);
        String responseStr = HttpUtil.createPost(url)
                .header(HEADER_TIMESTAMP, String.valueOf(timestamp))  // 时间戳（防重放窗口）
                .header(HEADER_SIGN, sign)                            // 请求签名（防伪造/防篡改）
                .body(json)                                           // 设置请求体（与签名内容一致）
                .execute()
                .body();
        if (StringUtils.isBlank(responseStr)) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "executeCode remoteSandbox error, message = " + responseStr);
        }
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
    }
}

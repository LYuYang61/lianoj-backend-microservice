package com.lian.lianojbackendjudgeservice.judge.codesandbox;


import com.lian.lianojbackendjudgeservice.judge.codesandbox.impl.ExampleCodeSandbox;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.impl.RemoteCodeSandbox;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.impl.ThirdPartyCodeSandbox;

/**
 * @author lian
 * @title CodeSandboxFactory
 * @date 2025/1/13 23:04
 * @description 代码沙箱工厂（根据字符串参数创建指定的代码沙箱实例）
 */
public class CodeSandboxFactory {

    public static CodeSandbox newInstance(String type) {
        switch (type) {
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                return new RemoteCodeSandbox();
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
            default:
                return new ExampleCodeSandbox();
        }
    }
}

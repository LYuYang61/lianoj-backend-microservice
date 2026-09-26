package com.lian.lianojbackendjudgeservice.judge.strategy.comparator;

import cn.hutool.core.util.StrUtil;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * SPJ 特殊判题比对器（E10）：由出题人编写的校验程序判定用例是否通过，
 * 解决"答案不唯一"类题目（浮点误差、构造题、多解输出）无法精确比对的问题。
 *
 * <p><b>SPJ 程序协议</b>（约定优先于配置，前后端文档同步）：
 * <ol>
 *   <li>SPJ 程序用 Java 编写，与用户代码一样在沙箱中隔离执行——出题人代码同样不可信；</li>
 *   <li>标准输入按固定分隔符承载三段数据（顺序固定）：
 *       {@code 测试输入 + "\n#EXPECTED#\n" + 期望输出 + "\n#ACTUAL#\n" + 用户输出}；</li>
 *   <li>程序向标准输出写 {@code 1} 表示该用例通过、{@code 0} 表示不通过（取首行判断）。</li>
 * </ol>
 *
 * <p>实现说明：复用判题服务已持有的沙箱引用执行 SPJ（一次沙箱调用对应一个用例），
 * 不需要新的传输通道；比对器本身无状态，判题服务每次判题时按需 new 出即可。
 *
 * @author lian
 */
@Slf4j
public class SpjOutputComparator implements OutputComparator {

    /** 三段数据的分隔符（选用了 OJ 数据中几乎不可能出现的字符串） */
    public static final String EXPECTED_MARK = "#EXPECTED#";

    public static final String ACTUAL_MARK = "#ACTUAL#";

    /** 通过标记：SPJ 程序 stdout 首行输出 1 表示通过 */
    private static final String ACCEPT_MARK = "1";

    private final CodeSandbox codeSandbox;

    private final String spjCode;

    public SpjOutputComparator(CodeSandbox codeSandbox, String spjCode) {
        this.codeSandbox = codeSandbox;
        this.spjCode = spjCode;
    }

    @Override
    public boolean accept(JudgeCase judgeCase, String actualOutput) {
        if (StrUtil.isBlank(spjCode)) {
            // 防御：spjMode=special 但题目未配置 SPJ 程序 → 退化为精确比对
            log.warn("题目启用 SPJ 模式但未配置 spjCode, 退化为精确比对");
            return judgeCase.getOutput().equals(actualOutput);
        }
        String stdin = judgeCase.getInput() + "\n"
                + EXPECTED_MARK + "\n" + judgeCase.getOutput() + "\n"
                + ACTUAL_MARK + "\n" + actualOutput;
        ExecuteCodeRequest request = ExecuteCodeRequest.builder()
                .code(spjCode)
                .language("java")
                .inputList(Collections.singletonList(stdin))
                .build();
        try {
            ExecuteCodeResponse response = codeSandbox.executeCode(request);
            // SPJ 执行出错（编译失败/超时/黑名单）一律视为"不通过"，且不能中断整个判题
            if (!Integer.valueOf(ExecuteCodeResponse.STATUS_SUCCESS).equals(response.getStatus())) {
                log.warn("SPJ 程序执行异常, 判定不通过: status={}, message={}",
                        response.getStatus(), response.getMessage());
                return false;
            }
            String firstLine = response.getOutputList() == null || response.getOutputList().isEmpty()
                    ? "" : response.getOutputList().get(0).trim();
            return ACCEPT_MARK.equals(firstLine);
        } catch (Exception e) {
            log.error("SPJ 比对器调用沙箱失败, 判定不通过: {}", e.getMessage());
            return false;
        }
    }
}

package com.lian.lianojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.lian.lianojbackendcommon.common.ErrorCode;
import com.lian.lianojbackendcommon.exception.BusinessException;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.lian.lianojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lian.lianojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lian.lianojbackendjudgeservice.judge.strategy.JudgeContext;
import com.lian.lianojbackendjudgeservice.judge.strategy.JudgeManager;
import com.lian.lianojbackendmodel.model.codesandbox.JudgeInfo;
import com.lian.lianojbackendmodel.model.dto.question.JudgeCase;
import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import com.lian.lianojbackendmodel.model.vo.enums.QuestionSubmitStatusEnum;
import com.lian.lianojbackendserviceclient.service.QuestionService;
import com.lian.lianojbackendserviceclient.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lian
 * @title JudgeServiceImpl
 * @date 2025/1/14 10:28
 * @description 判题服务实现
 */
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目信息不存在");
        }
        // 2）如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提交信息失败");
        }
        // 4）调用沙箱，获取到执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = questionSubmit.getCode();
        String language = questionSubmit.getLanguage();
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class); // JSON字符串转对象
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList()); // 获取输入用例
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder() // 构建执行代码请求
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest); // 调用沙箱执行代码
        List<String> outputList = executeCodeResponse.getOutputList();  // 获取输出结果
        // 5）根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext(); // 判题上下文
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext); // 执行判题
        // 6）修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提交信息失败");
        }
        // 7）返回判题结果
        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionSubmitId);
        return questionSubmitResult;
    }
}

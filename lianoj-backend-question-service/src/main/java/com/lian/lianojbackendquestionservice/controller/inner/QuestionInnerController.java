package com.lian.lianojbackendquestionservice.controller.inner;

import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import com.lian.lianojbackendquestionservice.service.QuestionService;
import com.lian.lianojbackendquestionservice.service.QuestionSubmitService;
import com.lian.lianojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author lian
 * @title QuestionInnerController
 * @date 2025/2/6 15:25
 * @description 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @GetMapping("/get/id")
    @Override
    public Question getQuestionById(@RequestParam("questionId") long questionId) {
        return questionService.getById(questionId);
    }

    @GetMapping("/question_submit/get/id")
    @Override
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId) {
        return questionSubmitService.getById(questionSubmitId);
    }

    @PostMapping("/question_submit/update")
    @Override
    public boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

    @PostMapping("/question_submit/grab")
    @Override
    public boolean grabJudgeStatus(@RequestParam("questionSubmitId") long questionSubmitId) {
        return questionSubmitService.grabJudgeStatus(questionSubmitId);
    }

    @PostMapping("/accepted/num/increase")
    @Override
    public boolean increaseAcceptedNum(@RequestParam("questionId") long questionId) {
        return questionService.increaseAcceptedNum(questionId);
    }

    @PostMapping("/question_submit/zombie/reset")
    @Override
    public boolean resetZombieJudge(@RequestParam("questionSubmitId") long questionSubmitId,
                                    @RequestParam("beforeMinutes") int beforeMinutes) {
        return questionSubmitService.resetZombieJudge(questionSubmitId, beforeMinutes);
    }

    @PostMapping("/question_submit/finished/reset")
    @Override
    public boolean resetFinishedJudgeToWaiting(@RequestParam("questionSubmitId") long questionSubmitId) {
        return questionSubmitService.resetFinishedJudgeToWaiting(questionSubmitId);
    }

    @GetMapping("/question_submit/zombie/list")
    @Override
    public java.util.List<Long> listZombieQuestionSubmitIds(@RequestParam("beforeMinutes") int beforeMinutes) {
        return questionSubmitService.listZombieQuestionSubmitIds(beforeMinutes);
    }

    @GetMapping("/question_submit/waiting/list")
    @Override
    public java.util.List<Long> listWaitingJudgeIds(@RequestParam("beforeMinutes") int beforeMinutes) {
        return questionSubmitService.listWaitingJudgeIds(beforeMinutes);
    }
}

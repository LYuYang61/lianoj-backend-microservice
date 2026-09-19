package com.lian.lianojbackendserviceclient.service;


import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
* @author Lenovo
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2025-01-09 10:50:49
*/
@FeignClient(name = "lianoj-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient{

    /**
     * 根据题目ID获取题目
     * @param questionId
     * @return
     */
    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    /**
     * 根据题目提交ID获取题目提交
     * @param questionSubmitId
     * @return
     */
    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId);

    /**
     * 更新题目提交
     * @param questionSubmit
     * @return
     */
    @PostMapping("/question_submit/update")
    boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

    /**
     * 抢占判题权（幂等核心）：
     * 条件更新 question_submit 的 status（仅当当前状态为"等待中"时置为"判题中"），
     * 以数据库影响行数判定是否抢占成功——MQ 重复投递时，第二条消息必然抢占失败，
     * 从源头防止同一提交被重复判题、重复计数。
     *
     * @param questionSubmitId 提交 id
     * @return true = 抢占成功（本次调用负责判题）；false = 已被处理（重复消息）
     */
    @PostMapping("/question_submit/grab")
    boolean grabJudgeStatus(@RequestParam("questionSubmitId") long questionSubmitId);

    /**
     * 题目通过数原子自增（acceptedNum = acceptedNum + 1）：
     * 判定结果为 Accepted 时由判题服务调用，
     * 原子性由 SQL 层（setSql 自增）保证，避免"读-改-写"并发丢失更新。
     *
     * @param questionId 题目 id
     * @return
     */
    @PostMapping("/accepted/num/increase")
    boolean increaseAcceptedNum(@RequestParam("questionId") long questionId);

}

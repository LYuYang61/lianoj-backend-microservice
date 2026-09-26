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

    /**
     * 僵尸判题重置（E07）：
     * 把"判题中(status=1)且超过 beforeMinutes 无进展"的提交条件重置为"等待中"。
     * 双重条件（status + updateTime）保证：多实例定时任务并发扫描时只有一个能重置成功；
     * 重置会刷新 updateTime，即使提交被快速重新抢占也不会被下一轮扫描误伤。
     *
     * @param questionSubmitId 提交 id
     * @param beforeMinutes    判题中最长容忍时长（分钟）
     */
    @PostMapping("/question_submit/zombie/reset")
    boolean resetZombieJudge(@RequestParam("questionSubmitId") long questionSubmitId,
                             @RequestParam("beforeMinutes") int beforeMinutes);

    /**
     * 已完成提交重置为"等待中"（E07 管理端重判）：
     * 仅允许 status ∈ {成功(2), 失败(3)} 的提交重置，判题中的提交不允许重复排队。
     *
     * @param questionSubmitId 提交 id
     */
    @PostMapping("/question_submit/finished/reset")
    boolean resetFinishedJudgeToWaiting(@RequestParam("questionSubmitId") long questionSubmitId);

    /**
     * 查询僵尸判题的提交 id 列表（E07）：
     * status = 判题中(1) 且 updateTime 早于 now - beforeMinutes 的提交。
     * 条件过滤在 SQL 层完成，避免定时任务拉取全量提交。
     *
     * @param beforeMinutes 判题中最长容忍时长（分钟）
     */
    @GetMapping("/question_submit/zombie/list")
    java.util.List<Long> listZombieQuestionSubmitIds(@RequestParam("beforeMinutes") int beforeMinutes);

    /**
     * 查询"等待中"悬挂的提交 id 列表（E07 补强）：
     * status = 等待中(0) 且 updateTime 早于 now - beforeMinutes ——
     * 典型成因是判题消息投递失败或消息丢失（提交已建队但永远等不到消费者）。
     * 该状态的提交重新投递消息是幂等安全的（grabJudgeStatus 会正确处理已被处理的情形）。
     *
     * @param beforeMinutes 等待中的最长容忍时长（分钟）
     */
    @GetMapping("/question_submit/waiting/list")
    java.util.List<Long> listWaitingJudgeIds(@RequestParam("beforeMinutes") int beforeMinutes);

}

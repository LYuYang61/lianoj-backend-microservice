package com.lian.lianojbackendquestionservice.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lian.lianojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.lian.lianojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;
import com.lian.lianojbackendmodel.model.entity.User;
import com.lian.lianojbackendmodel.model.vo.QuestionSubmitVO;

/**
* @author Lenovo
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2025-01-09 10:51:41
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {
    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest 题目提交信息
     * @param loginUser
     * @return
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser);

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUser
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser);

    /**
     * 抢占判题权（幂等核心）：
     * 仅当提交状态为"等待中"时才将其置为"判题中"，以影响行数判定结果。
     * 用于判题服务消费 MQ 消息时的防重——重复投递的消息只有第一次能抢占成功。
     *
     * @param questionSubmitId 提交 id
     * @return true = 抢占成功，调用方负责完成判题；false = 已被处理，应直接丢弃消息
     */
    boolean grabJudgeStatus(long questionSubmitId);

    /**
     * 僵尸判题重置（E07）：把"判题中且超过 beforeMinutes 无进展"的提交条件重置为"等待中"。
     * 条件含 status 与 updateTime 双重约束，天然防并发误重置。
     *
     * @param questionSubmitId 提交 id
     * @param beforeMinutes    判题中最长容忍时长（分钟）
     * @return true = 重置成功（调用方负责重新投递消息）
     */
    boolean resetZombieJudge(long questionSubmitId, int beforeMinutes);

    /**
     * 已完成提交重置为"等待中"（E07 管理端重判）：
     * 仅当状态为"成功/失败"时允许重置，"判题中"的提交不允许重复排队。
     *
     * @param questionSubmitId 提交 id
     * @return true = 重置成功（调用方负责重新投递消息）
     */
    boolean resetFinishedJudgeToWaiting(long questionSubmitId);

    /**
     * 查询僵尸判题的提交 id 列表（E07）：status=判题中 且 updateTime 早于 now-beforeMinutes。
     * 过滤在 SQL 层完成，定时任务只拿目标集合。
     *
     * @param beforeMinutes 判题中最长容忍时长（分钟）
     * @return 提交 id 列表
     */
    java.util.List<Long> listZombieQuestionSubmitIds(int beforeMinutes);

    /**
     * 查询"等待中"悬挂的提交 id 列表（E07 补强）：
     * status=等待中 且 updateTime 早于 now-beforeMinutes（判题消息丢失/投递失败的提交）。
     * 对这类提交重新投递消息是幂等安全的。
     *
     * @param beforeMinutes 等待中的最长容忍时长（分钟）
     * @return 提交 id 列表
     */
    java.util.List<Long> listWaitingJudgeIds(int beforeMinutes);

}

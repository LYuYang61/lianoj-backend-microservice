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

}

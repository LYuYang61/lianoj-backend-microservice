package com.lian.lianojbackendquestionservice.service;



import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lian.lianojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.lian.lianojbackendmodel.model.entity.Question;
import com.lian.lianojbackendmodel.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2025-01-09 10:50:49
*/
public interface QuestionService extends IService<Question> {
    /**
     * 校验
     *
     * @param question
     * @param add
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    /**
     * 提交数原子自增（submitNum = submitNum + 1）
     * 用户提交题目落库成功后调用，与判题结果无关（凡是有效提交都计数）
     *
     * @param questionId 题目 id
     * @return 是否更新成功
     */
    boolean increaseSubmitNum(long questionId);

    /**
     * 通过数原子自增（acceptedNum = acceptedNum + 1）
     * 判题结果为 Accepted 时由判题服务经 Feign 调用
     *
     * @param questionId 题目 id
     * @return 是否更新成功
     */
    boolean increaseAcceptedNum(long questionId);


}

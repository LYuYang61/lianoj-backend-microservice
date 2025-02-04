package com.lian.lianojbackendmodel.model.vo;

import cn.hutool.json.JSONUtil;
import com.lian.lianojbackendmodel.model.dto.question.JudgeConfig;
import com.lian.lianojbackendmodel.model.entity.Question;

import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author lian
 * @title QuestionVO
 * @date 2025/1/10 10:38
 * @description 题目封装类
 */
@Data
public class QuestionVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 题目提交数
     */
    private Integer submitNum;

    /**
     * 题目通过数
     */
    private Integer acceptedNum;

    /**
     * 判题配置（json 对象）
     */
    private JudgeConfig judgeConfig;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建题目人的信息
     */
    private UserVO userVO;

    /**
     * 包装类转对象
     *
     * @param questionVO
     * @return
     */
    public static Question voToObj(QuestionVO questionVO) {
        if (questionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionVO, question);   // 将questionVO的属性值拷贝到question中
        List<String> tagList = questionVO.getTags();      // 获取questionVO的tags属性值
        if (tagList != null) {
            question.setTags(JSONUtil.toJsonStr(tagList)); // 将tagList转为json字符串
        }
        JudgeConfig judgeConfig = questionVO.getJudgeConfig(); // 获取questionVO的judgeConfig属性值
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig)); // 将judgeConfig转为json字符串
        }
        return question;
    }

    /**
     * 对象转包装类
     *
     * @param question
     * @return
     */
    public static QuestionVO objToVo(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO questionVO = new QuestionVO();
        BeanUtils.copyProperties(question, questionVO); // 将question的属性值拷贝到questionVO中
        List<String> tagList = JSONUtil.toList(JSONUtil.parseArray(question.getTags()), String.class); // 将question的tags属性值转为List<String>
        questionVO.setTags(tagList);
        String judgeConfig = question.getJudgeConfig(); // 获取question的judgeConfig属性值
        if (judgeConfig != null) {
            questionVO.setJudgeConfig(JSONUtil.toBean(judgeConfig, JudgeConfig.class)); // 将judgeConfig转为JudgeConfig对象
        }
        return questionVO;
    }

    private static final long serialVersionUID = 1L;
}

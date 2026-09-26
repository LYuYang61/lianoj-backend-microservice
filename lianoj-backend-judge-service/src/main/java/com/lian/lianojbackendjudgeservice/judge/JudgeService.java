package com.lian.lianojbackendjudgeservice.judge;


import com.lian.lianojbackendmodel.model.entity.QuestionSubmit;

/**
 * @author lian
 * @title JudgeService
 * @date 2025/1/13 23:38
 * @description 判题服务
 */
public interface JudgeService {
    /**
     * 判题
     *
     * @param questionSubmitId
     * @return
     */
    QuestionSubmit doJudge(long questionSubmitId);

    /**
     * 管理端重判（E07）：把一次已完成的提交重新排队判题
     * 仅允许 status ∈ {成功, 失败} 的提交重判（判题中的提交不允许重复排队）；
     * 重置为"等待中"后重新投递 MQ 消息。
     *
     * @param questionSubmitId 提交 id
     * @return true = 已重置并重新入队
     */
    boolean rejudge(long questionSubmitId);

    /**
     * 僵尸判题恢复（E07）：定时任务调用。
     * 扫描"判题中(status=1)但超过 beforeMinutes 仍无进展"的提交，
     * 逐条条件重置为"等待中"并重新投递 MQ 消息。
     *
     * @param beforeMinutes 判题中状态的最长容忍时长（分钟）
     * @return 本次成功恢复并重新入队的数量
     */
    int recoverZombieJudges(int beforeMinutes);
}

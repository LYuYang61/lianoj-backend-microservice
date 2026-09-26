package com.lian.lianojbackendjudgeservice.task;

import com.lian.lianojbackendjudgeservice.judge.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author lian
 * @title ZombieJudgeRecoveryTask
 * @description 僵尸判题恢复任务（E07）
 *
 * <p>背景：判题中途宕机/重启时，提交会永久停留在"判题中(status=1)"，
 * 且对应 MQ 消息已被消费（即使 nack 也被丢弃）——没有外部干预就无法自愈。
 * 本任务周期性扫描这类"僵尸提交"，条件重置后重新入队。
 *
 * <p>设计要点：
 * <ul>
 *   <li>fixedDelay（而非 fixedRate）：上一轮跑完再计时，避免恢复慢时任务堆积；</li>
 *   <li>多实例安全：重置是条件 UPDATE（status=1 且 updateTime 超时），
 *       并发扫描只有一个实例能成功，重置成功才投递消息（与 E01 抢占语义同构）；</li>
 *   <li>initialDelay 60 秒：给应用启动和依赖就绪留出时间，避免启动瞬间误扫。</li>
 * </ul>
 */
@Component
@Slf4j
public class ZombieJudgeRecoveryTask {

    /** 判题中状态的最长容忍时长（分钟）：超过即判定为僵尸 */
    private static final int ZOMBIE_THRESHOLD_MINUTES = 10;

    @Resource
    private JudgeService judgeService;

    /**
     * 每 60 秒执行一轮僵尸扫描（fixedDelay：上一轮结束后再计时）
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void recover() {
        try {
            int recovered = judgeService.recoverZombieJudges(ZOMBIE_THRESHOLD_MINUTES);
            if (recovered > 0) {
                log.warn("本轮僵尸判题恢复完成: 恢复 {} 条提交", recovered);
            }
        } catch (Exception e) {
            // 定时任务吞掉异常：单轮失败不影响下一轮（日志告警供排查）
            log.error("僵尸判题恢复任务执行异常", e);
        }
    }
}

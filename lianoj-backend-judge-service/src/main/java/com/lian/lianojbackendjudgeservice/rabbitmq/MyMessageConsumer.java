package com.lian.lianojbackendjudgeservice.rabbitmq;

import com.lian.lianojbackendjudgeservice.judge.JudgeService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author lian
 * @title MyMessageConsumer
 * @date 2025/2/7 15:18
 * @description  消息消费者
 */
@Component
@Slf4j
public class MyMessageConsumer {

    @Resource
    private JudgeService judgeService;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        try {
            // 消息体必须是数字提交id；解析也纳入 try——非法报文走 nack 丢弃而非无限重投
            long questionSubmitId = Long.parseLong(message);
            judgeService.doJudge(questionSubmitId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("判题消息处理失败, message={}", message, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

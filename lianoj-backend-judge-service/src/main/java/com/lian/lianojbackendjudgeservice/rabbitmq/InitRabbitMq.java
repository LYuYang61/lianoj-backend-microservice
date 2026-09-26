package com.lian.lianojbackendjudgeservice.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lian
 * @title InitRabbitMq
 * @date 2025/2/7 15:12
 * @description 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
@Slf4j
public class InitRabbitMq {

    /** 判题消息拓扑 */
    private static final String EXCHANGE_NAME = "code_exchange";

    private static final String QUEUE_NAME = "code_queue";

    private static final String ROUTING_KEY = "my_routingKey";

    /** 死信拓扑（E07）：消费者 nack(requeue=false) 的消息路由到这里，供排查与人工重投 */
    private static final String DLX_EXCHANGE_NAME = "dlx.exchange";

    private static final String DLX_QUEUE_NAME = "dlx.queue";

    private static final String DLX_ROUTING_KEY = "dlx.routingKey";

    public static void doInit() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");   // 设置主机
            // 与 compose.yml 中创建的用户一致；不能用 guest，RabbitMQ 限制 guest 仅本机连接
            factory.setUsername("lian");
            factory.setPassword("123456");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();   // 创建通道

            // ① 死信拓扑（先于业务队列声明）：nack 丢弃的消息 → dlx.exchange → dlx.queue
            channel.exchangeDeclare(DLX_EXCHANGE_NAME, "direct", true);
            channel.queueDeclare(DLX_QUEUE_NAME, true, false, false, null);
            channel.queueBind(DLX_QUEUE_NAME, DLX_EXCHANGE_NAME, DLX_ROUTING_KEY);

            // ② 业务拓扑：code_queue 声明死信参数——
            //    被 nack(requeue=false) 的消息不再蒸发，而是转入死信队列
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");  // 创建交换机
            java.util.Map<String, Object> queueArgs = new java.util.HashMap<>();
            queueArgs.put("x-dead-letter-exchange", DLX_EXCHANGE_NAME);
            queueArgs.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
            channel.queueDeclare(QUEUE_NAME, true, false, false, queueArgs);  // 创建队列(持久化+死信)
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);   // 绑定队列
            log.info("消息队列启动成功(含死信拓扑)");
        } catch (Exception e) {
            // E07 审查修复：识别"队列参数冲突"（旧 code_queue 无死信参数时重复声明会 406）——
            // 此错误只记一行日志会被静默吞掉（服务照常启动但死信能力缺失），
            // 必须给出明确的自愈指引
            if (String.valueOf(e.getCause()).contains("PRECONDITION_FAILED")
                    || String.valueOf(e).contains("PRECONDITION_FAILED")
                    || (e.getMessage() != null && e.getMessage().contains("inequivalent arg"))) {
                log.error("【需人工处理】code_queue 已存在且参数与本次声明不一致(PRECONDITION_FAILED)。"
                        + "死信路由未生效！请执行: docker exec oj-rabbitmq rabbitmqctl delete_queue code_queue "
                        + "后重启本服务（队列会带死信参数自动重建）", e);
            } else {
                log.error("消息队列启动失败", e);
            }
        }
    }

    public static void main(String[] args) {
        doInit();
    }
}

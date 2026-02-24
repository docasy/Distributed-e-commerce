package com.ecommerce.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 */
@Slf4j
@Component
public class OrderMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时消息（延迟消息）
     * @param orderNo 订单号
     * @param delayMillis 延迟时间（毫秒）
     */
    public void sendOrderTimeoutMessage(String orderNo, long delayMillis) {
        try {
            MessageProperties properties = new MessageProperties();
            properties.setExpiration(String.valueOf(delayMillis));
            
            Message message = new Message(orderNo.getBytes(), properties);
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_TIMEOUT_EXCHANGE,
                    RabbitMQConfig.ORDER_TIMEOUT_ROUTING_KEY,
                    message
            );
            
            log.info("发送订单超时消息：orderNo={}, delay={}ms", orderNo, delayMillis);
        } catch (Exception e) {
            log.error("发送订单超时消息失败：{}", e.getMessage(), e);
        }
    }
}

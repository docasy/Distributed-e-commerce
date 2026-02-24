package com.ecommerce.order.mq;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单消息消费者
 */
@Slf4j
@Component
public class OrderMessageConsumer {

    @Autowired
    private OrderService orderService;

    /**
     * 处理订单超时消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_TIMEOUT_DLX_QUEUE)
    public void handleOrderTimeout(Message message, Channel channel) throws IOException {
        try {
            String orderNo = new String(message.getBody());
            log.info("收到订单超时消息：{}", orderNo);

            // 查询订单
            Order order = orderService.getOrderByOrderNo(orderNo);
            if (order == null) {
                log.warn("订单不存在：{}", orderNo);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 如果订单还是待支付状态，则自动取消
            if (order.getStatus() == 0) {
                orderService.cancelOrder(orderNo);
                log.info("订单超时自动取消：{}", orderNo);
            }

            // 确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("处理订单超时消息失败：{}", e.getMessage(), e);
            // 拒绝消息并重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}

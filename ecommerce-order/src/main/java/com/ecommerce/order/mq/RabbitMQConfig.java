package com.ecommerce.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // 订单超时队列
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String ORDER_TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    
    // 死信队列
    public static final String ORDER_TIMEOUT_DLX_QUEUE = "order.timeout.dlx.queue";
    public static final String ORDER_TIMEOUT_DLX_EXCHANGE = "order.timeout.dlx.exchange";
    public static final String ORDER_TIMEOUT_DLX_ROUTING_KEY = "order.timeout.dlx";

    /**
     * 订单超时队列（配置死信）
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_TIMEOUT_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_DLX_ROUTING_KEY)
                .build();
    }

    /**
     * 订单超时交换机
     */
    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(ORDER_TIMEOUT_EXCHANGE);
    }

    /**
     * 绑定
     */
    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(orderTimeoutExchange())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue orderTimeoutDlxQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_DLX_QUEUE).build();
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange orderTimeoutDlxExchange() {
        return new DirectExchange(ORDER_TIMEOUT_DLX_EXCHANGE);
    }

    /**
     * 死信绑定
     */
    @Bean
    public Binding orderTimeoutDlxBinding() {
        return BindingBuilder.bind(orderTimeoutDlxQueue())
                .to(orderTimeoutDlxExchange())
                .with(ORDER_TIMEOUT_DLX_ROUTING_KEY);
    }
}

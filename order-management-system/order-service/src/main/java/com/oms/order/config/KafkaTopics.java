package com.oms.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopics {

    private String orderCreated = "order-created";
    private String paymentCompleted = "payment-completed";
    private String paymentFailed = "payment-failed";

    public String getOrderCreated() {
        return orderCreated;
    }

    public void setOrderCreated(String orderCreated) {
        this.orderCreated = orderCreated;
    }

    public String getPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(String paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
    }

    public String getPaymentFailed() {
        return paymentFailed;
    }

    public void setPaymentFailed(String paymentFailed) {
        this.paymentFailed = paymentFailed;
    }
}

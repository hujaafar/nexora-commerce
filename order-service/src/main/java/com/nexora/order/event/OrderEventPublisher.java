/* File purpose: Publishes order changes for audit, notifications, and future integrations. */
package com.nexora.order.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.events.order-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderEvent event) {
        kafkaTemplate.send(topic, event.orderId(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOGGER.warn("Could not publish order event {}", event.type(), error);
                    }
                });
    }
}

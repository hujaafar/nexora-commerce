/*
 * File purpose: Publishes service lifecycle events to Kafka.
 */
package com.buy01.product.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
public class ProductEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ProductEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.events.product-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ProductEvent event) {
        kafkaTemplate.send(topic, event.productId(), event)
                .whenComplete((result, failure) -> {
                    if (failure == null) {
                        LOGGER.debug(
                                "Published {} for product {}",
                                event.type(),
                                event.productId());
                    } else {
                        LOGGER.error(
                                "Could not publish {} for product {}",
                                event.type(),
                                event.productId(),
                                failure);
                    }
                });
    }
}

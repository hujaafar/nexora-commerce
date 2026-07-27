/* BUY-01 learning header
 * File purpose: Publishes service lifecycle events to Kafka.
 * Learning focus: Asynchronous messaging, topic configuration, and loose service coupling.
 */
package com.buy01.product.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ProductEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
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

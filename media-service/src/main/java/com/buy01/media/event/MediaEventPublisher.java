/* BUY-01 learning header
 * File purpose: Publishes service lifecycle events to Kafka.
 * Learning focus: Asynchronous messaging, topic configuration, and loose service coupling.
 */
package com.buy01.media.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
public class MediaEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public MediaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.events.media-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(MediaEvent event) {
        kafkaTemplate.send(topic, event.mediaId(), event)
                .whenComplete((result, failure) -> {
                    if (failure == null) {
                        LOGGER.debug(
                                "Published {} for media {}",
                                event.type(),
                                event.mediaId());
                    } else {
                        LOGGER.error(
                                "Could not publish {} for media {}",
                                event.type(),
                                event.mediaId(),
                                failure);
                    }
                });
    }
}

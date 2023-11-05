package com.fypgrading.reviewservice.service;

import com.fypgrading.reviewservice.config.RabbitConfig;
import com.fypgrading.reviewservice.service.event.GradingSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.stereotype.Service;

@Service
public class EventDispatcher {

    private final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);
    private final SimpleMessageConverter converter = new SimpleMessageConverter();
    private final RabbitTemplate rabbitTemplate;

    public EventDispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGradingSubmitted(GradingSubmittedEvent event) {
        Message message = converter.toMessage(event, new MessageProperties());

        logger.info("Sending GradingSubmittedEvent to " + RabbitConfig.GRADES_QUEUE_NAME + ": " + message);

        rabbitTemplate.send(
                "",
                RabbitConfig.GRADES_QUEUE_NAME,
                message);
    }

    public void sendNotification(Integer teamId) {
        Message message = converter.toMessage(teamId, new MessageProperties());

        logger.info("Sending NotificationEvent to " + RabbitConfig.NOTIFICATION_QUEUE_NAME + ": " + message);

        rabbitTemplate.send(
                "",
                RabbitConfig.NOTIFICATION_QUEUE_NAME,
                message);
    }
}

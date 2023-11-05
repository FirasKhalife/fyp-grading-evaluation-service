package com.fypgrading.reviewservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fypgrading.reviewservice.config.RabbitConfig;
import com.fypgrading.reviewservice.exceptions.RabbitException;
import com.fypgrading.reviewservice.service.event.GradingSubmittedEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class EventHandler implements ChannelAwareMessageListener {

    private final Logger logger = LoggerFactory.getLogger(EventHandler.class);
    private final RabbitTemplate rabbitTemplate;
    private final GradingService gradingService;
    private final ObjectMapper objectMapper;

    public EventHandler(ObjectMapper objectMapper, RabbitTemplate rabbitTemplate, GradingService gradingService) {
        this.rabbitTemplate = rabbitTemplate;
        this.gradingService = gradingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, Channel channel) {
        byte[] body = message.getBody();
        GradingSubmittedEvent event;
        String messageBody = new String(body);
        logger.info("Grading Submitted event received: " + messageBody);

        try {
            event = objectMapper.readValue(messageBody, GradingSubmittedEvent.class);
            gradingService.searchForReviewers(event);
        }
        catch (Exception ex) {
            RabbitException exception = new RabbitException("Exception raised!", message, channel);
            sendNack(exception);
        }
    }

    private void sendNack(RabbitException ex) {
        Message message = ex.getQueueMessage();
        MessageProperties props = message.getMessageProperties();
        List<Map<String, ?>> xDeathHeader = props.getXDeathHeader();

        try {
            if (xDeathHeader == null || xDeathHeader.isEmpty() ||
                    (Long) xDeathHeader.get(0).get("count") < RabbitConfig.RETRY_GRADES_MAX_COUNT) {
                ex.getChannel().basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                return;
            }
            // if retryCount == RabbitConfig.RETRY_GRADES_MAX_COUNT, send to dead letter queue
            logger.warn("Message sent to dead letter queue: " + message);
            rabbitTemplate.send("", RabbitConfig.DEAD_QUEUE_NAME, message);
        }
        catch (IOException e) {
            logger.error("Error sending nack: " + e.getMessage());
        }
    }
}

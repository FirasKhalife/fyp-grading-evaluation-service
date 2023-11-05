package com.fypgrading.reviewservice.exceptions;

import com.rabbitmq.client.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.amqp.core.Message;

@Data
@EqualsAndHashCode(callSuper = true)
public class RabbitException extends RuntimeException {

    private Message queueMessage;

    private Channel channel;

    public RabbitException(String message, Message queueMessage, Channel channel) {
        super(message);
        this.queueMessage = queueMessage;
        this.channel = channel;
    }
}


//package com.fypgrading.reviewservice.config;
//
//import com.fypgrading.reviewservice.service.EventHandler;
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitAdmin;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
//import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitConfig {
//
//    /**
//     * Notification Queue
//     */
//    public static String NOTIFICATION_QUEUE_NAME;
//    public static String NOTIFICATION_EXCHANGE_NAME;
//    public static String NOTIFICATION_ROUTING_KEY;
//
//    @Value("${spring.rabbitmq.notification.queue}")
//    public void setNotificationQueueName(String notificationsQueueName) {
//        NOTIFICATION_QUEUE_NAME = notificationsQueueName;
//    }
//
//    @Value("${spring.rabbitmq.notification.exchange}")
//    public void setNotificationExchangeName(String notificationExchangeName) {
//        NOTIFICATION_EXCHANGE_NAME = notificationExchangeName;
//    }
//
//    @Value("${spring.rabbitmq.notification.routing-key}")
//    public void setNotificationRoutingKey(String notificationRoutingKey) {
//        NOTIFICATION_ROUTING_KEY = notificationRoutingKey;
//    }
//
//    @Bean
//    public Queue notificationQueue(@Value("${spring.rabbitmq.retry-notification.exchange}") String retryNotificationExchangeName,
//                                   @Value("${spring.rabbitmq.retry-notification.routing-key}") String retryNotificationRoutingKey) {
//        return QueueBuilder.durable(NOTIFICATION_QUEUE_NAME)
//                .withArgument("x-dead-letter-exchange", retryNotificationExchangeName)
//                .withArgument("x-dead-letter-routing-key", retryNotificationRoutingKey)
//                .build();
//    }
//
//    @Bean
//    public TopicExchange notificationExchange() {
//        return new TopicExchange(NOTIFICATION_EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Binding bindingNotification(@Qualifier("notificationQueue") Queue queue,
//                                       @Qualifier("notificationExchange") TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(NOTIFICATION_ROUTING_KEY);
//    }
//
//    /**
//     * Grades Queue
//     */
//    public static String GRADES_QUEUE_NAME;
//    public static String GRADES_EXCHANGE_NAME;
//    public static String GRADES_ROUTING_KEY;
//
//    @Value("${spring.rabbitmq.grades.queue}")
//    public void setGradesQueueName(String notificationQueueName) {
//        GRADES_QUEUE_NAME = notificationQueueName;
//    }
//
//    @Value("${spring.rabbitmq.grades.exchange}")
//    public void setGradesExchangeName(String gradesExchangeName) {
//        GRADES_EXCHANGE_NAME = gradesExchangeName;
//    }
//
//    @Value("${spring.rabbitmq.grades.routing-key}")
//    public void setGradesRoutingKey(String gradesRoutingKey) {
//        GRADES_ROUTING_KEY = gradesRoutingKey;
//    }
//
//    @Bean
//    public Queue gradesQueue() {
//        return QueueBuilder.durable(GRADES_QUEUE_NAME)
//                .withArgument("x-dead-letter-exchange", RETRY_GRADES_EXCHANGE_NAME)
//                .withArgument("x-dead-letter-routing-key", RETRY_GRADES_ROUTING_KEY)
//                .build();
//    }
//
//    @Bean
//    public TopicExchange gradesExchange() {
//        return new TopicExchange(GRADES_EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Binding bindingGrades(@Qualifier("gradesQueue") Queue queue,
//                                 @Qualifier("gradesExchange") TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(GRADES_ROUTING_KEY);
//    }
//
//    /**
//     * Retry Grades Queue
//     * In case of processing failure in the Grades Queue, the message will be sent to the Retry Grades Queue where it will stay for a certain amount of time (RETRY_GRADES_TTL) before being sent back to the Grades Queue
//     */
//    public static String RETRY_GRADES_QUEUE_NAME;
//    public static String RETRY_GRADES_EXCHANGE_NAME;
//    public static String RETRY_GRADES_ROUTING_KEY;
//    public static Integer RETRY_GRADES_TTL;
//    public static Integer RETRY_GRADES_MAX_COUNT;
//
//    @Value("${spring.rabbitmq.retry-grades.queue}")
//    public void setRetryGradesQueueName(String retryGradesQueueName) {
//        RETRY_GRADES_QUEUE_NAME = retryGradesQueueName;
//    }
//
//    @Value("${spring.rabbitmq.retry-grades.exchange}")
//    public void setRetryGradesExchangeName(String retryGradesExchangeName) {
//        RETRY_GRADES_EXCHANGE_NAME = retryGradesExchangeName;
//    }
//
//    @Value("${spring.rabbitmq.retry-grades.routing-key}")
//    public void setRetryGradesRoutingKey(String retryGradesRoutingKey) {
//        RETRY_GRADES_ROUTING_KEY = retryGradesRoutingKey;
//    }
//
//    @Value("${spring.rabbitmq.retry-grades.ttl}")
//    public void setRetryGradesTTL(Integer retryGradesTTL) {
//        RETRY_GRADES_TTL = retryGradesTTL;
//    }
//
//    @Value("${spring.rabbitmq.retry-grades.max-count}")
//    public void setRetryGradesMaxCount(Integer retryGradesMaxCount) {
//        RETRY_GRADES_MAX_COUNT = retryGradesMaxCount;
//    }
//
//    @Bean
//    Queue retryGradesQueue() {
//        return QueueBuilder.durable(RETRY_GRADES_QUEUE_NAME)
//                .withArgument("x-message-ttl", RETRY_GRADES_TTL)
//                .withArgument("x-dead-letter-exchange", GRADES_EXCHANGE_NAME)
//                .withArgument("x-dead-letter-routing-key", GRADES_ROUTING_KEY)
//                .build();
//    }
//
//    @Bean
//    public TopicExchange retryGradesExchange() {
//        return new TopicExchange(RETRY_GRADES_EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Binding bindingRetryGrades(@Qualifier("retryGradesQueue") Queue queue,
//                                            @Qualifier("retryGradesExchange") TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(RETRY_GRADES_ROUTING_KEY);
//    }
//
//
//    /**
//     * Dead Letter Queue
//     * After a certain count of retries, the message will be sent to the Dead Letter Queue
//     */
//    public static String DEAD_QUEUE_NAME;
//    public static String DEAD_EXCHANGE_NAME;
//    public static String DEAD_ROUTING_KEY;
//
//    @Value("${spring.rabbitmq.dead-letter.queue}")
//    public void setDeadQueueName(String deadQueueName) {
//        DEAD_QUEUE_NAME = deadQueueName;
//    }
//
//    @Value("${spring.rabbitmq.dead-letter.exchange}")
//    public void setDeadExchangeName(String deadExchangeName) {
//        DEAD_EXCHANGE_NAME = deadExchangeName;
//    }
//
//    @Value("${spring.rabbitmq.dead-letter.routing-key}")
//    public void setDeadRoutingKey(String deadRoutingKey) {
//        DEAD_ROUTING_KEY = deadRoutingKey;
//    }
//
//    @Bean
//    Queue deadLetterQueue() {
//        return QueueBuilder.durable(DEAD_QUEUE_NAME).build();
//    }
//
//    @Bean
//    public TopicExchange deadLetterExchange() {
//        return new TopicExchange(DEAD_EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Binding bindingDeadLetter(@Qualifier("deadLetterQueue") Queue queue,
//                                     @Qualifier("deadLetterExchange") TopicExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(DEAD_ROUTING_KEY);
//    }
//
//    /**
//     * RabbitMQ Listener
//     */
//    @Bean
//    MessageListenerAdapter messageListenerAdapter(EventHandler eventHandler) {
//        MessageListenerAdapter messageListenerAdapter =
//                new MessageListenerAdapter(eventHandler, "onMessage");
//        messageListenerAdapter.setMessageConverter(jackson2JsonMessageConverter());
//        return messageListenerAdapter;
//    }
//
//    @Bean
//    SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory,
//                                                                  MessageListenerAdapter messageListenerAdapter) {
//        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
//        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
//        simpleMessageListenerContainer.setQueueNames(NOTIFICATION_QUEUE_NAME);
//        simpleMessageListenerContainer.setMessageListener(messageListenerAdapter);
//        return simpleMessageListenerContainer;
//    }
//
//    /**
//     * RabbitMQ Config
//     */
//    @Bean
//    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
//        rabbitAdmin.setAutoStartup(true);
//        return rabbitAdmin;
//    }
//
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
//        return rabbitTemplate;
//    }
//
//    @Bean
//    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//}

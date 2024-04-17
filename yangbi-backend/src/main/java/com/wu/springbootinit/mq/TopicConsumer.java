package com.wu.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class TopicConsumer {

  private static final String EXCHANGE_NAME = "topic_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "topic");

      // 创建队列-1
      String queueName1 = "frontend-test";
      channel.queueDeclare(queueName1, true, false, false, null);
      channel.queueBind(queueName1, EXCHANGE_NAME, "#.前端.#");

      // 创建队列-2
      String queueName2 = "backend-test";
      channel.queueDeclare(queueName2, true, false, false, null);
      channel.queueBind(queueName2, EXCHANGE_NAME, "#.后端.#");

      // 创建队列-3
      String queueName3 = "product-test";
      channel.queueDeclare(queueName3, true, false, false, null);
      channel.queueBind(queueName3, EXCHANGE_NAME, "#.产品.#");

      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

      // 处理从队列接收的消息
      DeliverCallback direct1DeliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [direct1] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
      DeliverCallback direct2DeliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [direct2] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
      DeliverCallback direct3DeliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [direct3] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };

      // 消费队列的消息
      channel.basicConsume(queueName1, true, direct1DeliverCallback, consumerTag -> {
      });
      channel.basicConsume(queueName2, true, direct2DeliverCallback, consumerTag -> {
      });
      channel.basicConsume(queueName3, true, direct3DeliverCallback, consumerTag -> {
      });
  }
}
package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.serialization.SerializationStrategy;

public class RabbitMQConfiguration {
   private final String userName;
   private final String password;
   private final String hostName;
   private final int port;
   private final String vhost;
   private final SerializationStrategy serializationStrategy;
   private final int publisherPoolSize;

   private RabbitMQConfiguration(
         String userName,
         String password,
         String hostName,
         int port,
         String vhost,
         SerializationStrategy serializationStrategy,
         int publisherPoolSize) {
      this.userName = userName;
      this.password = password;
      this.hostName = hostName;
      this.port = port;
      this.vhost = vhost;
      this.serializationStrategy = serializationStrategy;
      this.publisherPoolSize = publisherPoolSize;
   }

   public String getUserName() {
      return userName;
   }

   public String getPassword() {
      return password;
   }

   public String getHostName() {
      return hostName;
   }

   public int getPort() {
      return port;
   }

   public String getVhost() {
      return vhost;
   }

   public SerializationStrategy getSerializationStrategy() {
      return serializationStrategy;
   }

   public int getPublisherPoolSize() {
      return publisherPoolSize;
   }

   public static class Builder {

      private String userName;
      private String password;
      private String hostName;
      private int port = 5672;
      private String vhost = "/";
      private SerializationStrategy serializationStrategy = SerializationStrategy.defaultSerializationStrategy();
      private int publisherPoolSize = 5;

      /**
       * A new rabbit configuration builder
       * @param userName rabbitmq username
       * @param password rabbitmq password
       * @param hostName rabbitmq host name
       */
      public Builder(String userName, String password, String hostName) {
         this.userName = userName;
         this.password = password;
         this.hostName = hostName;
      }

      /**
       * Sets the AMQP port to use (default is 5672)
       * @param port the port number
       * @return this builder
       */
      public Builder setPort(int port) {
         this.port = port;
         return this;
      }

      /**
       * Sets the virtual host to connect to (default is "/")
       * @param vhost the virtual host name
       * @return this builder
       */
      public Builder setVhost(String vhost) {
         this.vhost = vhost;
         return this;
      }

      /**
       * Sets the serialization strategy for messages sent to RabbitMQ (default is Jackson)
       * @param serializationStrategy the serialization strategy
       * @return this builder
       */
      public Builder setSerializationStrategy(SerializationStrategy serializationStrategy) {
         this.serializationStrategy = serializationStrategy;
         return this;
      }

      /**
       * Sets the max size of the publisher channel pool (default is 5)
       * @param publisherPoolSize the size
       * @return this builder
       */
      public Builder setPublisherPoolSize(int publisherPoolSize) {
         if (publisherPoolSize < 1) {
            throw new IllegalArgumentException("Publisher pool should at least be one, but was " + publisherPoolSize);
         }
         this.publisherPoolSize = publisherPoolSize;
         return this;
      }

      /**
       * Builds a rabbit configuration
       * @return the configuration
       */
      public RabbitMQConfiguration build() {
         return new RabbitMQConfiguration(userName, password, hostName, port, vhost, serializationStrategy, publisherPoolSize);
      }
   }
}
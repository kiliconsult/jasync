package com.kili.jasync.environment.rabbitmq;

import com.kili.jasync.serialization.SerializationStrategy;

public class RabbitMQConfiguration {
   private String userName;
   private String password;
   private String hostName;
   private int port;
   private String vhost;
   private SerializationStrategy serializationStrategy;

   private RabbitMQConfiguration(
         String userName,
         String password,
         String hostName,
         int port,
         String vhost,
         SerializationStrategy serializationStrategy) {
      this.userName = userName;
      this.password = password;
      this.hostName = hostName;
      this.port = port;
      this.vhost = vhost;
      this.serializationStrategy = serializationStrategy;
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

   public static class Builder {

      private String userName;
      private String password;
      private String hostName;
      private int port = 5672;
      private String vhost = "/";
      private SerializationStrategy serializationStrategy = SerializationStrategy.defaultSerializationStrategy();

      public Builder(String userName, String password, String hostName) {
         this.userName = userName;
         this.password = password;
         this.hostName = hostName;
      }

      public Builder setPort(int port) {
         this.port = port;
         return this;
      }

      public Builder setVhost(String vhost) {
         this.vhost = vhost;
         return this;
      }

      public Builder setSerializationStrategy(SerializationStrategy serializationStrategy) {
         this.serializationStrategy = serializationStrategy;
         return this;
      }

      public RabbitMQConfiguration build() {
         return new RabbitMQConfiguration(userName, password, hostName, port, vhost, serializationStrategy);
      }
   }
}
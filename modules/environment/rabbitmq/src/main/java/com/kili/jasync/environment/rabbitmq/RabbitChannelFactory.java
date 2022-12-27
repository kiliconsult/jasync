package com.kili.jasync.environment.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RabbitChannelFactory extends BasePooledObjectFactory<Channel> {

   private Connection connection;

   public RabbitChannelFactory(Connection connection) {
      this.connection = connection;
   }

   @Override
   public Channel create() throws Exception {
      return connection.createChannel();
   }

   @Override
   public PooledObject<Channel> wrap(Channel obj) {
      return new DefaultPooledObject<>(obj);
   }

   @Override
   public void destroyObject(PooledObject<Channel> p) throws Exception {
      p.getObject().close();
   }
}

package com.kili.jasync.serialization;

/**
 * The strategy that is used for serialization and deserialization of messages
 */
public interface SerializationStrategy {

   static SerializationStrategy defaultSerializationStrategy() {
      return new JacksonSerializationStrategy();
   }

   byte[] serialize(Object object) throws Exception;

   <T> T deserialize(Class<T> clazz, byte[] bytes) throws Exception;
}

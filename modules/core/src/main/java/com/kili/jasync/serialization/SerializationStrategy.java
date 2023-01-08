package com.kili.jasync.serialization;

/**
 * The strategy that is used for serialization and deserialization of messages
 */
public interface SerializationStrategy {

   static SerializationStrategy defaultSerializationStrategy() {
      return new JacksonSerializationStrategy();
   }

   /**
    * Serialize an object to a byte array
    * @param object the given object
    * @return a byte array
    * @throws Exception
    */
   byte[] serialize(Object object) throws Exception;

   /**
    * Deserialize a byte array into a java object
    * @param clazz the class to instantiate
    * @param bytes the byte array
    * @return an instance of the clazz
    * @param <T> Type of the object
    * @throws Exception
    */
   <T> T deserialize(Class<T> clazz, byte[] bytes) throws Exception;
}

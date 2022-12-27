package com.kili.jasync.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JacksonSerializationStrategy implements SerializationStrategy {

   private static final ObjectMapper MAPPER = new ObjectMapper();

   @Override
   public byte[] serialize(Object object) throws JsonProcessingException {
      return MAPPER.writeValueAsBytes(object);
   }

   @Override
   public <T> T deserialize(Class<T> clazz, byte[] bytes) throws IOException {
      return MAPPER.readerFor(clazz).readValue(bytes);
   }
}

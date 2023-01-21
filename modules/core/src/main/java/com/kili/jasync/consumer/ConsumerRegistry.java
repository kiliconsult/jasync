package com.kili.jasync.consumer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConsumerRegistry<O> {

   private final Map<Key<?>, O> registry = new HashMap<>();

   @SuppressWarnings("unchecked")
   public <T> void registerConsumer(Class<? extends Consumer<T>> consumerClass, Class<T> messageType, O object) {
      Key<T> key = new Key<>(consumerClass, messageType);
      registry.put(key, object);
   }

   @SuppressWarnings("unchecked")
   public <T> O getConsumer(Class<? extends Consumer<T>> consumerClass, Class<T> messageType) {
      Key<T> key = new Key<>(consumerClass, messageType);
      return registry.get(key);
   }

   public Collection<O> values() {
      return registry.values();
   }

   public static class Key<T> {
      private final Class<? extends Consumer<T>> consumerClass;
      private final Class<T> messageType;

      public Key(Class<? extends Consumer<T>> consumerClass, Class<T> messageType) {
         this.consumerClass = consumerClass;
         this.messageType = messageType;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }
         Key<?> key = (Key<?>) o;
         return consumerClass.equals(key.consumerClass) && messageType.equals(key.messageType);
      }

      @Override
      public int hashCode() {
         return Objects.hash(consumerClass, messageType);
      }
   }
}

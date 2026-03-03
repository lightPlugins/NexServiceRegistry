package io.nexstudios.serviceregistry;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe implementation of the {@link ServiceRegistry} interface.
 *
 * This class provides functionality to register, find, and require services by their exact type keys.
 * Services are stored as instances mapped to their respective types, and type-safety is enforced during registration.
 */
public final class DefaultServiceRegistry implements ServiceRegistry {

  private final ConcurrentHashMap<Class<?>, Object> map = new ConcurrentHashMap<>();

  @Override
  public <T> void register(Class<T> type, T instance) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("instance must not be null");
    }
    if (!type.isInstance(instance)) {
      throw new IllegalArgumentException(
          "instance type mismatch: expected " + type.getName() + " but was " + instance.getClass().getName()
      );
    }

    map.put(type, instance);
  }

  @Override
  public <T> Optional<T> find(Class<T> type) {
    if (type == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(type.cast(map.get(type)));
  }

  @Override
  public <T> T require(Class<T> type) {
    return find(type).orElseThrow(() -> new IllegalStateException(
        "No service registered for type: " + (type == null ? "null" : type.getName())
    ));
  }

  @Override
  public int size() {
    return map.size();
  }
}
package io.nexstudios.serviceregistry;

import java.util.Optional;

/**
 * Interface defining a registry for managing services within an application.
 * Provides methods to register, find, and require services based on their exact type.
 * Implementations of this interface should ensure thread safety and type safety.
 */
public interface ServiceRegistry {

  /**
   * Registers an instance of the specified type in the service registry.
   *
   * The method ensures that the provided type and instance are not null,
   * and that the instance is of the expected type. If the type and instance
   * validations pass, the instance is registered and associated with the specified type.
   *
   * @param <T>      the type of the service or object being registered
   * @param type     the class representing the type under which the instance will be registered; must not be null
   * @param instance the instance of the object to be registered; must not be null and must be assignable to the type
   * @throws IllegalArgumentException if the type or instance is null, or if the instance is not assignable to the specified type
   */
  <T> void register(Class<T> type, T instance);

  /**
   * Retrieves an instance of the specified type from the service registry, if available.
   *
   * This method performs a lookup for a service or object that matches the exact type
   * provided. If a corresponding instance is found, it is returned wrapped in an
   * {@link Optional}. If no instance is registered for the given type, an empty
   * {@code Optional} is returned.
   *
   * @param <T> the type of the service or object to find
   * @param type the class representing the type of the requested service or object; must not be null
   * @return an {@code Optional} containing the found instance if registered, or an empty {@code Optional} if not
   */
  <T> Optional<T> find(Class<T> type);

  /**
   * Retrieves an instance of the specified type from the service registry.
   * If no instance is registered for the given type, an exception is thrown.
   *
   * @param <T>  the type of the service or object to retrieve
   * @param type the class representing the type of the requested service or object; must not be null
   * @return the instance of the specified type if registered
   * @throws IllegalStateException if no service is registered for the specified type
   */
  <T> T require(Class<T> type);

  /**
   * Retrieves the total number of services currently registered in the registry.
   *
   * This method returns the count of all registered services, where each service is uniquely
   * associated with its respective type. The count reflects the total number of mappings stored
   * in the registry at the time this method is called.
   *
   * @return the number of registered services in the registry
   */
  int size();
}
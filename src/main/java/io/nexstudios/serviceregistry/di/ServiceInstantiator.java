package io.nexstudios.serviceregistry.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Provides functionality to instantiate service objects, leveraging their constructors
 * and ensuring proper validation of arguments and class types.
 *
 * This class attempts to create service instances using two approaches:
 * - A constructor with a {@link ServiceAccessor} parameter.
 * - A no-argument constructor.
 *
 * If both approaches fail or the provided implementation type is invalid
 * (e.g., it is abstract or an interface), an exception is thrown.
 *
 * This class is immutable and thread-safe.
 */
public final class ServiceInstantiator {

  public <T extends Service> T create(ServiceAccessor accessor, Class<? extends T> implType) {
    Objects.requireNonNull(accessor, "accessor must not be null");
    Objects.requireNonNull(implType, "implType must not be null");

    if (implType.isInterface() || Modifier.isAbstract(implType.getModifiers())) {
      throw new IllegalArgumentException("implType must be a concrete class: " + implType.getName());
    }

    try {
      try {
        Constructor<? extends T> c = implType.getDeclaredConstructor(ServiceAccessor.class);
        c.setAccessible(true);
        return c.newInstance(accessor);
      } catch (NoSuchMethodException ignored) { }

      Constructor<? extends T> constructor = implType.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new IllegalStateException("Failed to instantiate service: " + implType.getName() + " (" + cause + ")", cause);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to instantiate service: " + implType.getName() + " (" + e + ")", e);
    }
  }
}
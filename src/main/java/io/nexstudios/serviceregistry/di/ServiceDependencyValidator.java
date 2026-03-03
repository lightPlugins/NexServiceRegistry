package io.nexstudios.serviceregistry.di;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Validates the dependencies of a service implementation class against a given service accessor.
 *
 * This class provides functionality to ensure that all declared dependencies of a service
 * are fulfilled and available in the associated {@link ServiceAccessor}. It performs
 * checks to validate that the implementation class is concrete, its dependencies extend
 * the {@link Service} interface, and that all required dependencies are registered in the
 * service registry.
 *
 * This validation helps maintain the integrity and correctness of the dependency structure
 * within a service-based architecture.
 */
public final class ServiceDependencyValidator {

  public void validateDependencies(ServiceAccessor accessor, Class<?> implType) {
    Objects.requireNonNull(accessor, "accessor must not be null");
    Objects.requireNonNull(implType, "implType must not be null");

    if (implType.isInterface() || Modifier.isAbstract(implType.getModifiers())) {
      throw new IllegalArgumentException("implType must be a concrete class: " + implType.getName());
    }

    for (Class<?> dep : collectDependencies(implType)) {
      if (dep == null) {
        throw new IllegalStateException("Dependency list contains null for: " + implType.getName());
      }

      if (!Service.class.isAssignableFrom(dep)) {
        throw new IllegalArgumentException(
            "Invalid dependency " + dep.getName() + " on " + implType.getName() + ": dependencies must extend " + Service.class.getName()
        );
      }

      @SuppressWarnings("unchecked")
      Class<? extends Service> depServiceType = (Class<? extends Service>) dep;

      if (accessor.findService(depServiceType).isEmpty()) {
        String ownerPart = accessor.ownerName().map(n -> " (owner=" + n + ")").orElse("");
        throw new IllegalStateException(
            "Missing dependency " + dep.getName() + " required by " + implType.getName() + ownerPart
        );
      }
    }
  }

  private Set<Class<?>> collectDependencies(Class<?> type) {
    Set<Class<?>> out = new LinkedHashSet<>();

    Class<?> current = type;
    while (current != null && current != Object.class) {
      Dependencies deps = current.getAnnotation(Dependencies.class);
      if (deps != null) {
        Collections.addAll(out, deps.value());
        Collections.addAll(out, deps.includes());
        if (!deps.inherit()) {
          break;
        }
      }
      current = current.getSuperclass();
    }

    return out;
  }
}
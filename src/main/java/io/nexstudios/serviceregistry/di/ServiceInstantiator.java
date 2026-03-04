package io.nexstudios.serviceregistry.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Provides functionality to instantiate service objects, leveraging their constructors
 * and ensuring proper validation of arguments and class types.
 *
 * This class supports constructor injection:
 * - If there is exactly one constructor, it will be used.
 * - Otherwise, a constructor annotated with {@link Inject} is preferred.
 * - Otherwise, the "greediest" constructor (most parameters) is used.
 *
 * Parameters are resolved as follows:
 * - {@link ServiceAccessor} -> injected with the current accessor
 * - {@link ServiceOwner} -> injected with the current owner (if present)
 * - Otherwise: treated as a service dependency
 *   - if concrete service class -> created recursively
 *   - else -> resolved via {@link ServiceAccessor#getService(Class)}
 *
 * Cyclic dependencies are detected via a per-thread instantiation stack.
 *
 * This class is immutable and thread-safe.
 */
public final class ServiceInstantiator {

  private static final ThreadLocal<Deque<Class<?>>> INSTANTIATION_STACK =
      ThreadLocal.withInitial(ArrayDeque::new);

  public <T extends Service> T create(ServiceAccessor accessor, Class<? extends T> implType) {
    Objects.requireNonNull(accessor, "accessor must not be null");
    Objects.requireNonNull(implType, "implType must not be null");

    if (implType.isInterface() || Modifier.isAbstract(implType.getModifiers())) {
      throw new IllegalArgumentException("implType must be a concrete class: " + implType.getName());
    }

    Deque<Class<?>> stack = INSTANTIATION_STACK.get();
    if (stack.contains(implType)) {
      throw new IllegalStateException("Cyclic service dependency detected: " + formatCycle(stack, implType));
    }

    stack.push(implType);
    try {
      Constructor<? extends T> ctor = selectConstructor(implType);

      Object[] args = resolveArguments(accessor, implType, ctor);

      ctor.setAccessible(true);
      return ctor.newInstance(args);

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      throw new IllegalStateException("Failed to instantiate service: " + implType.getName() + " (" + cause + ")", cause);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to instantiate service: " + implType.getName() + " (" + e + ")", e);
    } finally {
      Class<?> popped = stack.pop();
      if (popped != implType) {
        // should never happen, but protects against stack corruption
        stack.clear();
        throw new IllegalStateException("Service instantiation stack corrupted while creating: " + implType.getName());
      }
      if (stack.isEmpty()) {
        INSTANTIATION_STACK.remove();
      }
    }
  }

  private <T extends Service> Constructor<? extends T> selectConstructor(Class<? extends T> implType) {
    @SuppressWarnings("unchecked")
    Constructor<? extends T>[] ctors = (Constructor<? extends T>[]) implType.getDeclaredConstructors();

    if (ctors.length == 0) {
      throw new IllegalStateException("No constructors found for: " + implType.getName());
    }

    if (ctors.length == 1) {
      return ctors[0];
    }

    List<Constructor<? extends T>> injectCtors = new ArrayList<>();
    for (Constructor<? extends T> c : ctors) {
      if (c.isAnnotationPresent(Inject.class)) {
        injectCtors.add(c);
      }
    }

    if (injectCtors.size() == 1) {
      return injectCtors.getFirst();
    }
    if (injectCtors.size() > 1) {
      throw new IllegalStateException(
          "Multiple @Inject constructors found for " + implType.getName() + ": " + formatConstructorList(injectCtors)
      );
    }

    Constructor<? extends T> best = null;
    int bestCount = -1;
    boolean tie = false;

    for (Constructor<? extends T> c : ctors) {
      int pc = c.getParameterCount();
      if (pc > bestCount) {
        best = c;
        bestCount = pc;
        tie = false;
      } else if (pc == bestCount) {
        tie = true;
      }
    }

    if (best == null) {
      throw new IllegalStateException("Could not select constructor for: " + implType.getName());
    }
    if (tie) {
      throw new IllegalStateException(
          "Ambiguous greediest constructor selection for " + implType.getName()
              + " (multiple constructors with " + bestCount + " parameters). "
              + "Please annotate the desired constructor with @Inject."
      );
    }

    return best;
  }

  private <T extends Service> Object[] resolveArguments(
      ServiceAccessor accessor,
      Class<? extends T> implType,
      Constructor<? extends T> ctor
  ) {
    Class<?>[] paramTypes = ctor.getParameterTypes();
    Object[] args = new Object[paramTypes.length];

    for (int i = 0; i < paramTypes.length; i++) {
      Class<?> p = paramTypes[i];
      try {
        args[i] = resolveSingleArgument(accessor, p);
      } catch (RuntimeException ex) {
        throw new IllegalStateException(
            "Failed to resolve constructor parameter for service " + implType.getName()
                + " at index " + i + " (type=" + p.getName() + ")"
                + " in constructor " + formatConstructorSignature(ctor) + ": " + ex.getMessage(),
            ex
        );
      }
    }

    return args;
  }

  private Object resolveSingleArgument(ServiceAccessor accessor, Class<?> paramType) {
    if (paramType == ServiceAccessor.class) {
      return accessor;
    }

    if (ServiceOwner.class.isAssignableFrom(paramType)) {
      ServiceOwner owner = accessor.owner();
      if (!paramType.isInstance(owner)) {
        throw new IllegalStateException(
            "ServiceOwner type mismatch: constructor expects " + paramType.getName()
                + " but current owner is " + owner.getClass().getName()
        );
      }
      return owner;
    }

    if (!Service.class.isAssignableFrom(paramType)) {
      throw new IllegalStateException(
          "Parameter type is not supported (must be ServiceAccessor, ServiceOwner or a Service): " + paramType.getName()
      );
    }

    @SuppressWarnings("unchecked")
    Class<? extends Service> serviceType = (Class<? extends Service>) paramType;

    // If it's a concrete class, we can create it recursively (and user can choose to register it externally if they need singleton behavior).
    if (!serviceType.isInterface() && !Modifier.isAbstract(serviceType.getModifiers())) {
      return create(accessor, serviceType);
    }

    // Otherwise, it must come from registry.
    return accessor.getService(serviceType);
  }

  private static String formatConstructorSignature(Constructor<?> ctor) {
    StringJoiner sj = new StringJoiner(", ", ctor.getDeclaringClass().getName() + "(", ")");
    for (Class<?> p : ctor.getParameterTypes()) {
      sj.add(p.getName());
    }
    return sj.toString();
  }

  private static String formatConstructorList(List<? extends Constructor<?>> ctors) {
    StringJoiner sj = new StringJoiner("; ");
    for (Constructor<?> c : ctors) {
      sj.add(formatConstructorSignature(c));
    }
    return sj.toString();
  }

  private static String formatCycle(Deque<Class<?>> stack, Class<?> repeating) {
    List<Class<?>> chain = new ArrayList<>(stack); // top-first
    StringBuilder sb = new StringBuilder();

    // chain is [current, ..., root], we want root -> ... -> repeating -> repeating
    for (int i = chain.size() - 1; i >= 0; i--) {
      sb.append(chain.get(i).getName()).append(" -> ");
    }
    sb.append(repeating.getName());
    return sb.toString();
  }
}
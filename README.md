# nex-service-registry

> A small, standalone **Java 21** service registry with an optional **DI-light** layer for simple service lookup, creation, and dependency validation.

<p align="left">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-blue" />
  <img alt="Build Tool" src="https://img.shields.io/badge/Build-Gradle%20(Kotlin%20DSL)-02303A" />
  <img alt="Plugin" src="https://img.shields.io/badge/Gradle%20Plugins-java--library%20%7C%20maven--publish-success" />
  <img alt="Runtime Dependencies" src="https://img.shields.io/badge/Runtime%20Dependencies-None-brightgreen" />
</p>

---

## Features

- **Minimal core API** for type-safe service registration and lookup
- **Thread-safe default implementation** backed by `ConcurrentHashMap<Class<?>, Object>`
- **No external runtime dependencies**
- **Pure Java 21** and suitable for any standalone JVM project
- **Optional DI-light layer** for lightweight service creation and dependency validation
- **Constructor-aware instantiation** through `ServiceAccessor`
- **Predictable failure behavior** with clear exceptions
- **Gradle-friendly publishing** via `maven-publish`

---

## Installation

Build and publish the library to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

Then consume it from another Gradle project.

### `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

### `build.gradle.kts`

```kotlin
dependencies {
    implementation("io.nexstudios:nex-service-registry:1.0.0")
}
```

> Replace the version with the version you publish locally or remotely.

---

## Quick Start: ServiceRegistry

```java
import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.ServiceRegistry;

public final class Main {
    public static void main(String[] args) {
        ServiceRegistry registry = new DefaultServiceRegistry();

        registry.register(String.class, "Hello, registry!");

        String value = registry.require(String.class);
        System.out.println(value);
    }
}
```

---

## Use Cases / Examples

### Simple registry usage

```java
import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.ServiceRegistry;

import java.util.Optional;

public final class RegistryExample {
    public static void main(String[] args) {
        ServiceRegistry registry = new DefaultServiceRegistry();

        registry.register(String.class, "NexStudios");
        registry.register(Integer.class, 21);

        Optional<String> name = registry.find(String.class);
        Optional<Double> missing = registry.find(Double.class);

        String required = registry.require(String.class);
        int size = registry.size();

        System.out.println(name.orElse("Unknown")); // NexStudios
        System.out.println(missing.isPresent());    // false
        System.out.println(required);               // NexStudios
        System.out.println(size);                   // 2
    }
}
```

### DI-Light: register interface + implementation class

```java
import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

public final class DiRegisterExample {

    interface MessageService extends Service {
        String message();
    }

    public static final class DefaultMessageService implements MessageService {
        @Override
        public String message() {
            return "Hello from DI-light";
        }
    }

    public static void main(String[] args) {
        ServiceAccessor accessor = new ServiceAccessor(new DefaultServiceRegistry());

        MessageService service = accessor.register(MessageService.class, DefaultMessageService.class);
        System.out.println(service.message());
    }
}
```

### Dependencies with `@Dependencies`

Use `@Dependencies` when a service requires one or more other services to be available first.
The dependency types listed in the annotation must implement `Service`.

```java
import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

public final class DependenciesExample {

    interface DatabaseService extends Service {
    }

    interface UserService extends Service {
    }

    public static final class DefaultDatabaseService implements DatabaseService {
    }

    @Dependencies(DatabaseService.class)
    public static final class DefaultUserService implements UserService {
    }

    public static void main(String[] args) {
        ServiceAccessor accessor = new ServiceAccessor(new DefaultServiceRegistry());

        try {
            accessor.register(UserService.class, DefaultUserService.class);
        } catch (IllegalStateException ex) {
            System.out.println("Missing dependency: " + ex.getMessage());
        }

        accessor.register(DatabaseService.class, DefaultDatabaseService.class);
        UserService userService = accessor.register(UserService.class, DefaultUserService.class);

        System.out.println(userService.getClass().getSimpleName());
    }
}
```

### `ServiceAccessor` constructor vs no-arg constructor

`ServiceAccessor` supports two instantiation paths for `create(...)` and `register(..., implType)`:

1. A constructor that accepts `ServiceAccessor`
2. A no-argument constructor

```java
import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

public final class ConstructorExample {

    public static final class AccessorAwareService implements Service {
        private final ServiceAccessor accessor;

        public AccessorAwareService(ServiceAccessor accessor) {
            this.accessor = accessor;
        }

        public ServiceAccessor accessor() {
            return accessor;
        }
    }

    public static final class PlainService implements Service {
        public PlainService() {
        }
    }

    public static void main(String[] args) {
        ServiceAccessor accessor = new ServiceAccessor(new DefaultServiceRegistry());

        AccessorAwareService a = accessor.create(AccessorAwareService.class);
        PlainService b = accessor.create(PlainService.class);

        System.out.println(a.accessor() != null); // true
        System.out.println(b != null);            // true
    }
}
```

### Optional Kotlin consumer example

The library is Java-first, but it can be consumed cleanly from Kotlin as well.

```kotlin
import io.nexstudios.serviceregistry.DefaultServiceRegistry
import io.nexstudios.serviceregistry.ServiceRegistry

fun main() {
    val registry: ServiceRegistry = DefaultServiceRegistry()

    registry.register(String::class.java, "Hello from Kotlin")

    val required = registry.require(String::class.java)
    val optional = registry.find(Int::class.java)

    println(required)
    println(optional.isPresent)
}
```

---

## Thread-safety Notes and Design Decisions

- `DefaultServiceRegistry` uses a `ConcurrentHashMap<Class<?>, Object>` internally.
- Registration and lookup are safe to perform from multiple threads.
- Services are stored **by exact key type**.
- The registry intentionally stays small and explicit: no scanning, no proxies, no hidden lifecycle management.

---

## Error Handling

The library keeps failure behavior explicit and easy to reason about.

- `find(null)` returns `Optional.empty()`.
- `require(Class<T>)` throws `IllegalStateException` when no service is registered for the requested type.
- DI-light registration and creation may throw `IllegalStateException` when:
  - a required dependency is missing
  - the target type cannot be instantiated through a supported constructor
  - dependency metadata is invalid at runtime
- Reflection-related failures are surfaced as runtime failures rather than being silently ignored.

---

## Publishing

This project is designed to work cleanly with Gradle's `maven-publish` plugin.

To publish locally:

```bash
./gradlew publishToMavenLocal
```

With `maven-publish` configured, the generated artifact can then be consumed from `mavenLocal()` like any other standard Java library.

---

## License

**MIT** 

---

## Contributing

Contributions are welcome.

- Keep the API minimal and focused.
- Preserve Java 21 compatibility.
- Avoid adding external runtime dependencies.
- Prefer small, well-tested changes with clear rationale.

If you plan to make a larger change, open an issue or discussion first to align on scope and design.

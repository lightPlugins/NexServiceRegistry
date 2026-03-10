package io.nexstudios.serviceregistry.di;

import io.nexstudios.serviceregistry.ServiceRegistry;

import java.util.Objects;
import java.util.Optional;

public final class ServiceAccessor {

  private final ServiceRegistry registry;
  private final ServiceOwner owner; // optional
  private final ServiceDependencyValidator validator;
  private final ServiceInstantiator instantiator;

  public ServiceAccessor(ServiceRegistry registry) {
    this(registry, null, new ServiceDependencyValidator(), new ServiceInstantiator());
  }

  public ServiceAccessor(ServiceRegistry registry, ServiceOwner owner) {
    this(registry, owner, new ServiceDependencyValidator(), new ServiceInstantiator());
  }

  public ServiceAccessor(ServiceRegistry registry, ServiceOwner owner,
                         ServiceDependencyValidator validator,
                         ServiceInstantiator instantiator) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
    this.owner = owner;
    this.validator = Objects.requireNonNull(validator, "validator must not be null");
    this.instantiator = Objects.requireNonNull(instantiator, "instantiator must not be null");
  }

  public Optional<String> ownerName() {
    return owner == null ? Optional.empty() : Optional.ofNullable(owner.name());
  }

  public ServiceOwner owner() {
    if (owner == null) {
      throw new IllegalStateException("No ServiceOwner available in this ServiceAccessor");
    }
    return owner;
  }

  public <T extends Service> T getService(Class<T> type) {
    return registry.require(type);
  }

  public <T extends Service> Optional<T> findService(Class<T> type) {
    return registry.find(type);
  }

  public <T extends Service> void register(Class<T> type, T instance) {
    registry.register(type, instance);
  }

  public <T extends Service> T register(Class<T> type, Class<? extends T> implType) {
    if (type == null) throw new IllegalArgumentException("type must not be null");
    if (implType == null) throw new IllegalArgumentException("implType must not be null");

    validator.validateDependencies(this, implType);
    T instance = instantiator.create(this, implType);

    registry.register(type, instance);
    return instance;
  }

  public <T extends Service> T create(Class<T> implType) {
    if (implType == null) throw new IllegalArgumentException("implType must not be null");

    validator.validateDependencies(this, implType);
    return instantiator.create(this, implType);
  }

  public void install(ServiceModule module) {
    Objects.requireNonNull(module, "module must not be null");
    module.install(this);
  }

  public void installAll(Iterable<? extends ServiceModule> modules) {
    Objects.requireNonNull(modules, "modules must not be null");
    for (ServiceModule m : modules) {
      install(m);
    }
  }
}
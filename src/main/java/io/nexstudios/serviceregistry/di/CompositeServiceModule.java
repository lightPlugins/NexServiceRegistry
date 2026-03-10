package io.nexstudios.serviceregistry.di;

import java.util.List;
import java.util.Objects;

public final class CompositeServiceModule implements ServiceModule {

  private final List<ServiceModule> modules;

  public CompositeServiceModule(List<ServiceModule> modules) {
    this.modules = List.copyOf(Objects.requireNonNull(modules, "modules must not be null"));
  }

  @Override
  public void install(ServiceAccessor services) {
    for (ServiceModule m : modules) {
      m.install(services);
    }
  }
}
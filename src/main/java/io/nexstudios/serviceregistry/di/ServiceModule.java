package io.nexstudios.serviceregistry.di;

@FunctionalInterface
public interface ServiceModule {
  void install(ServiceAccessor services);
}
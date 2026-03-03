package io.nexstudios.serviceregistry.di;

/**
 * Represents an entity that owns or manages services in a dependency injection context.
 *
 * Implementations of this interface define a unique name identifier for the service owner.
 * The name can be used for identification, association, or tracking of services managed
 * within the application's service registry.
 */
public interface ServiceOwner {
  String name();
}
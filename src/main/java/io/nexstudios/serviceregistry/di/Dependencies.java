package io.nexstudios.serviceregistry.di;

import java.lang.annotation.*;

/**
 * Declares dependencies for a DI-Light {@link Service} implementation.
 *
 * <p>All dependency types referenced by this annotation must themselves implement {@link Service}.
 * This is enforced at runtime by the dependency validator.</p>
 *
 * <p>The annotation provides three main components:</p>
 * <ul>
 *   <li>{@link #value()} – primary dependencies</li>
 *   <li>{@link #includes()} – additional/auxiliary dependencies</li>
 *   <li>{@link #inherit()} – whether dependencies declared on superclasses should be inherited</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Dependencies {

  /**
   * Specifies the primary dependencies required by a class in a dependency injection context.
   *
   * @return an array of classes representing the dependencies of the annotated class
   */
  Class<?>[] value() default {};

  /**
   * Specifies additional dependencies that should be included for a class in a
   * dependency injection context. These dependencies are optional and can be used
   * to augment the primary dependencies defined in {@link Dependencies#value()}.
   *
   * @return an array of classes representing the additional dependencies
   */
  Class<?>[] includes() default {};

  /**
   * Determines whether the dependency information defined in the annotated class
   * should be inherited from its superclass(es).
   *
   * @return true if dependencies from superclasses should be inherited; false otherwise
   */
  boolean inherit() default true;
}
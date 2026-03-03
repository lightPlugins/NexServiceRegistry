package io.nexstudios.serviceregistry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultServiceRegistryTest {

  interface Foo {}
  static final class FooImpl implements Foo {}

  @Test
  void register_find_require_size() {
    ServiceRegistry r = new DefaultServiceRegistry();

    assertEquals(0, r.size());
    assertEquals(Optional.empty(), r.find(Foo.class));

    FooImpl foo = new FooImpl();
    r.register(Foo.class, foo);

    assertEquals(1, r.size());
    assertSame(foo, r.find(Foo.class).orElseThrow());
    assertSame(foo, r.require(Foo.class));
  }

  @Test
  void register_null_checks() {
    ServiceRegistry r = new DefaultServiceRegistry();

    IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> r.register(null, new FooImpl()));
    assertTrue(e1.getMessage().toLowerCase().contains("type"));

    IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> r.register(Foo.class, null));
    assertTrue(e2.getMessage().toLowerCase().contains("instance"));
  }

  @Test
  void find_null_is_empty() {
    ServiceRegistry r = new DefaultServiceRegistry();
    assertEquals(Optional.empty(), r.find(null));
  }

  @Test
  void require_throws_when_missing() {
    ServiceRegistry r = new DefaultServiceRegistry();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> r.require(Foo.class));
    assertTrue(e.getMessage().contains(Foo.class.getName()));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void register_rejects_wrong_instance_type() {
    ServiceRegistry r = new DefaultServiceRegistry();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
        r.register((Class) Foo.class, "not a foo")
    );

    assertTrue(e.getMessage().toLowerCase().contains("mismatch"));
  }
}
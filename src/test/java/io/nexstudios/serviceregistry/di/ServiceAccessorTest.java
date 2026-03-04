package io.nexstudios.serviceregistry.di;

import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAccessorTest {

  interface A extends Service {}
  static final class AImpl implements A {}

  interface B extends Service {}

  @Dependencies(A.class)
  static final class BImpl implements B {
    final ServiceAccessor accessor;
    BImpl(ServiceAccessor accessor) { this.accessor = accessor; }
  }

  @Test
  void register_implType_validates_dependencies_and_instantiates() {
    ServiceAccessor acc = new ServiceAccessor(new DefaultServiceRegistry());

    assertThrows(IllegalStateException.class, () -> acc.register(B.class, BImpl.class));

    acc.register(A.class, new AImpl());
    B b = acc.register(B.class, BImpl.class);

    assertNotNull(b);
    assertSame(b, acc.getService(B.class));
    assertSame(acc, ((BImpl) b).accessor);
  }

  @Test
  void create_does_not_register() {
    ServiceAccessor acc = new ServiceAccessor(new DefaultServiceRegistry());
    acc.register(A.class, new AImpl());

    BImpl b = acc.create(BImpl.class);
    assertNotNull(b);

    assertTrue(acc.findService(B.class).isEmpty());
  }

  static final class OwnerImpl implements ServiceOwner {
    private final String name;
    OwnerImpl(String name) { this.name = name; }
    @Override public String name() { return name; }
  }

  interface C extends Service {}

  static final class CImpl implements C {
    final ServiceOwner owner;
    CImpl(ServiceOwner owner) { this.owner = owner; }
  }

  @Test
  void create_injects_serviceOwner_instance() {
    ServiceOwner owner = new OwnerImpl("nex");
    ServiceAccessor acc = new ServiceAccessor(new DefaultServiceRegistry(), owner);

    CImpl c = acc.create(CImpl.class);
    assertNotNull(c);
    assertSame(owner, c.owner);
  }
}
package example;

public class InstanceACalls {

  private InstanceBCalls instanceB = null;

  public void a() {
    if (instanceB != null) {
      instanceB.b();
    }
  }

  public void c() {}

  public void e() {
    if (instanceB != null) {
      this.c();
    }
  }

  public InstanceACalls setInstanceB(final InstanceBCalls instanceB) {
    this.instanceB = instanceB;
    return this;
  }
}

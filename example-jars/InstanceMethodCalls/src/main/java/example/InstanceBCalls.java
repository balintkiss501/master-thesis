package example;

public class InstanceBCalls {

  private InstanceACalls instanceA = null;

  public void b() {
    if (instanceA != null) {
      instanceA.c();
      this.d();
    }
  }

  public void d() {
    if (instanceA != null) {
      instanceA.e();
      instanceA.c();
    }
  }

  public InstanceBCalls setInstanceA(final InstanceACalls instanceA) {
    this.instanceA = instanceA;
    return this;
  }
}

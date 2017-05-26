package testclass;

public class TestSubClass extends TestSuperClass implements TestInterface {

    enum TestEnum {
        FIRST,
        SECOND,
        THIRD;
    }

    class InnerClass {
        public int f;
        public int g;
    }

    private int x;
    public int y;
    public InnerClass innerClass;

    private TestSubClass() {
        this.x = 1;
    }

    public TestSubClass(final int y) {
        this.y = y;
    }

    @Override
    public void interfaceMethod() {

    }

    public int subMethod0() {
        return x + y;
    }

    public int subMethod1(final int a, final int b) {
        return a + b;
    }
}

package example;

class StaticMethodCalls {

  public static void a() { b(); }
  public static void b() { c(); d(); }
  public static void c() {}
  public static void d() { e(); c(); }
  public static void e() { c(); }

  public static void main(String[] args)
  {
    a();
  }
}

package prozee.proc;

public class Person {
  private int age;

  private String name;
  private String foo;

  public int getAge() {
    return age;
  }

  @Foo
  public void setAge(int age) {
    this.age = age;
  }

  public String getName() {
    return name;
  }

  @Foo
  public void setName(String name) {
    this.name = name;
  }
}

package prozee.proc;

import javax.annotation.processing.Generated;

@Generated(value = "", date = "", comments = "")
public class Program {
  public static void main(String... args) {
    Person ss = new PersonBuilder().setAge(1).setName("ss").build();
    System.out.println(ss.getName());
  }
}

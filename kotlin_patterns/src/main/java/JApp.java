import create.JavaBuilderUser;
import create.JavaPrototype;
import create.JavaSimpleFactory;

import performance.Varargs;
import structural.JavaDecorator;

public class JApp {

    private void fun() throws CloneNotSupportedException {
        JavaBuilderUser user = new JavaBuilderUser.Builder().buildName("karl").buildAddress("SZ").buildAge(10).build();

        JavaPrototype ontology = new JavaPrototype("Karl", 18, "", "");
        JavaPrototype clone = (JavaPrototype) ontology.clone();

        JavaSimpleFactory.GasCar gasCar = (JavaSimpleFactory.GasCar) new JavaSimpleFactory().new CarFactory().buildCar(JavaSimpleFactory.Type.GAS);
        JavaSimpleFactory.EV evCar = (JavaSimpleFactory.EV) new JavaSimpleFactory().new CarFactory().buildCar(JavaSimpleFactory.Type.EV);
    }

    public static void main(String[] args) {
       // new JavaStrategy().run();
        new JavaDecorator().test();
        int[] nums = {1, 2, 3, 4};
        new Varargs().numbers(nums);
        for (int num : nums) {
            System.out.print(num);
        }

    }
}

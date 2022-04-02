public class JApp {

    private void fun() throws CloneNotSupportedException {
        JavaBuilderUser user = new JavaBuilderUser.Builder().buildName("karl").buildAddress("SZ").buildAge(10).build();

        JavaPrototype ontology = new JavaPrototype("Karl", 18, "", "");
        JavaPrototype clone = (JavaPrototype) ontology.clone();

        JavaSimpleFactory.GasCar gasCar = (JavaSimpleFactory.GasCar) new JavaSimpleFactory().new CarFactory().buildCar(JavaSimpleFactory.Type.GAS);
        JavaSimpleFactory.EV evCar = (JavaSimpleFactory.EV) new JavaSimpleFactory().new CarFactory().buildCar(JavaSimpleFactory.Type.EV);
    }
}

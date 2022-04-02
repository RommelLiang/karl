package create;

public class JavaSimpleFactory {

    public interface Car {
        void replenishingEnergy();
    }

    public class GasCar implements Car {

        @Override
        public void replenishingEnergy() {
            System.out.println("加95");
        }
    }

    public class EV implements Car {

        @Override
        public void replenishingEnergy() {
            System.out.println("充电");
        }
    }

    public static class PHEV implements Car {

        @Override
        public void replenishingEnergy() {
            System.out.println("充电和加油");
        }
    }

    public enum Type{
        GAS,
        EV,
        PHEV
    }

    public  class CarFactory {
        public Car buildCar(Type type){
            switch (type){
                case GAS: return new GasCar();
                case EV: return new EV();
                case PHEV: return new PHEV();
            }
            return null;
        }
    }
}

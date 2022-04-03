package structural;

public class JavaDecorator {

    public void test() {
        new Phev(new Car()).refuel();
    }

    interface Oil {
        public void refuel();
    }

    class Car implements Oil {
        @Override
        public void refuel() {
            System.out.println("98加满");
        }
    }

    abstract class CarDecorator implements Oil {
        private Car car;

        public CarDecorator(Car car) {
            this.car = car;
        }

        @Override
        public void refuel() {
            car.refuel();
        }
    }

    class Phev extends CarDecorator {

        public Phev(Car car) {
            super(car);
        }

        @Override
        public void refuel() {
            super.refuel();
            System.out.println("再去冲一个小时的电");

        }
    }
}

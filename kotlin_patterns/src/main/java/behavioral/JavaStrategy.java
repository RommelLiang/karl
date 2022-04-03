package behavioral;

public class JavaStrategy {

    public void run() {
        new Works(new Proletariat()).go();
        new Works(new MiddleClass()).go();
        new Works(new Capitalist()).go();
    }
    public interface GoToWork{
        public void toWork();
    }

    public class Proletariat implements GoToWork{

        @Override
        public void toWork() {
            System.out.println("无产阶级只能骑自行车");
        }
    }

    public class MiddleClass implements GoToWork{

        @Override
        public void toWork() {
            System.out.println("中产可以开小汽车");
        }
    }

    public class Capitalist implements GoToWork{

        @Override
        public void toWork() {
            System.out.println("资本家不需要上班，因为他们挂在路灯上");
        }
    }

    public class Works {
        private GoToWork goToWork;

        public Works(GoToWork goToWork) {
            this.goToWork = goToWork;
        }

        public void go(){
            goToWork.toWork();
        }
    }
}

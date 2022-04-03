package behavioral;

public class JavaTemplate {

    public void run() {
        new Template().init();
    }

    abstract class TemplateAbstract {
        abstract void first();

        abstract void second();

        abstract void third();

        void init() {
            first();
            second();
            third();
        }
    }

    class Template extends TemplateAbstract {

        @Override
        void first() {
            System.out.println("第一个");
        }

        @Override
        void second() {
            System.out.println("第二个");
        }

        @Override
        void third() {
            System.out.println("第三个");
        }
    }
}

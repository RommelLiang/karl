package create;

public class JavaDoubleCheck {
    private volatile static JavaDoubleCheck singleton;

    private JavaDoubleCheck() {
    }

    public static JavaDoubleCheck getSingleton() {
        if (singleton == null) {
            synchronized (JavaDoubleCheck.class) {
                if (singleton == null) {
                    singleton = new JavaDoubleCheck();
                }
            }
        }
        return singleton;
    }
}

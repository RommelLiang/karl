package create;

public class JavaStaticSingleton {
    public static final JavaStaticSingleton INSTANCE = new JavaStaticSingleton();

    private JavaStaticSingleton() {
    }

    public String mKey = "";

    //other ...
}

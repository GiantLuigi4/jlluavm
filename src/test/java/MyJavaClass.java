public class MyJavaClass {
    static {
        System.loadLibrary("jni_native");
    }

    public static native long nativeMethod(Class<?> clz, String name, String sign);

    public static void thing() {
        System.out.println("thing called");
    }

    public static void main(String[] args) {
        System.out.println(nativeMethod(MyJavaClass.class, "thing", "()V"));
    }
}

import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.LUASyntaxConsumer;
import tfc.jlluavm.parse.LUAToken;
import tfc.jlluavm.parse.LUATokenizer;
import tfc.jni.ProtoJNI;

public class Test {
    public static void print(String data) {
        System.out.println(data);
    }

    public static void main(String[] args) {
        ProtoJNI.init();
        System.out.println(ProtoJNI.getJNIEnv());
        System.out.println("MID");
        long methodID = ProtoJNI.getStaticMethodID(Test.class, "print", "(Ljava/lang/String;)V");
        System.out.println(methodID);
        ProtoJNI.callStaticVoid(ProtoJNI.class, methodID, "Hello!");

        for (int i = 0; i < 1; i++) {
            LUATokenizer tokenizer = new LUATokenizer();
            LUASyntaxConsumer consumer = new LUASyntaxConsumer();

            BufferedStream<LUAToken> tokenStream = tokenizer.tokenStream("""
                    varA = 3
                    
                    if -!0 ~= 20
                    then
                        varA = varA + 0.5
                    end
                    
                    varA = varA + 3
                    
                    return varA
                    """);

            long nt = System.nanoTime();
            do {
                consumer.acceptCurrent(tokenStream);
                tokenStream.next();
            } while (!tokenStream.isDone());

            consumer.finish();

            System.out.println(System.nanoTime() - nt);
        }
    }
}

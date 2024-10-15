import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.LUASyntaxConsumer;
import tfc.jlluavm.parse.LUAToken;
import tfc.jlluavm.parse.LUATokenizer;
import tfc.jni.ProtoJNI;

public class Test {
    public static void main(String[] args) {
        ProtoJNI.init();
        System.out.println(ProtoJNI.getJNIEnv());
        System.out.println("MID");
        System.out.println(ProtoJNI.getStaticMethodID(Test.class, "main", "([Ljava/lang/String;)V"));

        for (int i = 0; i < 1; i++) {
            LUATokenizer tokenizer = new LUATokenizer();
            LUASyntaxConsumer consumer = new LUASyntaxConsumer();

            BufferedStream<LUAToken> tokenStream = tokenizer.tokenStream("""
                    varA = !0
                                        
                    for i = 100, 50, (0-1)
                    do
                        i = i + 1
                        varA = varA + i
                        for i = 100, 50, (0-1)
                        do
                            i = i + 1
                            varA = varA + i
                        end
                    end
                                        
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

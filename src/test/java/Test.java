import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.LUASyntaxConsumer;
import tfc.jlluavm.parse.LUAToken;
import tfc.jlluavm.parse.LUATokenizer;
import tfc.jni.ProtoJNI;

public class Test {
    public static void main(String[] args) {
        ProtoJNI.init();
        System.out.println(ProtoJNI.getJNIEnv());
        System.out.println(ProtoJNI.getClassID(Test.class));

        for (int i = 0; i < 1; i++) {
            LUATokenizer tokenizer = new LUATokenizer();
            LUASyntaxConsumer consumer = new LUASyntaxConsumer();

            BufferedStream<LUAToken> tokenStream = tokenizer.tokenStream("""
                    varA = !0
                    varB = 20
                    
                    if varA > varB
                    then
                        varB = varA - 1
                    elseif varA == varB
                    then
                        varA = varA + 1
                        varB = 0
                    else
                        varA = varB - 1
                    end
                    
                    return varA - varB
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

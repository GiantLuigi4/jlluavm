import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.LUASyntaxConsumer;
import tfc.jlluavm.parse.LUAToken;
import tfc.jlluavm.parse.LUATokenizer;

public class Test {
    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            LUATokenizer tokenizer = new LUATokenizer();
            LUASyntaxConsumer consumer = new LUASyntaxConsumer();

            BufferedStream<LUAToken> tokenStream = tokenizer.tokenStream("""
                    local varE = 5 + 3
                    local varE = varE + 6
                    
                    for i=5, 10, 1 do
                        local varE = varE + 1
                    end
                    
                    if varE <= 5
                    then
                        local varE = 0
                    elseif varE >= 20
                    then
                        local varE = 2.5
                        
                        for i=5, 10, 1 do
                            local varE = varE + 1
                            for i1=5, 10, 1 do
                                local varE = varE + 1
                            end
                        end
                    end
                    
                    return varE
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

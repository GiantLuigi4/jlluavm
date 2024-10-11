import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.LUAToken;
import tfc.jlluavm.parse.LUATokenizer;

public class LAHTest {
    public static void main(String[] args) {
        LUATokenizer tokenizer = new LUATokenizer();
        BufferedStream<LUAToken> tokenStream = tokenizer.tokenStream("""
                0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16
                """);

        for (int i = 1; i <= 16; i++) {
            System.out.println(tokenStream.peekFuture(i).text);
        }

        tokenStream.advance(16);
        for (int i = 0; i < tokenStream.bufferLen; i++) {
            System.out.println(tokenStream.peekHistory(i).text);
        }
    }
}

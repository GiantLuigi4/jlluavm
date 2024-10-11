package tfc.jlluavm.parse;

public class LUAToken {
    public final String type;
    public final String text;
    public final boolean skip;

    public LUAToken(String type, String text) {
        this(type, text, false);
    }

    public LUAToken(String type, String text, boolean skip) {
        this.type = type;
        this.text = text;
        this.skip = skip;
    }

    public static LUAToken[] toTokens(String type, String[] texts) {
        LUAToken[] tokens = new LUAToken[texts.length];
        for (int i = 0; i < texts.length; i++) {
            tokens[i] = new LUAToken(type, texts[i]);
        }
        return tokens;
    }
}

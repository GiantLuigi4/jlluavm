package tfc.jlluavm.parse.util;

import tfc.jlluavm.parse.LUAToken;

public class Resolver {
    public enum ThingType {
        VARIABLE,
        CALL,
        NUMBER,
        OPERATION,
        GENERIC,
        FUNCTION,
        RETURN,
        FOR_LOOP,
        IF,
        ELSE,
        ELIF,
        DO,
        EOF;
    }

    public static ThingType nextThing(BufferedStream<LUAToken> stream, int index) {
        LUAToken current = stream.peekFuture(index);

        if (current == null) return ThingType.EOF;

        if (current.text.equals("function"))
            return ThingType.FUNCTION;
        if (current.text.equals("local"))
            return nextThing(stream, index + 1);

        if (current.type.equals("numeric")) return ThingType.NUMBER;

        if (current.type.equals("literal")) {
            LUAToken nextElem = stream.peekFuture(index + 1);

            if (nextElem == null) return ThingType.GENERIC;

            if (nextElem.text.equals("="))
                return ThingType.VARIABLE;
            else if (nextElem.text.equals("("))
                return ThingType.CALL;
        }

        return switch (current.text) {
            case "for" -> ThingType.FOR_LOOP;
            case "if" -> ThingType.IF;
            case "else" -> ThingType.ELSE;
            case "elseif" -> ThingType.ELIF;
            case "return" -> ThingType.RETURN;
            case "do" -> ThingType.DO;
            case "*", "+", "-", "/", "^", "//", "not", "#", "~",
                    "<=", "==", ">=", "!=", "<", ">" -> ThingType.OPERATION;
            default -> ThingType.GENERIC;
        };
    }

    public static ThingType nextThing(BufferedStream<LUAToken> stream) {
        return nextThing(stream, 0);
    }
}

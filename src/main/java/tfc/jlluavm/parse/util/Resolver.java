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
        RETURN;
    }

    public static ThingType nextThing(BufferedStream<LUAToken> stream, int index) {
        LUAToken current = stream.peekFuture(index);

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
            case "*", "+", "-", "/", "^", "//", "not", "#", "~" -> ThingType.OPERATION;
            case "return" -> ThingType.RETURN;
            default -> ThingType.GENERIC;
        };
    }

    // order:
    // Exponentiation (^)
    // Unary operators (not, #, ~, etc.)
    // Multiplication and division (*, /, //, etc.)
    // Addition and subtraction (+, -)

    public static ThingType nextThing(BufferedStream<LUAToken> stream) {
        return nextThing(stream, 0);
    }
}

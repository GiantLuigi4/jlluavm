package tfc.jlluavm.parse;

import tfc.jlluavm.parse.util.BufferedStream;

public class LUATokenizer {
    LUAToken[] keywords = LUAToken.toTokens("keywords", new String[]{
            "and", "break", "do", "else", "elseif",
            "end", "false", "for", "function", "if",
            "in", "local", "nil", "not", "or",
            "repeat", "return", "then", "true", "until", "while",
    });

    LUAToken[] tokens = LUAToken.toTokens("symbols", new String[]{
            "+", "- ", "*", "/", "%", "^", "#",
            "==", "~=", "<=", ">=", "<", ">", "=",
            "(", ") ", "{", "}", "[", "]",
            ";", ": ", ",", ".", "..", "...",
    });

    public BufferedStream<LUAToken> tokenStream(String text) {
        return new BufferedStream<>(1) {
            int textIndex = 0;

            private boolean isEmpty() {
                return textIndex >= text.length();
            }

            boolean skip = false;

            @Override
            protected boolean $shouldSkip() {
                return skip;
            }

            @Override
            protected LUAToken $next() {
                this.skip = false;
                if (isEmpty()) {
                    return null;
                }

                char cChar = text.charAt(textIndex);
                if (Character.isLetter(cChar)) {
                    for (LUAToken keyword : keywords) {
                        if (text.startsWith(keyword.text, textIndex)) {
                            textIndex += keyword.text.length();
                            return keyword;
                        }
                    }
                } else if (!Character.isDigit(cChar) && !Character.isWhitespace(cChar)) {
                    for (LUAToken token : tokens) {
                        if (text.startsWith(token.text, textIndex)) {
                            textIndex += token.text.length();
                            return token;
                        }
                    }
                }
                if (Character.isWhitespace(cChar)) {
                    StringBuilder built = new StringBuilder();
                    while (Character.isWhitespace(cChar)) {
                        built.append(cChar);
                        textIndex += 1;
                        if (isEmpty()) break;
                        cChar = text.charAt(textIndex);
                    }
                    this.skip = true;
                    return new LUAToken("whitespace", built.toString(), true);
                } else {
                    int type;
                    if (Character.isLetter(cChar)) type = 1;
                    else if (Character.isDigit(cChar)) type = 2;
                    else type = 3;

                    StringBuilder built = new StringBuilder();
                    while (true) {
                        built.append(cChar);
                        textIndex += 1;
                        if (isEmpty()) break;
                        cChar = text.charAt(textIndex);

                        if (Character.isWhitespace(cChar))
                            break;

                        boolean bV = switch (type) {
                            case 1 -> !Character.isLetterOrDigit(cChar);
                            case 2 -> !Character.isDigit(cChar);
                            case 3 -> true; // when does this case occur, actually?
                            default -> throw new RuntimeException("NYI or invalid");
                        };
                        if (bV) break;
                    }
                    return new LUAToken(
                            switch (type) {
                                case 1 -> "literal";
                                case 2 -> "numeric";
                                case 3 -> "unknown_symbol";
                                case 4 -> "string";
                                default -> throw new RuntimeException("");
                            },
                            built.toString()
                    );
                }
            }
        };
    }
}

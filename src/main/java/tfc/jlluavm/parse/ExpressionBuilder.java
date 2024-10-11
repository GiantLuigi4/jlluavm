package tfc.jlluavm.parse;

import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import tfc.jlluavm.parse.llvm.LLVMBuilderRoot;

import java.util.ArrayList;
import java.util.List;

public class ExpressionBuilder {
    List<LLVMValueRef> refs = new ArrayList<>();
    List<Operation> ops = new ArrayList<>();

    public LLVMValueRef build(LLVMBuilderRoot builderRoot) {
        for (int precedence = 0; precedence <= lastPrecedence; precedence++) {
            for (int i = 0; i < refs.size() - 1; i++) {
                Operation op = ops.get(i);
                if (op.precedence == precedence) {
                    ops.remove(i);
                    LLVMValueRef refL = refs.remove(i);
                    LLVMValueRef refR = refs.remove(i);

                    switch (op) {
                        case MUL -> {
                            refs.add(i, LLVM.LLVMBuildFMul(builderRoot.direct(), refL, refR, builderRoot.nextDescriminator("add")));
                        }
                        case DIV -> {
                            refs.add(i, LLVM.LLVMBuildFDiv(builderRoot.direct(), refL, refR, builderRoot.nextDescriminator("add")));
                        }
                        case ADD -> {
                            refs.add(i, LLVM.LLVMBuildFAdd(builderRoot.direct(), refL, refR, builderRoot.nextDescriminator("add")));
                        }
                        case SUB -> {
                            refs.add(i, LLVM.LLVMBuildFSub(builderRoot.direct(), refL, refR, builderRoot.nextDescriminator("sub")));
                        }
                    }

                    i -= 1;
                }
            }
        }
        return refs.get(0);
    }

    public void addValue(LLVMValueRef ref) {
        refs.add(ref);
    }

    // order:
    // Exponentiation (^)
    // Unary operators (not, #, ~, etc.)
    // Multiplication and division (*, /, //, etc.)
    // Addition and subtraction (+, -)

    public enum Operation {
        EXP('^', 0),
        NOT("not", 1), LEN('#', 1),
        MUL('*', 2), DIV('/', 2),
        ADD('+', 3), SUB('-', 3),
        ;

        public final String symb;
        public final int precedence;

        Operation(String symb, int precedence) {
            this.symb = symb;
            this.precedence = precedence;
        }

        Operation(char symb, int precedence) {
            this.symb = String.valueOf(symb);
            this.precedence = precedence;
        }
    }

    private static final Operation[] opers = Operation.values();
    public static final int lastPrecedence = opers[opers.length - 1].precedence;

    public void addOperation(String text) {
//        5 + 5 * 2
        for (Operation oper : opers) {
            if (oper.symb.equals(text)) {
                ops.add(oper);
                return;
            }
        }
    }
}

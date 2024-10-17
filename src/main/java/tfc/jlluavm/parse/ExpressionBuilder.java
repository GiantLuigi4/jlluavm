package tfc.jlluavm.parse;

import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMFunctionBuilder;

import java.util.ArrayList;
import java.util.List;

public class ExpressionBuilder {
    List<LUAValue> refs = new ArrayList<>();
    List<Operation> ops = new ArrayList<>();

    LUAValue toLUA(LLVMBuilderRoot root, LLVMValueRef typeL, LLVMValueRef typeR, LLVMValueRef refOut) {
        // TODO: conjoin types
        return new LUAValue(false, typeL, root.bitCast(refOut, root.LONG));
    }

    LUAValue toLUA(LLVMBuilderRoot root, LLVMValueRef typeOut, LLVMValueRef refOut) {
        // TODO: conjoin types
        return new LUAValue(false, typeOut, root.bitCast(refOut, root.LONG));
    }

    public LUAValue build(LLVMFunctionBuilder function, LLVMBuilderRoot builderRoot) {
        for (int precedence = 0; precedence <= lastPrecedence; precedence++) {
            for (int i = 0; i < refs.size() - 1; i++) {
                Operation op = ops.get(i);
                if (op.precedence == precedence) {
                    ops.remove(i);
                    LUAValue valL = refs.remove(i);
                    LUAValue valR = refs.remove(i);

                    valL = valL.coerce(function, builderRoot, valR);
                    valR = valR.coerce(function, builderRoot, valL);

                    LLVMValueRef refL = valL.getData(builderRoot, builderRoot.DOUBLE);
                    LLVMValueRef refR = valR.getData(builderRoot, builderRoot.DOUBLE);
//                    LLVMValueRef refL = valL.data;
//                    LLVMValueRef refR = valR.data;

                    switch (op) {
                        case MUL -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    builderRoot.trackValue(LLVM.LLVMBuildFMul(builderRoot.direct(), refL, refR, builderRoot.nextDiscriminator("mul"))))
                            );
                        }
                        case DIV -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    builderRoot.trackValue(LLVM.LLVMBuildFDiv(builderRoot.direct(), refL, refR, builderRoot.nextDiscriminator("div"))))
                            );
                        }
                        case ADD -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    builderRoot.trackValue(LLVM.LLVMBuildFAdd(builderRoot.direct(), refL, refR, builderRoot.nextDiscriminator("add"))))
                            );
                        }
                        case SUB -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    builderRoot.trackValue(LLVM.LLVMBuildFSub(builderRoot.direct(), refL, refR, builderRoot.nextDiscriminator("sub"))))
                            );
                        }

                        case GE -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    builderRoot.CONST_2B,
                                    builderRoot.extend(
                                            builderRoot.compareGE(refL, refR),
                                            builderRoot.LONG
                                    ))
                            );
                        }
                        case LE -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    builderRoot.CONST_2B,
                                    builderRoot.extend(
                                            builderRoot.compareLE(refL, refR),
                                            builderRoot.LONG
                                    ))
                            );
                        }
                        case G -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    builderRoot.CONST_2B,
                                    builderRoot.extend(
                                            builderRoot.compareG(refL, refR),
                                            builderRoot.LONG
                                    ))
                            );
                        }
                        case L -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    builderRoot.CONST_2B,
                                    builderRoot.extend(
                                            builderRoot.compareL(refL, refR),
                                            builderRoot.LONG
                                    ))
                            );
                        }
                        case EE -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    builderRoot.CONST_2B,
                                    builderRoot.extend(
                                            builderRoot.compareE(refL, refR),
                                            builderRoot.LONG
                                    ))
                            );
                        }
                        case NE -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    builderRoot.CONST_2B,
                                    builderRoot.extend(
                                            builderRoot.compareNE(refL, refR),
                                            builderRoot.LONG
                                    ))
                            );
                        }
                    }

                    i -= 1;
                }
            }
        }
        return refs.get(0);
    }

    public void addValue(LUAValue ref) {
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

        LE("<=", 4), GE(">=", 4),
        L('<', 4), G('>', 4),
        EE("==", 4), NE("!=", 4);

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

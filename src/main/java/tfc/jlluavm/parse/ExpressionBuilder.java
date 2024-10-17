package tfc.jlluavm.parse;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMFunctionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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

    public LLVMValueRef op(
            LLVMFunctionBuilder builder, LLVMBuilderRoot root,
            LUAValue left, LUAValue right,
            BiFunction<LLVMValueRef, LLVMValueRef, LLVMValueRef> emitLong,
            BiFunction<LLVMValueRef, LLVMValueRef, LLVMValueRef> emitDouble,
            BiFunction<LLVMValueRef, LLVMValueRef, LLVMValueRef> emitBool,
            BiFunction<LLVMValueRef, LLVMValueRef, LLVMValueRef> emitString,
            String luaTableValueName
    ) {
        LLVMValueRef typeLeft = left.type;
        LLVMValueRef typeRight = right.type;

        LLVMBasicBlockRef hndlInt = builder.createBlock(luaTableValueName + "_iInt");
        LLVMBasicBlockRef hndlFloat = builder.createBlock(luaTableValueName + "_iFloat");
        LLVMBasicBlockRef cmpFloat = builder.createBlock(luaTableValueName + "_cmpFloat");
        LLVMBasicBlockRef thrw = builder.createBlock(luaTableValueName + "_throw");
        LLVMBasicBlockRef end = builder.createBlock(luaTableValueName + "_end_op");

        LLVMValueRef out = root.alloca(root.LONG, luaTableValueName + "_result_buffer");
        root.conditionalJump(root.intCompareE(typeLeft, root.CONST_0B), hndlInt, cmpFloat);

        // TODO: funny story: metatables
        builder.buildBlock(hndlInt);
        root.setValue(
                out,
                root.bitCast(
                        emitLong.apply(root.bitCast(left.data, root.LONG), root.bitCast(right.data, root.LONG)),
                        root.LONG
                )
        );
        root.jump(end);

        builder.buildBlock(cmpFloat);
        root.conditionalJump(root.intCompareE(typeLeft, root.CONST_1B), hndlFloat, thrw);

        builder.buildBlock(hndlFloat);
        root.setValue(
                out,
                root.bitCast(
                        emitDouble.apply(root.bitCast(left.data, root.DOUBLE), root.bitCast(right.data, root.DOUBLE)),
                        root.LONG
                )
        );
        root.jump(end);

        builder.buildBlock(thrw);
        root.unreachable();

        builder.buildBlock(end);

        return root.getValue(root.LONG, out);
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

                    switch (op) {
                        case MUL -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    op(
                                            function, builderRoot,
                                            valL, valR,
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildMul(
                                                        builderRoot.direct(),
                                                        left, right, "int_add"
                                                ));
                                            },
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildFMul(
                                                        builderRoot.direct(),
                                                        left, right, "float_add"
                                                ));
                                            },
                                            null, null,
                                            "add"
                                    )
                            ));
                        }
                        case DIV -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    op(
                                            function, builderRoot,
                                            valL, valR,
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildSDiv(
                                                        builderRoot.direct(),
                                                        left, right, "int_add"
                                                ));
                                            },
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildFDiv(
                                                        builderRoot.direct(),
                                                        left, right, "float_add"
                                                ));
                                            },
                                            null, null,
                                            "add"
                                    )
                            ));
                        }
                        case ADD -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    op(
                                            function, builderRoot,
                                            valL, valR,
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildAdd(
                                                        builderRoot.direct(),
                                                        left, right, "int_add"
                                                ));
                                            },
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildFAdd(
                                                        builderRoot.direct(),
                                                        left, right, "float_add"
                                                ));
                                            },
                                            null, null,
                                            "add"
                                    )
                            ));
                        }
                        case SUB -> {
                            refs.add(i, toLUA(
                                    builderRoot,
                                    valL.type,
                                    valR.type,
                                    op(
                                            function, builderRoot,
                                            valL, valR,
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildSub(
                                                        builderRoot.direct(),
                                                        left, right, "int_add"
                                                ));
                                            },
                                            (left, right) -> {
                                                return builderRoot.trackValue(LLVM.LLVMBuildFSub(
                                                        builderRoot.direct(),
                                                        left, right, "float_add"
                                                ));
                                            },
                                            null, null,
                                            "add"
                                    )
                            ));
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

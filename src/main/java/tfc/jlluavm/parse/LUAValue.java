package tfc.jlluavm.parse;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMFunctionBuilder;

public class LUAValue {
    public final LLVMValueRef type;
    public final LLVMValueRef data;

    static long DBG_TYPE_BYTE = 0;

    public final boolean ptr;

    public LUAValue(boolean ptr, LLVMValueRef type, LLVMValueRef data) {
        this.ptr = ptr;
        this.type = type;
        this.data = data;
    }

    public LUAValue(LLVMValueRef type, LLVMValueRef data) {
        this.type = type;
        this.data = data;
        ptr = true;
    }

    public LUAValue(LLVMBuilderRoot root, int type, LLVMValueRef data) {
        this.type = switch (type) {
            case 0 -> root.CONST_0B; // int/long
            case 1 -> root.CONST_1B; // float/double
            case 2 -> root.CONST_2B; // boolean
            case 3 -> root.CONST_3B; // string
            case 4 -> root.CONST_4B; // table
            case 5 -> root.CONST_5B; // function
            case 6 -> root.CONST_6B; // jni function
            default -> throw new RuntimeException("NYI");
        };
        // bool cannot be cast to int/long
        if (type != 2) this.data = root.bitCast(data, root.LONG);
        else this.data = data;
        ptr = false;
    }

    @Deprecated
    public LLVMValueRef getData(LLVMBuilderRoot root, LLVMTypeRef targetType) {
        return root.bitCast(data, targetType);
    }

    // TODO: find a way to prevent type coercion spamming when types can established ahead of time to already be the same
    public LUAValue coerce(LLVMFunctionBuilder builder, LLVMBuilderRoot root, LUAValue other) {
        LLVMValueRef typeOut = root.alloca(root.BYTE, "tmp_type");
        root.setValue(typeOut, type);
        LLVMValueRef valOut = root.alloca(root.LONG, "tmp_value");
        root.setValue(valOut, root.bitCast(data, root.LONG));

        LLVMBasicBlockRef continue_comp_ld = builder.createBlock("check_ld");
        LLVMBasicBlockRef long_double = builder.createBlock("coerce_long_double");
        LLVMBasicBlockRef end = builder.createBlock("coerce_end");

        LLVMValueRef myType = type;
        LLVMValueRef otherType = other.type;

        LLVMValueRef isEq = root.intCompareE(myType, otherType);
        {
            root.conditionalJump(isEq, end, continue_comp_ld);
        }

        {
            builder.buildBlock(continue_comp_ld);
            LLVMValueRef isS0 = root.intCompareE(myType, root.CONST_0B);
            LLVMValueRef isO1 = root.intCompareE(otherType, root.CONST_1B);
            LLVMValueRef toDouble = root.and(isS0, isO1);
            root.conditionalJump(toDouble, long_double, end);
        }

        {
            builder.buildBlock(long_double);
            LLVMValueRef cast = root.cast(LLVMBuilderRoot.CastOp.SIGNED_INT_TO_FLOAT, data, root.DOUBLE);
            cast = root.bitCast(cast, root.LONG);
            root.setValue(typeOut, other.type);
            root.setValue(valOut, cast);
            root.jump(end);
        }

        builder.buildBlock(end);
        return new LUAValue(
                false,
                root.getValue(root.BYTE, typeOut),
                root.getValue(root.LONG, valOut)
        );
    }
}

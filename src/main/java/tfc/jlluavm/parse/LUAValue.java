package tfc.jlluavm.parse;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import tfc.llvmutil.LLVMBuilderRoot;

public class LUAValue {
    public final LLVMValueRef type;
    public final LLVMValueRef data;

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
        if (type != 2) this.data = root.cast(data, root.LONG);
        else this.data = data;
        ptr = false;
    }

    public LLVMValueRef getData(LLVMBuilderRoot root, LLVMTypeRef targetType) {
        return root.cast(data, targetType);
    }
}

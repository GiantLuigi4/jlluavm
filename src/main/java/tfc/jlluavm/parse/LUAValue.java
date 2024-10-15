package tfc.jlluavm.parse;

import org.bytedeco.llvm.LLVM.LLVMValueRef;
import tfc.llvmutil.LLVMBuilderRoot;

public class LUAValue {
    LLVMValueRef type;
    LLVMValueRef data;

    public LUAValue(LLVMValueRef type, LLVMValueRef data) {
        this.type = type;
        this.data = data;
    }

    public LUAValue(LLVMBuilderRoot root, int type, LLVMValueRef data) {
        this.type = switch (type) {
            case 0 -> root.CONST_0B;
            case 1 -> root.CONST_1B;
            case 2 -> root.CONST_2B;
            case 3 -> root.CONST_3B;
            case 4 -> root.CONST_4B;
            case 5 -> root.CONST_5B;
            default -> throw new RuntimeException("NYI");
        };
        this.data = data;
    }
}

package tfc.jlluavm.parse.llvm;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;

import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMHelper {
    public static LLVMFunctionBuilder emitFunction(
            LLVMBuilderRoot root,
            String name,
            LLVMTypeRef type
    ) {
        return new LLVMFunctionBuilder(
                root,
                root.trackValue(LLVMAddFunction(root.module, name, type)),
                type,
                name
        );
    }

    public static LLVMValueRef getParam(LLVMValueRef function, int index) {
        return LLVMGetParam(function, index);
    }
}

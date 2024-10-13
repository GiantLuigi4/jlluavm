package tfc.llvmutil;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;

import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMHelper {
    public static LLVMFunctionBuilder emitFunction(
            LLVMBuilderRoot root,
            String name,
            LLVMTypeRef type
    ) {
        return emitFunction(true, root, name, type);
    }

    public static LLVMFunctionBuilder emitFunction(
            boolean withBody,
            LLVMBuilderRoot root,
            String name,
            LLVMTypeRef type
    ) {
        return new LLVMFunctionBuilder(
                withBody,
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

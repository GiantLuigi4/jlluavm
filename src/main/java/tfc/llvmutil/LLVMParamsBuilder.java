package tfc.llvmutil;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;

import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.llvm.global.LLVM.LLVMFunctionType;

public class LLVMParamsBuilder {
    LLVMBuilderRoot root;
    List<LLVMTypeRef> args = new ArrayList<>();

    public LLVMParamsBuilder(LLVMBuilderRoot root) {
        this.root = root;
    }

    public LLVMTypeRef build(LLVMTypeRef ret) {
        PointerPointer<LLVMTypeRef> paramsPtr = root.trackValue(new PointerPointer<>(args.size()));
        for (int i = 0; i < args.size(); i++)
            paramsPtr.put(i, root.trackValue(args.get(i)));

        return root.trackValue(
                LLVMFunctionType(
                        ret,
                        paramsPtr,
                        args.size(), 0
                )
        );
    }

    public LLVMParamsBuilder addArg(LLVMTypeRef type) {
        args.add(type);
        return this;
    }

    public LLVMParamsBuilder addArg(LLVMStructBuilder type) {
        args.add(type.struct);
        return this;
    }
}

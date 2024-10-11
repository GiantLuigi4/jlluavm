package tfc.jlluavm.exec;

import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.global.LLVM;

public class LuaExecutable {
    LLVMContextRef context;
    LLVMModuleRef moduleRef;

    public void free() {
        LLVM.LLVMDisposeModule(moduleRef);
    }
}

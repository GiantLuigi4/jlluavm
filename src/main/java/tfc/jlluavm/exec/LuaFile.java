package tfc.jlluavm.exec;

import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.global.LLVM;

import java.util.ArrayList;
import java.util.List;

public class LuaFile {
    LLVMContextRef context;
    LLVMModuleRef moduleRef;

    List<LuaFunction> functions = new ArrayList<>();

    public void free() {
        LLVM.LLVMDisposeModule(moduleRef);
    }
}

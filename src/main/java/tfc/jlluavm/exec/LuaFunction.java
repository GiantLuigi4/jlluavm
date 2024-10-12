package tfc.jlluavm.exec;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class LuaFunction {
    public final int paramCount;
    public final boolean isLocal;

    private final LLVMValueRef function;

    public LuaFunction(int paramCount, boolean isLocal, LLVMValueRef function) {
        this.paramCount = paramCount;
        this.isLocal = isLocal;
        this.function = function;
    }
}

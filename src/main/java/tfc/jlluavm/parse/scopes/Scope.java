package tfc.jlluavm.parse.scopes;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMFunctionBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Scope {
    private final LLVMFunctionBuilder function;
    private final Set<String> myVars = new HashSet<>();
    LLVMBuilderRoot root;
    GlobalScope globalScope;
    Scope parentScope;

    public Scope(GlobalScope globalScope, Scope parentScope, LLVMBuilderRoot root, LLVMFunctionBuilder function) {
        this.globalScope = globalScope;
        this.parentScope = parentScope;
        this.root = root;
        this.function = function;
    }

    HashMap<String, LLVMValueRef> variables = new HashMap<>();

    public void addVariable(boolean local, String name, LLVMValueRef value) {
        LLVMValueRef refr = variables.get(name);
        if (refr == null) {
            if (local || parentScope == null) {
                LLVMValueRef ptr = root.alloca(root.LONG);
                root.setValue(ptr, root.cast(value, root.LONG));
                variables.put(name, ptr);
            } else {
                parentScope.addVariable(local, name, value);
            }
        } else root.setValue(refr, root.cast(value, root.LONG));
    }

    public LLVMValueRef getVariable(LLVMTypeRef type, String var) {
        LLVMValueRef ref = variables.get(var);
        if (ref == null) {
            Scope toCheck = parentScope;
            if (toCheck == null) toCheck = globalScope;
            return toCheck.getVariable(type, var);
        }
        return root.cast(root.getValue(root.LONG, ref), type);
    }
}

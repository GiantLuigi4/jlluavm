package tfc.jlluavm.parse.scopes;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import tfc.jlluavm.parse.LUAValue;
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

    HashMap<String, LUAValue> variables = new HashMap<>();

    // TODO: type needs to be a dynamic value
    public void addVariable(boolean local, String name, LUAValue value) {
        LUAValue refr = variables.get(name);
        if (refr == null) {
            if (local || parentScope == null) {
                LLVMValueRef ptrT = root.alloca(root.BYTE, name + "_type_ptr");
                root.setValue(ptrT, root.bitCast(value.type, root.BYTE));

                LLVMValueRef ptr = root.alloca(root.LONG, name + "_value_ptr");
                root.setValue(ptr, root.bitCast(value.data, root.LONG));
                variables.put(name, new LUAValue(
                        ptrT, ptr
                ));
            } else {
                parentScope.addVariable(local, name, value);
            }
        } else {
            root.setValue(refr.type, root.bitCast(value.type, root.BYTE));
            root.setValue(refr.data, root.bitCast(value.data, root.LONG));
        }
    }

    public LUAValue getVariable(LLVMTypeRef type, String var) {
        LUAValue ref = variables.get(var);
        if (ref == null) {
            Scope toCheck = parentScope;
            if (toCheck == null) toCheck = globalScope;
            return toCheck.getVariable(type, var);
        }
        return new LUAValue(
                false,
                root.bitCast(root.getValue(root.BYTE, ref.type), root.BYTE),
                root.bitCast(root.getValue(root.LONG, ref.data), type)
        );
    }
}

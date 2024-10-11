package tfc.jlluavm.parse.llvm;

import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMFunctionBuilder {
    public final LLVMValueRef function;
    public final LLVMTypeRef type;

    LLVMBuilderRoot builder;
    HashMap<Integer, LLVMValueRef> params = new HashMap<>();
    LLVMBasicBlockRef root;

    List<LLVMBasicBlockRef> blocks = new ArrayList<>();
    LLVMBasicBlockRef active;

    String name;

    public LLVMFunctionBuilder(LLVMBuilderRoot builder, LLVMValueRef function, LLVMTypeRef type, String name) {
        this.builder = builder;
        this.function = function;
        this.type = type;
        this.name = name;

        LLVMSetFunctionCallConv(function, LLVMCCallConv);
        root = blockFor(function, "entry");
        active = root;
    }

    public LLVMFunctionBuilder buildRoot() {
        builder.position(root);
        active = root;
        return this;
    }

    public LLVMFunctionBuilder buildBlock(String name) {
        active = blockFor(function, name);
        return this;
    }

    public void resumeBuilding() {
        builder.position(active);
    }

    // TODO: uniquify block names?
    public LLVMBasicBlockRef blockFor(LLVMValueRef function, String name) {
        return LLVMAppendBasicBlock(function, name);
    }

    public LLVMValueRef getParam(int index) {
        LLVMValueRef ref = params.get(index);
        if (ref == null) {
            params.put(
                    index,
                    ref = LLVMHelper.getParam(function, index)
            );
        }
        return ref;
    }

    HashMap<String, LLVMValueRef> variables = new HashMap<>();

    public void addVariable(String name, LLVMValueRef value) {
        variables.put(name, value);
    }

    public LLVMValueRef getVariable(String var) {
        return variables.get(var);
    }

    public void ret(LLVMValueRef ref) {
        builder.trackValue(LLVMBuildRet(builder.builder, ref));
        // TODO: HUH? why does this return a value ref???
    }

    public void ret() {
        builder.trackValue(LLVMBuildRetVoid(builder.builder));
        // TODO: HUH? why does this return a value ref???
    }

    public boolean verifyFunction() {
        if (LLVM.LLVMVerifyFunction(function, LLVMPrintMessageAction) != 0) {
            System.err.println("Failed to verify function " + name);
            return false;
        }
        return true;
    }
}

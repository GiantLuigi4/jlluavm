package tfc.llvmutil;

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

    public LLVMFunctionBuilder(boolean withBody, LLVMBuilderRoot builder, LLVMValueRef function, LLVMTypeRef type, String name) {
        this.builder = builder;
        this.function = function;
        this.type = type;
        this.name = name;

        LLVMSetFunctionCallConv(function, LLVMCCallConv);
        if (withBody) {
            root = makeBlock("entry");
            active = root;
        }
    }

    public LLVMFunctionBuilder buildRoot() {
        builder.position(root);
        active = root;
        return this;
    }

    public LLVMFunctionBuilder buildBlock(LLVMBasicBlockRef block) {
        active = block;
        resumeBuilding();
        return this;
    }

    public LLVMBasicBlockRef buildBlock(String name) {
        active = makeBlock(name);
        resumeBuilding();
        return active;
    }

    public void resumeBuilding() {
        builder.position(active);
    }

    public LLVMValueRef getParamAsDouble(int index) {
        LLVMValueRef ref = params.get(index);
        if (ref == null) {
            ref = LLVMHelper.getParam(function, index);
            params.put(
                    index,
                    ref = builder.cast(ref, builder.DOUBLE)
            );
        }
        return ref;
    }

    public LLVMValueRef getParam(int index, LLVMTypeRef type) {
        LLVMValueRef ref = params.get(index);
        if (ref == null) {
            ref = LLVMHelper.getParam(function, index);
            params.put(
                    index,
                    ref = builder.cast(ref, type)
            );
        }
        return ref;
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

    public LLVMBasicBlockRef createBlock(String name) {
        return makeBlock(name);
    }

    protected LLVMBasicBlockRef makeBlock(String name) {
        return builder.trackValue(LLVMAppendBasicBlock(function, name));
    }

    public LLVMBasicBlockRef activeBlock() {
        return active;
    }

    public String getName() {
        return name;
    }
}

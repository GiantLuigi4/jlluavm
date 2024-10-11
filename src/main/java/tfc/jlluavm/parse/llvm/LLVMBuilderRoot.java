package tfc.jlluavm.parse.llvm;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;

import java.util.HashSet;
import java.util.Set;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMBuilderRoot {
    public final LLVMTypeRef BYTE;
    public final LLVMTypeRef SHORT;
    public final LLVMTypeRef INT;
    public final LLVMTypeRef LONG;
    public final LLVMTypeRef HALF;
    public final LLVMTypeRef FLOAT;
    public final LLVMTypeRef DOUBLE;

    LLVMContextRef context;
    LLVMModuleRef module;
    LLVMBuilderRef builder;

    public LLVMBuilderRoot(String name) {
        context = LLVMContextCreate();
        module = LLVMModuleCreateWithNameInContext(
                name,
                context
        );
        builder = LLVMCreateBuilderInContext(context);

        BYTE = LLVM.LLVMInt8TypeInContext(context);
        SHORT = LLVM.LLVMInt16TypeInContext(context);
        INT = LLVM.LLVMInt32TypeInContext(context);
        LONG = LLVM.LLVMInt64TypeInContext(context);

        HALF = LLVM.LLVMHalfTypeInContext(context);
        FLOAT = LLVM.LLVMFloatTypeInContext(context);
        DOUBLE = LLVM.LLVMDoubleTypeInContext(context);
    }

    public void position(LLVMBasicBlockRef root) {
        LLVMPositionBuilderAtEnd(builder, root);
    }

    public void disposeBuilder() {
        LLVMDisposeBuilder(builder);
    }

    private final BytePointer error = new BytePointer();

    public void verify() {
        if (LLVMVerifyModule(module, LLVMPrintMessageAction, error) != 0) {
            LLVMDisposeMessage(error);
            throw new RuntimeException("Failed to verify module");
        }
    }

    public LLVMFunctionBuilder function(String name, LLVMTypeRef type) {
        return LLVMHelper.emitFunction(this, name, type);
    }

    int load_indx = 0;

    public LLVMValueRef loadDouble(double value) {
        return cast(
                loadLong(Double.doubleToLongBits(value)),
                DOUBLE
        );
    }

    public LLVMValueRef loadFloat(float value) {
        return cast(
                loadInt(Float.floatToIntBits(value)),
                FLOAT
        );
    }

    public LLVMValueRef loadByte(short value) {
        return trackValue(LLVM.LLVMConstInt(BYTE, value, 0));
    }

    public LLVMValueRef loadShort(short value) {
        return trackValue(LLVM.LLVMConstInt(SHORT, value, 0));
    }

    public LLVMValueRef loadInt(int value) {
        return trackValue(LLVM.LLVMConstInt(INT, value, 0));
    }

    public LLVMValueRef loadLong(long value) {
        return trackValue(LLVM.LLVMConstInt(LONG, value, 0));
    }

    Set<Pointer> valueRefs = new HashSet<>();

    public <T extends Pointer> T trackValue(T llvmTypeRef) {
        valueRefs.add(llvmTypeRef);
        return llvmTypeRef;
    }

    public LLVMValueRef cast(LLVMValueRef value, LLVMTypeRef toType) {
        return trackValue(LLVM.LLVMBuildBitCast(
                builder, value,
                toType, "cast" + toType.address() + "_" + (load_indx++)
        ));
    }

    public LLVMBuilderRef direct() {
        return builder;
    }

    public String nextDescriminator(String of) {
        return of + "_" + (load_indx++);
    }

    public void dump() {
        LLVMDumpModule(module);
    }

    public LLVMModuleRef getModule() {
        return module;
    }
}

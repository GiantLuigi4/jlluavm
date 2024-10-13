package tfc.llvmutil;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;

import java.util.HashSet;
import java.util.Set;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMBuilderRoot {
    static {
        LLVMInitializeNativeTarget();
        LLVMInitializeNativeAsmPrinter();
    }

    public final LLVMValueRef CONST_FALSE;
    public final LLVMValueRef CONST_TRUE;
    public final LLVMValueRef CONST_0D;
    public final LLVMValueRef CONST_1D;

    public final LLVMTypeRef BIT;
    public final LLVMTypeRef BYTE;
    public final LLVMTypeRef SHORT;
    public final LLVMTypeRef INT;
    public final LLVMTypeRef LONG;
    public final LLVMTypeRef HALF;
    public final LLVMTypeRef FLOAT;
    public final LLVMTypeRef DOUBLE;
    public final LLVMTypeRef LABEL;
    public final LLVMTypeRef VOID;

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

        BIT = LLVM.LLVMInt1TypeInContext(context);
        BYTE = LLVM.LLVMInt8TypeInContext(context);
        SHORT = LLVM.LLVMInt16TypeInContext(context);
        INT = LLVM.LLVMInt32TypeInContext(context);
        LONG = LLVM.LLVMInt64TypeInContext(context);

        HALF = LLVM.LLVMHalfTypeInContext(context);
        FLOAT = LLVM.LLVMFloatTypeInContext(context);
        DOUBLE = LLVM.LLVMDoubleTypeInContext(context);

        LABEL = LLVM.LLVMLabelTypeInContext(context);
        VOID = LLVM.LLVMVoidTypeInContext(context);

        CONST_FALSE = LLVM.LLVMConstInt(BIT, 0, 0);
        CONST_TRUE = LLVM.LLVMConstInt(BIT, 1, 0);

        CONST_0D = loadDouble(0);
        CONST_1D = loadDouble(1);
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

    public LLVMFunctionBuilder functionPrototype(String name, LLVMTypeRef type) {
        return LLVMHelper.emitFunction(false, this, name, type);
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
                toType, nextDiscriminator("cast")
        ));
    }

    public LLVMBuilderRef direct() {
        return builder;
    }

    public String nextDiscriminator(String of) {
        return of + "_" + (load_indx++);
    }

    public void dump() {
        LLVMDumpModule(module);
    }

    public LLVMModuleRef getModule() {
        return module;
    }

    public LLVMValueRef alloca(LLVMTypeRef type) {
        return trackValue(LLVM.LLVMBuildAlloca(builder, type, nextDiscriminator("alloca")));
    }

    public LLVMValueRef setValue(LLVMValueRef ptr, LLVMValueRef value) {
        return trackValue(LLVM.LLVMBuildStore(builder, value, ptr));
    }

    public LLVMValueRef getValue(LLVMTypeRef type, LLVMValueRef ptr) {
        return trackValue(LLVM.LLVMBuildLoad2(builder, type, ptr, nextDiscriminator("load")));
    }

    public LLVMValueRef sum(LLVMValueRef interm, LLVMValueRef step) {
        return trackValue(LLVM.LLVMBuildFAdd(builder, interm, step, nextDiscriminator("add")));
    }

    public LLVMValueRef compareLE(LLVMValueRef lh, LLVMValueRef rh) {
        return trackValue(LLVM.LLVMBuildFCmp(builder, LLVM.LLVMRealOLE, lh, rh, nextDiscriminator("comp")));
    }

    public LLVMValueRef compareL(LLVMValueRef lh, LLVMValueRef rh) {
        return trackValue(LLVM.LLVMBuildFCmp(builder, LLVMRealOLT, lh, rh, nextDiscriminator("comp")));
    }

    public LLVMValueRef compareG(LLVMValueRef lh, LLVMValueRef rh) {
        return trackValue(LLVM.LLVMBuildFCmp(builder, LLVMRealOGT, lh, rh, nextDiscriminator("comp")));
    }

    public LLVMValueRef compareE(LLVMValueRef lh, LLVMValueRef rh) {
        return trackValue(LLVM.LLVMBuildFCmp(builder, LLVMRealOEQ, lh, rh, nextDiscriminator("comp")));
    }

    public LLVMValueRef compareNE(LLVMValueRef lh, LLVMValueRef rh) {
        return trackValue(LLVM.LLVMBuildFCmp(builder, LLVMRealONE, lh, rh, nextDiscriminator("comp")));
    }

    public LLVMValueRef compareGE(LLVMValueRef lh, LLVMValueRef rh) {
        return trackValue(LLVM.LLVMBuildFCmp(builder, LLVM.LLVMRealOGE, lh, rh, nextDiscriminator("comp")));
    }

    public void jump(LLVMBasicBlockRef start) {
        trackValue(LLVM.LLVMBuildBr(builder, start));
    }

    public void conditionalJump(LLVMValueRef cond, LLVMBasicBlockRef iTrue, LLVMBasicBlockRef iFalse) {
        trackValue(LLVM.LLVMBuildCondBr(builder, cond, iTrue, iFalse));
    }

    public void unreachable() {
        LLVM.LLVMBuildUnreachable(builder);
    }

    public LLVMStructBuilder createStruct(String name) {
        return new LLVMStructBuilder(
                LLVM.LLVMStructCreateNamed(
                        context,
                        name
                )
        );
    }
}

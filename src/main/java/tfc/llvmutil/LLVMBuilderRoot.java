package tfc.llvmutil;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMBuilderRoot {

    static {
        LLVMInitializeNativeTarget();
        LLVMInitializeNativeAsmPrinter();

        LLVMInitializeAggressiveInstCombiner(LLVM.LLVMGetGlobalPassRegistry());
    }

    public final LLVMTypeRef LONG_PTR;
    public final LLVMTypeRef INT_PTR;
    public final LLVMTypeRef SHORT_PTR;
    public final LLVMTypeRef BYTE_PTR;
    public final LLVMTypeRef BIT_PTR;
    public final LLVMTypeRef VOID_PTR;

    public final LLVMValueRef CONST_FALSE;
    public final LLVMValueRef CONST_TRUE;

    public final LLVMValueRef CONST_NULL_BYTE;

    public final LLVMValueRef CONST_0B;
    public final LLVMValueRef CONST_1B;
    public final LLVMValueRef CONST_2B;
    public final LLVMValueRef CONST_3B;
    public final LLVMValueRef CONST_4B;
    public final LLVMValueRef CONST_5B;
    public final LLVMValueRef CONST_6B;

    public final LLVMValueRef CONST_0D;
    public final LLVMValueRef CONST_1D;

    public final LLVMValueRef CONST_0L;
    public final LLVMValueRef CONST_1L;

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
    public final LLVMBuilderRef builder;

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

        BIT_PTR = LLVM.LLVMPointerType(BIT, 0);
        BYTE_PTR = LLVM.LLVMPointerType(BYTE, 0);
        SHORT_PTR = LLVM.LLVMPointerType(SHORT, 0);
        INT_PTR = LLVM.LLVMPointerType(INT, 0);
        LONG_PTR = LLVM.LLVMPointerType(LONG, 0);
        VOID_PTR = LLVM.LLVMPointerType(VOID, 0);

        CONST_FALSE = LLVM.LLVMConstInt(BIT, 0, 0);
        CONST_TRUE = LLVM.LLVMConstInt(BIT, 1, 0);

        CONST_NULL_BYTE = LLVM.LLVMConstNull(BYTE);

        CONST_0D = loadDouble(0);
        CONST_1D = loadDouble(1);

        CONST_0L = loadLong(0);
        CONST_1L = loadLong(1);

        CONST_0B = loadByte((byte) 0);
        CONST_1B = loadByte((byte) 1);
        CONST_2B = loadByte((byte) 2);
        CONST_3B = loadByte((byte) 3);
        CONST_4B = loadByte((byte) 4);
        CONST_5B = loadByte((byte) 5);
        CONST_6B = loadByte((byte) 6);
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

    List<LLVMFunctionBuilder> functions = new ArrayList<>();

    public LLVMFunctionBuilder function(String name, LLVMTypeRef type) {
        LLVMFunctionBuilder func = LLVMHelper.emitFunction(this, name, type);
        functions.add(func);
        return func;
    }

    public LLVMFunctionBuilder functionPrototype(String name, LLVMTypeRef type) {
        LLVMFunctionBuilder func = LLVMHelper.emitFunction(false, this, name, type);
        LLVMSetLinkage(func.function, LLVMExternalLinkage);
        functions.add(func);
        return func;
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

    public LLVMValueRef intCast(LLVMValueRef value, LLVMTypeRef toType) {
        return trackValue(LLVM.LLVMBuildIntCast(
                builder, value,
                toType, nextDiscriminator("cast")
        ));
    }

    public LLVMValueRef ptrCast(LLVMValueRef value, LLVMTypeRef toType) {
        return trackValue(LLVM.LLVMBuildPointerCast(
                builder, value,
                toType, nextDiscriminator("cast")
        ));
    }

    public LLVMValueRef addrSpaceCast(LLVMValueRef value, LLVMTypeRef toType) {
        return trackValue(LLVM.LLVMBuildAddrSpaceCast(
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
        return alloca(type, "alloca");
    }

    public LLVMValueRef alloca(LLVMTypeRef type, String label) {
        return trackValue(LLVM.LLVMBuildAlloca(builder, type, nextDiscriminator(label)));
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

    public boolean validateFunctions() {
        boolean valid = true;
        for (LLVMFunctionBuilder function : functions)
            valid = function.verifyFunction() && valid;
        return valid;
    }

    public LLVMValueRef call(LLVMFunctionBuilder func, LLVMValueRef... args) {
        PointerPointer<LLVMValueRef> ptr = trackValue(new PointerPointer<>(args.length));
        for (int i = 0; i < args.length; i++) {
            ptr.put(i, args[i]);
        }

        LLVMValueRef call = LLVM.LLVMBuildCall(
                builder, func.function,
                ptr, args.length, nextDiscriminator("call")
        );
        LLVMSetInstructionCallConv(call, LLVMGetFunctionCallConv(func.function));
        return trackValue(call);
    }

    public void callV(LLVMFunctionBuilder func, LLVMValueRef... args) {
        PointerPointer<LLVMValueRef> ptr = trackValue(new PointerPointer<>(args.length));
        for (int i = 0; i < args.length; i++) {
            ptr.put(i, args[i]);
        }
        LLVMValueRef call = LLVM.LLVMBuildCall(
                builder, func.function,
                ptr, args.length, ""
        );
        LLVMSetInstructionCallConv(call, LLVMGetFunctionCallConv(func.function));
        trackValue(call);
    }

    public LLVMValueRef call(LLVMValueRef func, LLVMValueRef... args) {
        PointerPointer<LLVMValueRef> ptr = trackValue(new PointerPointer<>(args.length));
        for (int i = 0; i < args.length; i++) {
            ptr.put(i, args[i]);
        }

        LLVMValueRef call = LLVM.LLVMBuildCall(
                builder, func,
                ptr, args.length, nextDiscriminator("call")
        );
        LLVMSetInstructionCallConv(call, LLVMGetFunctionCallConv(func));
        return trackValue(call);
    }

    public void callV(LLVMValueRef func, LLVMValueRef... args) {
        PointerPointer<LLVMValueRef> ptr = trackValue(new PointerPointer<>(args.length));
        for (int i = 0; i < args.length; i++) {
            ptr.put(i, args[i]);
        }
        LLVMValueRef call = LLVM.LLVMBuildCall(
                builder, func,
                ptr, args.length, ""
        );
        LLVMSetInstructionCallConv(call, LLVMGetFunctionCallConv(func));
        trackValue(call);
    }

    public LLVMTypeRef pointerType(LLVMTypeRef bytePtr) {
        return LLVM.LLVMPointerType(bytePtr, 0);
    }

    public LLVMPassManagerRef standardOptimizer(int loopEliminationFactor) {
        LLVMPassManagerRef pass = LLVMCreatePassManager();
//        LLVMAddStripSymbolsPass(pass);

        LLVMAddCFGSimplificationPass(pass);
        LLVMAddAggressiveDCEPass(pass); // dead code elimination
        LLVMAddSimplifyLibCallsPass(pass);
        LLVMAddPartiallyInlineLibCallsPass(pass);
        LLVMAddEarlyCSEMemSSAPass(pass);
        LLVMAddEarlyCSEPass(pass);

        LLVMAddReassociatePass(pass);
        LLVMAddPromoteMemoryToRegisterPass(pass);
        LLVMAddLICMPass(pass);

        LLVMAddLoopRotatePass(pass);
        LLVMAddLoopIdiomPass(pass);

        LLVMAddInstructionCombiningPass(pass);

        LLVMAddScalarizerPass(pass);

        for (int i = 0; i < loopEliminationFactor; i++) {
            LLVMAddLoopUnrollPass(pass);
            LLVMAddCFGSimplificationPass(pass);
        }

//        LLVMAddReassociatePass(pass);
//        LLVMAddInstructionCombiningPass(pass);

        LLVMAddLoopUnswitchPass(pass);
        LLVMAddLoopDeletionPass(pass);
//        LLVMAddLoopVectorizePass(pass);
//        LLVMAddSLPVectorizePass(pass);
        LLVMAddJumpThreadingPass(pass);

        LLVMAddMemCpyOptPass(pass);
        LLVMAddConstantMergePass(pass);

        LLVMAddTailCallEliminationPass(pass);
        LLVMAddConstantPropagationPass(pass);

        LLVMAddNewGVNPass(pass);

        LLVMAddDeadStoreEliminationPass(pass);
        LLVMAddMergedLoadStoreMotionPass(pass);
        LLVMAddAggressiveDCEPass(pass); // dead code elimination

        LLVMAddReassociatePass(pass);
        LLVMAddIndVarSimplifyPass(pass);
        LLVMAddInstructionCombiningPass(pass);

        LLVMAddLoopVectorizePass(pass);
        LLVMAddSLPVectorizePass(pass);

//        LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
//        LLVMAddReassociatePass(pass);
//        LLVMAddIndVarSimplifyPass(pass);
//        LLVMAddInstructionCombiningPass(pass);
//        LLVMAddConstantMergePass(pass);
//        LLVMAddConstantPropagationPass(pass);
//        LLVMAddSCCPPass(pass);
//        LLVMAddLoopRotatePass(pass);
//        LLVMAddLoopUnrollAndJamPass(pass);
//        LLVMAddCFGSimplificationPass(pass);
//        LLVMAddReassociatePass(pass);
//        LLVMAddIndVarSimplifyPass(pass);
//        LLVMAddInstructionCombiningPass(pass);
//        LLVMAddCFGSimplificationPass(pass);
//        LLVMAddNewGVNPass(pass);
//        LLVMAddPromoteMemoryToRegisterPass(pass);
//        LLVMAddReassociatePass(pass);
//        LLVMAddIndVarSimplifyPass(pass);
//        LLVMAddInstructionCombiningPass(pass);
//        LLVMAddCFGSimplificationPass(pass);
//        LLVMAddNewGVNPass(pass);

        return pass;
    }
}

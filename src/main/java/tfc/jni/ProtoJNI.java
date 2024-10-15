package tfc.jni;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jni.JNINativeInterface;
import org.lwjgl.system.jni.JNINativeMethod;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMFunctionBuilder;
import tfc.llvmutil.LLVMParamsBuilder;
import tfc.llvmutil.LLVMStructBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.bytedeco.llvm.global.LLVM.*;

public class ProtoJNI {
    private static final BytePointer error = new BytePointer();

    static {
        String prefix = ProtoJNI.class.getName().replace(".", "_");

        LLVMBuilderRoot root = new LLVMBuilderRoot(
                ProtoJNI.class.getName()
        );

        LLVMFunctionBuilder builder;
        PointerPointer<LLVMTypeRef> ptr0 = new PointerPointer(234);
        for (long i = 0; i < ptr0.capacity(); i++) ptr0.put(i, root.VOID_PTR);

        LLVMStructBuilder jobj = root.createStruct("_jobject");
        LLVMTypeRef objPtr = jobj.pointerType(0);
        LLVMTypeRef clzPtr = objPtr;
        LLVMTypeRef strPtr = objPtr;
        LLVMStructBuilder jniEnv = root.createStruct("JNINativeInterface_");
        jniEnv.addElements(ptr0).setBody();
        LLVMTypeRef envPtr = jniEnv.pointerType(0);
        LLVMTypeRef envPtrPtr = root.pointerType(envPtr);
        LLVMTypeRef constCharPtr = root.BYTE_PTR;

        LLVMFunctionBuilder getUTFFunc = root.functionPrototype(
                "GetStringUTFChars",
                new LLVMParamsBuilder(root)
                        .addArg(envPtrPtr)
                        .addArg(strPtr)
                        .addArg(root.BYTE)
                        .build(constCharPtr)
        ).withConvention(LLVMCCallConv);
        LLVMFunctionBuilder freeUTFFunc = root.functionPrototype(
                "ReleaseStringUTFChars",
                new LLVMParamsBuilder(root)
                        .addArg(envPtrPtr)
                        .addArg(strPtr)
                        .addArg(constCharPtr)
                        .build(root.VOID)
        ).withConvention(LLVMCCallConv);
        LLVMFunctionBuilder getMethodIDFunc = root.functionPrototype(
                "GetMethodID",
                new LLVMParamsBuilder(root)
                        .addArg(envPtrPtr)
                        .addArg(clzPtr)
                        .addArg(constCharPtr)
                        .addArg(constCharPtr)
                        .build(root.LONG)
        ).withConvention(LLVMCCallConv);

        {
            // TODO: fix
            builder = root.function(
                    prefix + "_getJNIEnv",
                    new LLVMParamsBuilder(root)
                            .addArg(envPtrPtr).addArg(clzPtr)
                            .build(root.LONG)
            ).withConvention(LLVMCCallConv).export().buildRoot();
            LLVMValueRef val =root.getValue(envPtr, builder.getParam(0, envPtrPtr));
            builder.ret(root.ptrCast(val, root.LONG));
        }
        {
            builder = root.function(
                    prefix + "_getSJMethod",
                    new LLVMParamsBuilder(root)
                            .addArg(envPtrPtr).addArg(clzPtr)
                            .addArg(clzPtr)
                            .addArg(strPtr)
                            .addArg(strPtr)
                            .build(root.LONG)
            ).withConvention(LLVMCCallConv).export().buildRoot();

            LLVMValueRef envRef = builder.getParam(0, envPtrPtr);
            LLVMValueRef clz = builder.getParam(2, clzPtr);
            LLVMValueRef nameJSTR = builder.getParam(3, strPtr);
            LLVMValueRef signJSTR = builder.getParam(4, strPtr);

            LLVMValueRef envLoaded = root.getValue(envPtr, envRef);

            PointerPointer<LLVMValueRef> params0 = root.trackValue(new PointerPointer<>(2));
            params0.put(0, root.loadLong(0));
            params0.put(1, root.loadInt(169)); // 169 == GetStringUTF
            LLVMValueRef getUTF = LLVM.LLVMBuildInBoundsGEP(
                    root.builder, envLoaded,
                    params0, 2,
                    "getUTF"
            );
            PointerPointer<LLVMValueRef> params1 = root.trackValue(new PointerPointer<>(2));
            params1.put(0, root.loadLong(0));
            params1.put(1, root.loadInt(170)); // 170 == ReleaseStringUTF
            LLVMValueRef freeUTF = LLVM.LLVMBuildInBoundsGEP(
                    root.builder, envLoaded,
                    params1, 2,
                    "freeUTF"
            );
            PointerPointer<LLVMValueRef> params2 = root.trackValue(new PointerPointer<>(2));
            params2.put(0, root.loadLong(0));
            params2.put(1, root.loadInt(113)); // 113 == GetStaticMethodID, 33 == GetMethodID
            LLVMValueRef getMethodID = LLVM.LLVMBuildInBoundsGEP(
                    root.builder, envLoaded,
                    params2, 2,
                    "getMethodID"
            );

            LLVMValueRef jniFalse = LLVM.LLVMConstNull(root.BYTE);

            LLVMValueRef getUTFLoaded = root.getValue(root.pointerType(getUTFFunc.type), getUTF);
            LLVMValueRef freeUTFLoaded = root.getValue(root.pointerType(freeUTFFunc.type), freeUTF);
            LLVMValueRef getMethodIDLoaded = root.getValue(root.pointerType(getMethodIDFunc.type), getMethodID);

            LLVMValueRef nameChr = root.call(getUTFLoaded, envRef, nameJSTR, jniFalse);
            LLVMValueRef signChr = root.call(getUTFLoaded, envRef, signJSTR, jniFalse);

            LLVMValueRef res = root.call(
                    getMethodIDLoaded,
                    envRef, clz,
                    nameChr, signChr
            );

            root.callV(freeUTFLoaded, envRef, nameJSTR, nameChr);
            root.callV(freeUTFLoaded, envRef, signJSTR, signChr);

            builder.ret(res);
        }

        LLVMModuleRef moduleRef = root.getModule();
        root.disposeBuilder();
        root.dump();

        if (!root.validateFunctions()) {
            throw new RuntimeException("Failed to prototype JNI.");
        }


        LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
        LLVMMCJITCompilerOptions options = new LLVMMCJITCompilerOptions();
        if (LLVMCreateMCJITCompilerForModule(engine, moduleRef, options, 3, error) != 0) {
            System.err.println("Failed to create JIT compiler: " + error.getString());
            LLVMDisposeMessage(error);
            throw new RuntimeException("Failed to prototype JNI.");
        }

        JNINativeMethod.Buffer buffer = JNINativeMethod.calloc(2);

        ArrayList<ByteBuffer> buffers = new ArrayList<>();

        long addr;
        JNINativeMethod method;
        ByteBuffer name, sig;

        addr = LLVM.LLVMGetFunctionAddress(engine, prefix + "_getJNIEnv");
        method = buffer.get(0);
        method.fnPtr(addr);
        sig = MemoryUtil.memUTF8("()J");
        method.signature(sig);
        buffers.add(sig);
        name = MemoryUtil.memUTF8("getJNIEnv");
        method.name(name);
        buffers.add(name);

        addr = LLVM.LLVMGetFunctionAddress(engine, prefix + "_getSJMethod");
        method = buffer.get(1);
        method.fnPtr(addr);
        sig = MemoryUtil.memUTF8("(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J");
        method.signature(sig);
        buffers.add(sig);
        name = MemoryUtil.memUTF8("getStaticMethodID");
        method.name(name);
        buffers.add(name);

        JNINativeInterface.RegisterNatives(
                ProtoJNI.class,
                buffer
        );

        for (ByteBuffer byteBuffer : buffers) MemoryUtil.memFree(byteBuffer);
    }

    public static void init() {
    }

    public static native long getJNIEnv();

    public static native long getStaticMethodID(Class<?> clz, String name, String signature);
}

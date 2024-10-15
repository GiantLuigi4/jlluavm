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
        LLVMFunctionBuilder getMethodID = root.functionPrototype(
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
            ).withConvention(LLVMCCallConv).buildRoot();
            LLVMValueRef val =root.getValue(envPtr, builder.getParam(0, envPtrPtr));
            builder.ret(root.ptrCast(val, root.LONG));
        }
        {
            builder = root.function(
                    prefix + "_getJMethod",
                    new LLVMParamsBuilder(root)
                            .addArg(envPtrPtr).addArg(clzPtr)
                            .addArg(clzPtr)
                            .addArg(strPtr)
                            .addArg(strPtr)
                            .build(root.LONG)
            ).withConvention(LLVMCCallConv).buildRoot();

            LLVMValueRef envRef = builder.getParam(0, envPtrPtr);
            LLVMValueRef nameJSTR = builder.getParam(3, strPtr);
            LLVMValueRef signJSTR = builder.getParam(4, strPtr);

            LLVMValueRef envLoaded = root.getValue(envPtr, envRef);

            PointerPointer<LLVMValueRef> params = new PointerPointer<>(2);
            params.put(0, root.loadLong(0));
            params.put(1, root.loadInt(169)); // 169 == GetStaticMethodID
            LLVMValueRef getUTF = LLVM.LLVMBuildInBoundsGEP(
                    root.builder, envLoaded,
                    params, 2,
                    "getUTF"
            );

            LLVMValueRef jniFalse = LLVM.LLVMConstNull(root.BYTE);

            LLVMValueRef getUTFLoaded = root.getValue(root.pointerType(getUTFFunc.type), getUTF);

            LLVMValueRef nameChr = root.call(getUTFLoaded, envRef, nameJSTR, jniFalse);
            LLVMValueRef signChr = root.call(getUTFLoaded, envRef, signJSTR, jniFalse);

//            LLVMValueRef res = root.call(
//                    getMethodID,
//                    env, clz,
//                    nameChr, signChr
//            );
//
//            root.callV(freeUTF, env, nameJSTR, nameChr);
//            root.callV(freeUTF, env, signJSTR, signChr);
//
//            builder.ret(res);

//            builder.ret(root.ptrCast(nameJSTR, root.LONG));
            builder.ret(root.ptrCast(getUTFLoaded, root.LONG));
//            builder.ret(root.loadLong(0));
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

        addr = LLVM.LLVMGetFunctionAddress(engine, prefix + "_getJMethod");
        method = buffer.get(1);
        method.fnPtr(addr);
        sig = MemoryUtil.memUTF8("(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J");
        method.signature(sig);
        buffers.add(sig);
        name = MemoryUtil.memUTF8("getMethodID");
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

    public static native long getMethodID(Class<?> clz, String name, String signature);
}

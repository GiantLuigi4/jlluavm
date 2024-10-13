package tfc.jni;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef;
import org.bytedeco.llvm.LLVM.LLVMMCJITCompilerOptions;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
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

        LLVMStructBuilder jobj = root.createStruct("_jobject");
        LLVMStructBuilder jclz = root.createStruct("_jclass");
        LLVMStructBuilder jenv = root.createStruct("JNINativeInterface_");
        LLVMTypeRef envPtr = jenv.pointerType(8);
        jclz.addElement(jobj).setBody();

        LLVMFunctionBuilder builder;

        {
            builder = root.function(
                    prefix + "_getJNIEnv",
                    new LLVMParamsBuilder(root)
                            .addArg(root.LONG)
                            .addArg(jclz)
                            .build(root.LONG)
            ).buildRoot();
            builder.ret(builder.getParam(0, root.LONG));
        }
        {
            builder = root.function(
                    prefix + "_getClassID",
                    new LLVMParamsBuilder(root)
                            .addArg(envPtr)
                            .addArg(jclz)
                            .addArg(root.LONG)
                            .build(root.LONG)
            ).buildRoot();
            builder.ret(builder.getParam(2, root.LONG));
        }

        LLVMModuleRef moduleRef = root.getModule();
        root.disposeBuilder();
        root.dump();


        LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
        LLVMMCJITCompilerOptions options = new LLVMMCJITCompilerOptions();
        if (LLVMCreateMCJITCompilerForModule(engine, moduleRef, options, 3, error) != 0) {
            System.err.println("Failed to create JIT compiler: " + error.getString());
            LLVMDisposeMessage(error);
            throw new RuntimeException("Failed to prototype jni");
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

        addr = LLVM.LLVMGetFunctionAddress(engine, prefix + "_getClassID");
        method = buffer.get(1);
        method.fnPtr(addr);
        sig = MemoryUtil.memUTF8("(Ljava/lang/Class;)J");
        method.signature(sig);
        buffers.add(sig);
        name = MemoryUtil.memUTF8("getClassID");
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

    public static native long getClassID(Class<?> clz);
}

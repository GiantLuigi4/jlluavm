package tfc.jni;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMParamsBuilder;
import tfc.llvmutil.LLVMStructBuilder;

public class JNIHelper {
    public final PointerPointer<LLVMTypeRef> ptr0;
    public final LLVMStructBuilder jobj;
    public final LLVMTypeRef objPtr;
    public final LLVMTypeRef clzPtr;
    public final LLVMTypeRef strPtr;
    public final LLVMStructBuilder jniEnv;
    public final LLVMTypeRef envPtr;
    public final LLVMTypeRef envPtrPtr;
    public final LLVMTypeRef constCharPtr;

    public final LLVMTypeRef callVoidType;

    LLVMBuilderRoot root;

    public JNIHelper(LLVMBuilderRoot root) {
        this.root = root;
        ptr0 = new PointerPointer(234);
        jobj = root.createStruct("_jobject");
        objPtr = jobj.pointerType(0);
        clzPtr = objPtr;
        strPtr = objPtr;
        jniEnv = root.createStruct("JNINativeInterface_");
        jniEnv.addElements(ptr0).setBody();
        envPtr = jniEnv.pointerType(0);
        envPtrPtr = root.pointerType(envPtr);
        constCharPtr = root.BYTE_PTR;
        callVoidType = new LLVMParamsBuilder(root)
                .addArg(envPtrPtr)
                .addArg(clzPtr)
                .addArg(root.LONG)
                .addArg(strPtr)
                .build(root.LONG, true);
    }
}

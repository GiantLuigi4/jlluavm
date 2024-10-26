package tfc.jni;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMParamsBuilder;
import tfc.llvmutil.LLVMStructBuilder;

import java.util.List;

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
    public final LLVMTypeRef callFindClassType;

    LLVMBuilderRoot root;

    public JNIHelper(LLVMBuilderRoot root) {
        this.root = root;
        ptr0 = root.trackValue(new PointerPointer<>(234));
        for (long i = 0; i < ptr0.capacity(); i++) ptr0.put(i, root.VOID_PTR);
        jobj = root.createStruct("_jobject");
        objPtr = jobj.pointerType(0);
        clzPtr = objPtr;
        strPtr = objPtr;
        jniEnv = root.createStruct("JNINativeInterface_");
        jniEnv.addElements(ptr0).setBody();
        envPtr = jniEnv.pointerType(0);
        envPtrPtr = root.pointerType(envPtr);
        constCharPtr = root.BYTE_PTR;
        callFindClassType = new LLVMParamsBuilder(root)
                .addArg(envPtrPtr)
                .addArg(root.BYTE_PTR)
                .build(clzPtr, false);
        callVoidType = new LLVMParamsBuilder(root)
                .addArg(envPtrPtr)
                .addArg(clzPtr)
                .addArg(root.LONG)
                .build(root.LONG, true);
    }

    public LLVMValueRef getClass(LLVMValueRef jniEnv, LLVMValueRef className) {
        LLVMValueRef envLoaded = root.getValue(envPtr, root.bitCast(
                jniEnv, envPtrPtr
        ));

        PointerPointer<LLVMValueRef> params2 = root.trackValue(new PointerPointer<>(2));
        params2.put(0, root.loadLong(0));
        params2.put(1, root.loadInt(6)); // 6 == FindClass
        LLVMValueRef callFind = root.trackValue(LLVM.LLVMBuildInBoundsGEP(
                root.builder, envLoaded,
                params2, 2,
                "callFind"
        ));

        LLVMValueRef callFindLoaded = root.getValue(root.pointerType(callFindClassType), callFind);

        return root.call(
                callFindLoaded,
                jniEnv, root.bitCast(className, root.LONG)
        );
    }

    public void makeVoidCall(
            LLVMValueRef jniEnv,
            LLVMValueRef className,
            LLVMValueRef methodHandle,
            List<LLVMValueRef> paramsLLVM
    ) {
        LLVMValueRef envLoaded = root.getValue(envPtr, jniEnv);

        PointerPointer<LLVMValueRef> params2 = root.trackValue(new PointerPointer<>(2));
        params2.put(0, root.loadLong(0));
        params2.put(1, root.loadInt(141)); // 141 == CallStaticVoid
        LLVMValueRef callVoid = root.trackValue(LLVM.LLVMBuildInBoundsGEP(
                root.builder, envLoaded,
                params2, 2,
                "callVoid"
        ));

        LLVMValueRef callVoidLoaded = root.getValue(root.pointerType(callVoidType), callVoid);

        LLVMValueRef[] aarray = new LLVMValueRef[3 + paramsLLVM.size()];
        for (int i = 0; i < paramsLLVM.size(); i++)
            aarray[i + 3] = paramsLLVM.get(i);
        aarray[0] = jniEnv;
        aarray[1] = className;
        aarray[2] = methodHandle;
        root.call(
                callVoidLoaded,
                aarray
        );
    }
}

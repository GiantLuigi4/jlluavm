package tfc.llvmutil;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

import java.util.ArrayList;
import java.util.List;

public class LLVMStructBuilder {
    LLVMTypeRef struct;
    List<LLVMTypeRef> bodyElems = new ArrayList<>();
    PointerPointer<LLVMTypeRef> elemsPtr;

    public LLVMStructBuilder(LLVMTypeRef struct) {
        this.struct = struct;
    }

    public LLVMStructBuilder addElement(LLVMTypeRef type) {
        bodyElems.add(type);
        return this;
    }

    public LLVMStructBuilder addElement(LLVMStructBuilder type) {
        bodyElems.add(type.struct);
        return this;
    }

    public LLVMStructBuilder setBody() {
        elemsPtr = new PointerPointer<>(bodyElems.size());
        for (int i = 0; i < bodyElems.size(); i++) elemsPtr.put(i, bodyElems.get(i));
        LLVM.LLVMStructSetBody(
                struct,
                elemsPtr,
                bodyElems.size(),
                0
        );
        return this;
    }

    public LLVMTypeRef pointerType(int addrSpace) {
        return LLVM.LLVMPointerType(struct, addrSpace);
    }

    public LLVMTypeRef type() {
        return struct;
    }
}

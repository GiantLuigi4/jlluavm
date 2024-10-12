package tfc.jlluavm.parse.scopes;

import tfc.jlluavm.parse.llvm.LLVMBuilderRoot;

public class GlobalScope extends Scope {
    public GlobalScope(LLVMBuilderRoot root) {
        super(null, null, root, null);
    }
}

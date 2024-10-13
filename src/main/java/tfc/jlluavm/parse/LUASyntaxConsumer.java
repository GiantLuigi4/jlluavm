package tfc.jlluavm.parse;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;
import org.lwjgl.system.Library;
import tfc.llvmutil.LLVMBuilderRoot;
import tfc.llvmutil.LLVMFunctionBuilder;
import tfc.jlluavm.parse.scopes.GlobalScope;
import tfc.jlluavm.parse.scopes.Scope;
import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.util.Resolver;

import java.util.ArrayDeque;
import java.util.ArrayList;

import static org.bytedeco.llvm.global.LLVM.*;

import org.lwjgl.system.JNI;

public class LUASyntaxConsumer {
    static int loopEliminationFactor = 2;

    LLVMBuilderRoot root;
    ArrayList<LLVMFunctionBuilder> finishedFunctions = new ArrayList<>();
    ArrayDeque<LLVMFunctionBuilder> functionStack = new ArrayDeque<>();
    ArrayDeque<LLVMBasicBlockRef> breakTo = new ArrayDeque<>();
    GlobalScope global;
    ArrayDeque<Scope> scopes = new ArrayDeque<>();

    public LUASyntaxConsumer() {
        root = new LLVMBuilderRoot(
                "module_lua_jit_" + toString().replace("@", "")
        );

        global = new GlobalScope(root);
        LLVMTypeRef params = root.trackValue(LLVMFunctionType(
                root.LONG,
                root.LONG,
                1, 0
        ));
        functionStack.push(root.function(
                "$$module_lua_jit_$_root_entry_$_" + toString().replace("@", ""),
                params
        ).buildRoot());
        System.out.println("START SCOPE");
        scopes.push(global);
    }

    private void pushScope() {
        System.out.println("START SCOPE");
        scopes.push(new Scope(global, scopes.peek(), root, functionStack.peek()));
    }

    private void acceptBody(BufferedStream<LUAToken> tokenStream) {
        acceptBody(true, tokenStream);
    }

    private void popScope() {
        scopes.pop();
        System.out.println("END SCOPE");
    }

    boolean isEnder(LUAToken token) {
        return token.text.equals("end") || token.text.equals("elseif") || token.text.equals("else");
    }

    private void acceptBody(boolean withScope, BufferedStream<LUAToken> tokenStream) {
        if (withScope) pushScope();

        if (!tokenStream.current().text.equals("end")) {
            while (true) {
                acceptCurrent(tokenStream);
                LUAToken token = tokenStream.next();
                if (isEnder(token))
                    break;
            }
        }

        if (withScope) popScope();
    }

    private void acceptFunction(BufferedStream<LUAToken> tokenStream) {
        boolean local = tokenStream.current().text.equals("local");
        if (local) tokenStream.advance(); // local
        String name = tokenStream.next().text;

        LUAToken current;
        do {
            current = tokenStream.next();
            System.out.print(current.text);
            System.out.print(" ");
        } while (!current.text.equals(")"));
        System.out.println();
        System.out.print(local + " ");
        System.out.print(name);
        System.out.println();
        tokenStream.advance(); // )

        LLVMTypeRef params = root.trackValue(LLVMFunctionType(
                root.LONG,
                (PointerPointer) null,
                0, 0
        ));

        functionStack.add(root.function(
                name, params
        ).buildRoot());
        acceptBody(tokenStream);
        finishedFunctions.add(functionStack.pop());

        functionStack.peek().resumeBuilding();
    }

    private void acceptReturn(BufferedStream<LUAToken> tokenStream) {
        tokenStream.next();

        if (tokenStream.current() == null || tokenStream.current().text.equals("end")) {
            functionStack.peek().ret(root.loadLong(0));
            return;
        }

        LLVMValueRef ref = acceptValue(tokenStream);
        ref = root.cast(ref, root.LONG);
        functionStack.peek().ret(ref);
    }

    private String acceptVariable(boolean local, BufferedStream<LUAToken> tokenStream) {
        String name = tokenStream.current().text;
        tokenStream.advance(2); // name =

        System.out.print(local);
        System.out.print(" ");
        System.out.print(name);
        System.out.print(" = ");
        LLVMValueRef value = acceptValue(tokenStream);
        // TODO: global variables (per file by default, might need to be configurable though)
        scopes.peek().addVariable(local, name, value);
        return name;
    }

    private void acceptVariable(BufferedStream<LUAToken> tokenStream) {
        boolean local = tokenStream.current().text.equals("local");
        if (local) tokenStream.advance();
        acceptVariable(local, tokenStream);
    }

    private LLVMValueRef acceptSingleValue(BufferedStream<LUAToken> tokenStream) {
        if (tokenStream.current().type.equals("numeric")) {
            return root.loadDouble(Double.parseDouble(tokenStream.current().text));
        } else if (tokenStream.current().type.equals("literal")) {
            // TODO: scoped access to variables
            return scopes.peek().getVariable(root.DOUBLE, tokenStream.current().text);
        } else if (tokenStream.current().text.equals("(")) {
            tokenStream.advance();
            LLVMValueRef ref = acceptValue(tokenStream);
            tokenStream.advance();
            return ref;
            // TODO: remove
        } else if (tokenStream.current().type.equals("tmp_arg")) {
            return functionStack.peek().getParamAsDouble(Integer.parseInt(tokenStream.current().text.substring(1)));
        }
        return null;
    }

    private LLVMValueRef acceptValue(BufferedStream<LUAToken> tokenStream) {
        return acceptValue(new ExpressionBuilder(), tokenStream);
    }

    private LLVMValueRef acceptValue(ExpressionBuilder builder, BufferedStream<LUAToken> tokenStream) {
        System.out.println(tokenStream.current().text);
        // TODO: calls, array access
        // TODO: functions are values!
        LLVMValueRef ref = acceptSingleValue(tokenStream);
        if (ref != null) {
            builder.addValue(ref);
            while (Resolver.nextThing(tokenStream, 1) == Resolver.ThingType.OPERATION) {
                tokenStream.advance();
                builder.addOperation(tokenStream.current().text);
                tokenStream.advance();
                builder.addValue(acceptSingleValue(tokenStream));
            }
            return builder.build(root);
        } else if (tokenStream.current().text.equals("true")) {
            return root.CONST_TRUE;
        } else if (tokenStream.current().text.equals("false")) {
            return root.CONST_FALSE;
        }
        throw new RuntimeException("huh?");
    }

    private final BytePointer error = new BytePointer();

    private void acceptFor(BufferedStream<LUAToken> tokenStream) {
        pushScope();

        tokenStream.advance();
        String varName = acceptVariable(true, tokenStream);

        tokenStream.advance(2); // val ,

        LLVMValueRef terminator = acceptValue(tokenStream);
        tokenStream.advance();

        LLVMValueRef step;
        if (tokenStream.current().text.equals(",")) {
            tokenStream.advance();
            step = acceptValue(tokenStream);
            tokenStream.advance(); // value
        } else {
            step = root.CONST_1D;
        }
        System.out.println(tokenStream.current().text);

        if (!tokenStream.current().text.equals("do")) {
            throw new RuntimeException("Expected a \"do\" after loop header");
        }

        LLVMFunctionBuilder builder = functionStack.peek();

        // loop iteration tracker
        tokenStream.advance();

        LLVMBasicBlockRef startPos = builder.createBlock("start_loop_p");
        LLVMBasicBlockRef startNeg = builder.createBlock("start_loop_n");
        LLVMBasicBlockRef block = builder.createBlock("after_loop");
        LLVMBasicBlockRef body = builder.createBlock("loop_body");

        LLVMValueRef condPN = root.compareG(step, root.CONST_0D);
        root.conditionalJump(condPN, startPos, startNeg);

        breakTo.push(block);

        {
            {
                // negative header
                builder.buildBlock(startNeg);
                LLVMValueRef counter = scopes.peek().getVariable(root.DOUBLE, varName);
                LLVMValueRef cond = root.compareGE(counter, terminator);
                root.conditionalJump(cond, body, block);
            }
            {
                // positive header
                builder.buildBlock(startPos);
                LLVMValueRef counter = scopes.peek().getVariable(root.DOUBLE, varName);
                LLVMValueRef cond = root.compareLE(counter, terminator);
                root.conditionalJump(cond, body, block);
            }
            {
                // counter
                builder.buildBlock(body);
                LLVMValueRef counter = scopes.peek().getVariable(root.DOUBLE, varName);
                LLVMValueRef counterNV = root.sum(counter, step);
                scopes.peek().addVariable(true, varName, counterNV);

                // body
                pushScope();
                scopes.peek().addVariable(true, varName, counter);
                acceptBody(false, tokenStream);
                root.conditionalJump(condPN, startPos, startNeg);
                popScope();
            }
        }

        breakTo.pop();

        popScope();

        builder.buildBlock(block);
    }

    private void acceptIf(BufferedStream<LUAToken> tokenStream) {
        acceptIf(tokenStream, null);
    }

    private void acceptIf(BufferedStream<LUAToken> tokenStream, LLVMBasicBlockRef conclusion) {
        tokenStream.advance(); // if

        LLVMValueRef condition = acceptValue(tokenStream);
        LLVMFunctionBuilder builder = functionStack.peek();
        LLVMBasicBlockRef from = builder.activeBlock();
        LLVMBasicBlockRef body = builder.createBlock("body");
        LLVMBasicBlockRef to = builder.createBlock("to");
        boolean isMaster = conclusion == null;
        if (conclusion == null) conclusion = builder.createBlock("conclusion");

        {
            tokenStream.advance();
            if (!tokenStream.current().text.equals("then"))
                throw new RuntimeException("Expected \"then\" but got " + tokenStream.current().text);
            tokenStream.advance();
            builder.buildBlock(body);
            acceptBody(true, tokenStream);
            root.jump(conclusion);
        }

        builder.buildBlock(from);
        root.conditionalJump(condition, body, to);

        builder.buildBlock(to);

        if (tokenStream.current().text.equals("elseif")) {
            acceptIf(tokenStream, conclusion);
        } else if (tokenStream.current().text.equals("else")) {
            tokenStream.advance();
            acceptBody(true, tokenStream);
        }

        if (isMaster) {
            root.jump(conclusion);
            builder.buildBlock(conclusion);
        }
    }

    static {
        Library.initialize();
    }

    private void acceptDo(BufferedStream<LUAToken> tokenStream) {
        tokenStream.advance();
        acceptBody(true, tokenStream);
    }

    private void acceptBreak(BufferedStream<LUAToken> tokenStream) {
        root.jump(breakTo.peek());
        throw new RuntimeException("NYI");
    }

    public void finish() {
        popScope();

        root.dump();

        LLVMFunctionBuilder functionEntry = functionStack.peek();

        finishedFunctions.add(functionStack.pop());
        boolean verified = true;
        for (LLVMFunctionBuilder finishedFunction : finishedFunctions) {
            verified = finishedFunction.verifyFunction() && verified;
        }

        if (!verified)
            return;

        root.verify();

        LLVMInitializeAggressiveInstCombiner(LLVM.LLVMGetGlobalPassRegistry());
        LLVMPassManagerRef pass = LLVMCreatePassManager();
        LLVMAddStripSymbolsPass(pass);

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

        long nt = System.nanoTime();
        LLVMRunPassManager(pass, root.getModule());
        long nt1 = System.nanoTime();
        System.out.println("Optimization took: " + (nt1 - nt) + " ns");


        // cleanup
        LLVMDisposePassManager(pass);
        root.disposeBuilder();

        root.dump();

        // TODO: REMOVE! TESTING ONLY!
        LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
        LLVMMCJITCompilerOptions options = new LLVMMCJITCompilerOptions();
        if (LLVMCreateMCJITCompilerForModule(engine, root.getModule(), options, 3, error) != 0) {
            System.err.println("Failed to create JIT compiler: " + error.getString());
            LLVMDisposeMessage(error);
            return;
        }

        long addr = LLVM.LLVMGetFunctionAddress(engine, functionEntry.getName());
        long argV = Double.doubleToLongBits(21);

        nt = System.nanoTime();
        long result = JNI.invokePP(argV, addr);
        nt1 = System.nanoTime();
        System.out.println("Execution took: " + (nt1 - nt) + " ns");

        nt = System.nanoTime();
        result = JNI.invokePP(argV, addr);
        nt1 = System.nanoTime();
        System.out.println("Execution took: " + (nt1 - nt) + " ns");

        System.out.println();
        System.out.println("Running entry() with MCJIT...");
        System.out.println("Result: " + Double.longBitsToDouble(result));
    }

    public void acceptCurrent(BufferedStream<LUAToken> tokenStream) {
        if (tokenStream.current() == null) return;

        Resolver.ThingType currentType = Resolver.nextThing(tokenStream);
        System.out.println(" EMIT: " + currentType.toString());
        switch (currentType) {
            case FOR_LOOP -> acceptFor(tokenStream);
            case IF -> acceptIf(tokenStream);
            case FUNCTION -> acceptFunction(tokenStream);
            case VARIABLE -> acceptVariable(tokenStream);
            case RETURN -> acceptReturn(tokenStream);
            case DO -> acceptDo(tokenStream);
            case BREAK -> acceptBreak(tokenStream);
            default -> System.out.println("TOKEN: " + tokenStream.current().text);
        }
    }
}

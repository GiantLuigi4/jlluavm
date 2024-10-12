package tfc.jlluavm.parse;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;
import tfc.jlluavm.parse.llvm.LLVMBuilderRoot;
import tfc.jlluavm.parse.llvm.LLVMFunctionBuilder;
import tfc.jlluavm.parse.util.BufferedStream;
import tfc.jlluavm.parse.util.Resolver;

import java.util.ArrayDeque;
import java.util.ArrayList;

import static org.bytedeco.llvm.global.LLVM.*;

public class LUASyntaxConsumer {
    static {
        LLVMInitializeNativeTarget();
        LLVMInitializeNativeAsmPrinter();
    }

    LLVMBuilderRoot root;
    ArrayList<LLVMFunctionBuilder> finishedFunctions = new ArrayList<>();
    ArrayDeque<LLVMFunctionBuilder> functionStack = new ArrayDeque<>();

    public LUASyntaxConsumer() {
        root = new LLVMBuilderRoot(
                "module_lua_jit_" + toString().replace("@", "")
        );

        LLVMTypeRef params = root.trackValue(LLVMFunctionType(
                root.LONG,
                (PointerPointer) null,
                0, 0
        ));
        functionStack.push(root.function(
                "$$module_lua_jit_$_root_entry_$_" + toString().replace("@", ""),
                params
        ).buildRoot());
    }

    int scope = 0;

    private void pushScope() {
        scope++;

        System.out.println("START SCOPE");
    }

    private void acceptBody(BufferedStream<LUAToken> tokenStream) {
        acceptBody(true, tokenStream);
    }

    private void popScope() {
        System.out.println("END SCOPE");

        scope--;
    }

    private void acceptBody(boolean withScope, BufferedStream<LUAToken> tokenStream) {
        if (withScope) pushScope();

        if (!tokenStream.current().text.equals("end")) {
            while (true) {
                acceptCurrent(tokenStream);
                LUAToken token = tokenStream.next();
                if (token.text.equals("end") || token.text.equals("elseif") || token.text.equals("else"))
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
        if (local) {
            functionStack.peek().addVariable(name, value);
        } else throw new RuntimeException("NYI: Globals");
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
            return functionStack.peek().getVariable(root.DOUBLE, tokenStream.current().text);
        } else if (tokenStream.current().text.equals("(")) {
            tokenStream.advance();
            LLVMValueRef ref = acceptValue(tokenStream);
            tokenStream.advance();
            return ref;
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
        }
        throw new RuntimeException("NYI");
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
        LLVMBasicBlockRef bodyPos = builder.createBlock("loop_body_p");
        LLVMBasicBlockRef bodyNeg = builder.createBlock("loop_body_n");

        root.jump(startPos);

        {
            // positive
            {
                // header
                builder.buildBlock(startPos);
                LLVMValueRef counter = builder.getVariable(root.DOUBLE, varName);
                LLVMValueRef cond = root.compareLE(counter, terminator);
                root.conditionalJump(cond, bodyPos, block);

                // body
                builder.buildBlock(bodyPos);
                counter = root.sum(counter, step);
                builder.addVariable(varName, counter);
                acceptBody(false, tokenStream);
                root.jump(startPos);
            }
            // negative
            {
                // header
                builder.buildBlock(startNeg);
                LLVMValueRef counter = builder.getVariable(root.DOUBLE, varName);
                LLVMValueRef cond = root.compareGE(counter, terminator);
                root.conditionalJump(cond, bodyNeg, block);

                // body
                builder.buildBlock(bodyNeg);
                counter = root.sum(counter, step);
                builder.addVariable(varName, counter);
                acceptBody(false, tokenStream);
                root.jump(startNeg);
            }
        }

        builder.buildBlock(block);

        popScope();
    }

    private void acceptIf(BufferedStream<LUAToken> tokenStream) {
        tokenStream.advance(); // if

        LLVMValueRef condition = acceptValue(tokenStream);
        LLVMFunctionBuilder builder = functionStack.peek();
        LLVMBasicBlockRef from = builder.activeBlock();
        LLVMBasicBlockRef body = builder.createBlock("body");
        LLVMBasicBlockRef to = builder.createBlock("to");

        {
            tokenStream.advance();
            if (!tokenStream.current().text.equals("then"))
                throw new RuntimeException("Expected \"then\" but got " + tokenStream.current().text);
            tokenStream.advance();
            builder.buildBlock(body);
            acceptBody(true, tokenStream);
            root.jump(to);
        }

        builder.buildBlock(from);
        root.conditionalJump(condition, body, to);

        builder.buildBlock(to);

        if (tokenStream.current().equals("elseif")) {
            acceptIf(tokenStream);
        }
    }

    public void finish() {
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

        long nt = System.nanoTime();
        LLVMPassManagerRef pass = LLVMCreatePassManager();

//        LLVMAddStripSymbolsPass(pass);
        LLVMAddCFGSimplificationPass(pass);
        LLVMAddSimplifyLibCallsPass(pass);
        LLVMAddPartiallyInlineLibCallsPass(pass);
        LLVMAddEarlyCSEMemSSAPass(pass);

        LLVMAddReassociatePass(pass);
        LLVMAddPromoteMemoryToRegisterPass(pass);
        LLVMAddInstructionCombiningPass(pass);

        LLVMAddLoopRotatePass(pass);
        LLVMAddLoopIdiomPass(pass);
        LLVMAddIndVarSimplifyPass(pass);
        LLVMAddLoopUnswitchPass(pass);
        LLVMAddLoopDeletionPass(pass);
        LLVMAddLoopUnrollPass(pass);
        LLVMAddLoopVectorizePass(pass);
        LLVMAddScalarizerPass(pass);
        LLVMAddMemCpyOptPass(pass);
        LLVMAddConstantMergePass(pass);

        LLVMAddTailCallEliminationPass(pass);
        LLVMAddConstantPropagationPass(pass);

//        LLVMAddGVNPass(pass);
        LLVMAddNewGVNPass(pass);

//        LLVMAddIPSCCPPass(pass);
//        LLVMAddLICMPass(pass);
//        LLVMAddSCCPPass(pass);
//        LLVMAddEarlyCSEPass(pass);
        LLVMAddDeadStoreEliminationPass(pass);
        LLVMAddMergedLoadStoreMotionPass(pass);
//        LLVMAddPruneEHPass(pass);
//        LLVMAddAggressiveDCEPass(pass);

        LLVMAddBitTrackingDCEPass(pass);

        LLVMAddCFGSimplificationPass(pass);

        LLVMRunPassManager(pass, root.getModule());
        long nt1 = System.nanoTime();
        System.out.println("Optimization took: " + (nt1 - nt) + " ns");

//        LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory

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

        PointerPointer<LLVMGenericValueRef> pointer = new PointerPointer<>();
        LLVMGenericValueRef result = LLVMRunFunction(engine, functionEntry.function, /* argumentCount */ 0, pointer);
        System.out.println();
        System.out.println("Running entry() with MCJIT...");
        System.out.println("Result: " + Double.longBitsToDouble(LLVMGenericValueToInt(result, /* signExtend */ 0)));
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
            default -> System.out.println("TOKEN: " + tokenStream.current().text);
        }
    }
}

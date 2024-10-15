package tfc.jlluavm.parse;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryUtil;
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
import tfc.llvmutil.LLVMParamsBuilder;

public class LUASyntaxConsumer {
    static int loopEliminationFactor = 2;

    LLVMBuilderRoot root;
    ArrayList<LLVMFunctionBuilder> finishedFunctions = new ArrayList<>();
    ArrayDeque<LLVMFunctionBuilder> functionStack = new ArrayDeque<>();
    ArrayDeque<LLVMBasicBlockRef> breakTo = new ArrayDeque<>();
    GlobalScope global;
    ArrayDeque<Scope> scopes = new ArrayDeque<>();

    LUAValue CONST_NIL;
    LUAValue CONST_0;
    LUAValue CONST_1;
    LUAValue CONST_FALSE;
    LUAValue CONST_TRUE;

    public LUASyntaxConsumer() {
        root = new LLVMBuilderRoot(
                "module_lua_jit_" + toString().replace("@", "")
        );

        global = new GlobalScope(root);
        LLVMTypeRef params = root.trackValue(
                new LLVMParamsBuilder(root)
                        .addArg(root.BYTE_PTR)
                        .addArg(root.LONG_PTR)
                        .addArg(root.LONG)
                        .build(root.VOID));
        functionStack.push(root.function(
                "$$module_lua_jit_$_root_entry_$_" + toString().replace("@", ""),
                params
        ).buildRoot());
        System.out.println("START SCOPE");
        scopes.push(global);

        CONST_0 = new LUAValue(root, 0, root.CONST_0L);
        CONST_1 = new LUAValue(root, 0, root.CONST_1L);
        CONST_NIL = new LUAValue(root, 4, root.CONST_0L);
        CONST_FALSE = new LUAValue(root, 2, root.CONST_0L);
        CONST_TRUE = new LUAValue(root, 2, root.CONST_1L);
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

        LUAValue ref = acceptValue(tokenStream);
        LLVMValueRef v0 = functionStack.peek().getParam(0, root.BYTE_PTR);
        LLVMValueRef v1 = functionStack.peek().getParam(1, root.LONG_PTR);
        root.setValue(v0, ref.type);
        root.setValue(v1, ref.data);
        functionStack.peek().ret();
//        ref = root.cast(ref, root.LONG);
//        functionStack.peek().ret(ref);
    }

    private String acceptVariable(boolean local, BufferedStream<LUAToken> tokenStream) {
        String name = tokenStream.current().text;
        tokenStream.advance(2); // name =

        System.out.print(local);
        System.out.print(" ");
        System.out.print(name);
        System.out.print(" = ");
        LUAValue value = acceptValue(tokenStream);
        // TODO: global variables (per file by default, might need to be configurable though)
        scopes.peek().addVariable(local, name, value);
        return name;
    }

    private void acceptVariable(BufferedStream<LUAToken> tokenStream) {
        boolean local = tokenStream.current().text.equals("local");
        if (local) tokenStream.advance();
        acceptVariable(local, tokenStream);
    }

    private LUAValue acceptSingleValue(BufferedStream<LUAToken> tokenStream) {
        if (tokenStream.current().type.equals("numeric")) {
            return new LUAValue(
                    root, 1,
                    root.loadDouble(Double.parseDouble(tokenStream.current().text))
            );
        } else if (tokenStream.current().type.equals("literal")) {
            // TODO: scoped access to variables
            return scopes.peek().getVariable(root.DOUBLE, tokenStream.current().text);
        } else if (tokenStream.current().text.equals("(")) {
            tokenStream.advance();
            LUAValue ref = acceptValue(tokenStream);
            tokenStream.advance();
            return ref;
            // TODO: remove
        } else if (tokenStream.current().type.equals("tmp_arg")) {
            return new LUAValue(
                    root, 1,
                    functionStack.peek().getParamAsDouble(
                            Integer.parseInt(tokenStream.current().text.substring(1)) + 2
                    )
            );
        }
        return null;
    }

    private LUAValue acceptValue(BufferedStream<LUAToken> tokenStream) {
        return acceptValue(new ExpressionBuilder(), tokenStream);
    }

    private LUAValue acceptValue(ExpressionBuilder builder, BufferedStream<LUAToken> tokenStream) {
        System.out.println(tokenStream.current().text);
        // TODO: calls, array access
        // TODO: functions are values!
        LUAValue ref = acceptSingleValue(tokenStream);
        if (ref != null) {
            // TODO: typing
            builder.addValue(ref.getData(root, root.DOUBLE));
            while (Resolver.nextThing(tokenStream, 1) == Resolver.ThingType.OPERATION) {
                tokenStream.advance();
                builder.addOperation(tokenStream.current().text);
                tokenStream.advance();
                builder.addValue(acceptSingleValue(tokenStream).getData(root, root.DOUBLE));
            }
            return new LUAValue(root, 0, builder.build(root));
        } else if (tokenStream.current().text.equals("true")) {
            return CONST_TRUE;
        } else if (tokenStream.current().text.equals("false")) {
            return CONST_FALSE;
        } else if (tokenStream.current().text.equals("nil")) {
            return CONST_NIL;
        }
        throw new RuntimeException("huh?");
    }

    private final BytePointer error = new BytePointer();

    private void acceptFor(BufferedStream<LUAToken> tokenStream) {
        pushScope();

        tokenStream.advance();
        String varName = acceptVariable(true, tokenStream);

        tokenStream.advance(2); // val ,

        LUAValue terminator = acceptValue(tokenStream);
        tokenStream.advance();

        LUAValue step;
        if (tokenStream.current().text.equals(",")) {
            tokenStream.advance();
            step = acceptValue(tokenStream);
            tokenStream.advance(); // value
        } else {
            step = CONST_1;
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

        LLVMValueRef condPN = root.compareG(step.getData(root, root.DOUBLE), root.CONST_0D);
        root.conditionalJump(condPN, startPos, startNeg);

        breakTo.push(block);

        {
            {
                // negative header
                builder.buildBlock(startNeg);
                LUAValue counter = scopes.peek().getVariable(root.DOUBLE, varName);
                LLVMValueRef cond = root.compareGE(counter.getData(root, root.DOUBLE), terminator.getData(root, root.DOUBLE));
                root.conditionalJump(cond, body, block);
            }
            {
                // positive header
                builder.buildBlock(startPos);
                LUAValue counter = scopes.peek().getVariable(root.DOUBLE, varName);
                LLVMValueRef cond = root.compareLE(counter.getData(root, root.DOUBLE), terminator.getData(root, root.DOUBLE));
                root.conditionalJump(cond, body, block);
            }
            {
                // counter
                builder.buildBlock(body);
                LUAValue counter = scopes.peek().getVariable(root.DOUBLE, varName);
                LLVMValueRef counterNV = root.sum(counter.getData(root, root.DOUBLE), step.getData(root, root.DOUBLE)); // TODO: don't directly sum
                LUAValue value = new LUAValue(counter.type, counterNV);
                scopes.peek().addVariable(true, varName, value);

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

        LUAValue condition = acceptValue(tokenStream);
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
        root.conditionalJump(condition.data, body, to);

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

        LLVMPassManagerRef pass = root.standardOptimizer(loopEliminationFactor);
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

        long bp = MemoryUtil.nmemAlloc(8);
        long bv = MemoryUtil.nmemAlloc(8);
        nt = System.nanoTime();
        JNI.invokePPPV(
                bp, bv,
                argV,
                addr
        );
        nt1 = System.nanoTime();
        long result = MemoryUtil.memGetLong(bv);
        MemoryUtil.nmemFree(bv);
        MemoryUtil.nmemFree(bp);
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

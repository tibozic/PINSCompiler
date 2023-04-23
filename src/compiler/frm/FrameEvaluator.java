/**
 * @ Author: turk
 * @ Description: Analizator klicnih zapisov.
 */

package compiler.frm;

import static common.RequireNonNull.requireNonNull;

import common.Constants;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

import java.util.Stack;

public class FrameEvaluator implements Visitor {
    /**
     * Opis definicij funkcij in njihovih klicnih zapisov.
     */
    private NodeDescription<Frame> frames;

    /**
     * Opis definicij spremenljivk in njihovih dostopov.
     */
    private NodeDescription<Access> accesses;

    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč in njihovih podatkovnih tipov.
     */
    private final NodeDescription<Type> types;

    private Stack<Frame.Builder> builderStack;
    private int level = 1;

    public FrameEvaluator(
        NodeDescription<Frame> frames, 
        NodeDescription<Access> accesses,
        NodeDescription<Def> definitions,
        NodeDescription<Type> types
    ) {
        requireNonNull(frames, accesses, definitions, types);
        this.frames = frames;
        this.accesses = accesses;
        this.definitions = definitions;
        this.types = types;
        builderStack = new Stack<>();
    }

    @Override
    public void visit(Call call) {
        int argsSize = call.arguments.stream().map(arg -> {
            var argType = types.valueFor(arg);
            assert argType.isPresent(): String.format("ASSERT FAILED: No type found for argument `%s` in function call `%s`\n",
                    arg.toString(), call.name);

            return argType.get().sizeInBytesAsParam();
        }).mapToInt(i -> i).sum();

        argsSize += Constants.WordSize; // For the SL

        var funBuilder = builderStack.peek();
        funBuilder.addFunctionCall(argsSize);
    }


    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);
    }


    @Override
    public void visit(Block block) {
        block.expressions.stream().forEach(expr -> {
            expr.accept(this);
        });
    }


    @Override
    public void visit(For forLoop) {
        forLoop.counter.accept(this);
        forLoop.low.accept(this);
        forLoop.high.accept(this);
        forLoop.step.accept(this);
        forLoop.body.accept(this);
    }


    @Override
    public void visit(Name name) {
        return;
    }


    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);

        ifThenElse.elseExpression.ifPresent(expr -> expr.accept(this));
    }


    @Override
    public void visit(Literal literal) {
        return;
    }


    @Override
    public void visit(Unary unary) {
       unary.expr.accept(this);
    }


    @Override
    public void visit(While whileLoop) {
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);
    }


    @Override
    public void visit(Where where) {
        where.defs.accept(this);
        where.expr.accept(this);
    }


    @Override
    public void visit(Defs defs) {
        defs.definitions.stream().forEach(def -> {
            def.accept(this);
        });
    }


    @Override
    public void visit(FunDef funDef) {
        Frame.Label funLabel = null;
        if( level ==  1 ) {
            funLabel = Frame.Label.named(funDef.name);
        }
        else {
            funLabel = Frame.Label.nextAnonymous();
        }

        var funBuilder = new Frame.Builder(funLabel, level);
        builderStack.push(funBuilder);
        // ^^^ setup

        funDef.parameters.stream().forEach(param -> {
            param.accept(this);
        });

        level++;

        funDef.body.accept(this);

        level--;
        // vvv end
        frames.store(funBuilder.build(), funDef);
        builderStack.pop();
    }


    @Override
    public void visit(TypeDef typeDef) {
        typeDef.type.accept(this);
    }


    @Override
    public void visit(VarDef varDef) {
        var varDefType = types.valueFor(varDef);
        assert (varDefType.isPresent()): String.format("ASSERT FAILED: No type found for variable %s\n",
                varDef.name);
        var size = varDefType.get().sizeInBytes();
        var label = Frame.Label.named(varDef.name);

        if( level == 1 ) {
            var newAccess = new Access.Global(size, label);
            accesses.store(newAccess, varDef);
            return;
        }

        var funBuilder = builderStack.peek();
        var offset = funBuilder.addLocalVariable(size);
        var newAccess = new Access.Local(size, offset, level);
        accesses.store(newAccess, varDef);
    }


    @Override
    public void visit(Parameter parameter) {
        var funBuilder = builderStack.peek();

        var parameterType = types.valueFor(parameter);
        assert (parameterType.isPresent()): String.format("ASSERT FAILED: No type found for parameter `%s`\n",
                parameter.name);

        var size = parameterType.get().sizeInBytesAsParam();
        var offset = funBuilder.addParameter(size);
        var newAccess = new Access.Parameter(size, offset, level);
        accesses.store(newAccess, parameter);
    }


    @Override
    public void visit(Array array) {
        array.type.accept(this);
    }


    @Override
    public void visit(Atom atom) {
        return;
    }


    @Override
    public void visit(TypeName name) {
        return;
    }
}

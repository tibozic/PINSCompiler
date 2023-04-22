/**
 * @ Author: turk
 * @ Description: Preverjanje tipov.
 */

package compiler.seman.type;

import static common.RequireNonNull.requireNonNull;

import common.Report;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

import java.lang.reflect.Executable;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeChecker implements Visitor {
    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč, ki jim priredimo podatkovne tipe.
     */
    private NodeDescription<Type> types;

    public TypeChecker(NodeDescription<Def> definitions, NodeDescription<Type> types) {
        requireNonNull(definitions, types);
        this.definitions = definitions;
        this.types = types;
    }

    @Override
    public void visit(Call call) {
        call.arguments.stream().forEach(arg -> { arg.accept(this); });

        var funDef = definitions.valueFor(call);

        if( funDef.isEmpty() )
            Report.error(call.position, String.format("ERROR: Definition for function `%s` not found\n", call.name));

        var funTypeType = types.valueFor(funDef.get());

        if( funTypeType.isEmpty() )
            funDef.get().accept(this);

        funTypeType = types.valueFor(funDef.get());

        var funType = funTypeType.get().asFunction();

        if( funType.isEmpty() )
            Report.error(call.position, String.format("ERROR: Variable `%s` is not a function\n", call.name));

        var funReturnType = funType.get().returnType;

        // check that the args match
        if( call.arguments.size() != funType.get().parameters.size() )
            Report.error(call.position,
                    String.format("ERROR: Wrong number of arguments for a function call: `%d` != `%d`\n",
                            funType.get().parameters.size(),
                            call.arguments.size()));

        for(int i = 0; i < call.arguments.size(); ++i) {
            var argType = types.valueFor(call.arguments.get(i)).get();
            var paramType = funType.get().parameters.get(i);

            if( !argType.equals(paramType) )
                Report.error(call.position,
                        String.format("ERROR: Call argument type does not match function parameter type: `%s` != `%s`",
                                paramType.toString(),
                                argType));
        }

        types.store(funReturnType, call);
    }

    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);

        var leftType = types.valueFor(binary.left);
        var rightType = types.valueFor(binary.right);

        if( leftType.isEmpty() ) Report.error(binary.left.position, "ERROR: Unknown type for expression\n");
        if( rightType.isEmpty() ) Report.error(binary.right.position, "ERROR: Unknown type for expression\n");

        if( binary.operator == Binary.Operator.ARR ) {
            if( !(rightType.get().isInt()) )
                Report.error(binary.position,
                        "ERROR: Expression inside [] has to be an integer in an array call.\n");

            var arr = leftType.get().asArray();


            if( arr.isEmpty() )
                Report.error(binary.position, "ERROR: Left side of array call has to be an array\n");

            var arrElementType = arr.get().type;

            types.store(arrElementType, binary);
            return;
        }

        if( !(leftType.get().equals(rightType.get())) )
            Report.error(binary.position,
                    String.format("ERROR: Types for left and right side of expression don't match: `%s` != `%s`\n",
                            leftType.get(),
                            rightType.get()));

        if( binary.operator.isAndOr() ) {
            if( !leftType.get().isLog() )
                Report.error(binary.position,
                        String.format("ERROR: Logical operator `%s` only supports logical values\n",
                                binary.operator));

            types.store(new Type.Atom(Type.Atom.Kind.LOG), binary);
        }

        if( binary.operator.isArithmetic() ) {
            if( leftType.get().isInt() ) {
                types.store(new Type.Atom(Type.Atom.Kind.INT), binary);
                return;
            }
            else
                Report.error(binary.position,
                        String.format("ERROR: Arithmetic operator `%s` only supports integer values\n",
                                binary.operator));
        }

        if( binary.operator.isComparison() ) {
            if( leftType.get().isInt() || leftType.get().isLog() ) {
                types.store(new Type.Atom(Type.Atom.Kind.LOG), binary);
                return;
            }
            else
                Report.error(binary.position,
                        String.format("ERROR: Comparison operator `%s` only supports integer and logical values\n",
                                binary.operator));
        }

        if( binary.operator == Binary.Operator.ASSIGN ) {
            if( leftType.get().isInt() || leftType.get().isLog() || leftType.get().isStr() ) {
                types.store(leftType.get(), binary);
                return;
            }
            else
                Report.error(binary.position,
                        String.format("ERROR: Assign operator `%s` only supports integer and logical values\n",
                                binary.operator));
        }

    }

    @Override
    public void visit(Block block) {
        block.expressions.stream().forEach(expr -> { expr.accept(this); });

        var blockType = types.valueFor(block.expressions.get(block.expressions.size()-1));

        if( blockType.isEmpty() )
            Report.error(block.position, "ERROR: Unknown type of last expression in block\n");

        types.store(blockType.get(), block);
    }

    @Override
    public void visit(For forLoop) {
        forLoop.counter.accept(this);
        forLoop.high.accept(this);
        forLoop.low.accept(this);
        forLoop.step.accept(this);

        var loopCounter = types.valueFor(forLoop.counter);
        var loopHighType = types.valueFor(forLoop.high);
        var loopLowType = types.valueFor(forLoop.low);
        var loopStepType = types.valueFor(forLoop.step);

        if( loopCounter.isEmpty() )
            Report.error(forLoop.counter.position,
                    String.format("ERROR: Unknown type for loop counter\n"));

        if( loopHighType.isEmpty() )
            Report.error(forLoop.high.position,
                    String.format("ERROR: Unknown type for function high limit\n"));

        if( loopLowType.isEmpty() )
            Report.error(forLoop.low.position,
                    String.format("ERROR: Unknown type for function low limit\n"));

        if( loopStepType.isEmpty() )
            Report.error(forLoop.step.position,
                    String.format("ERROR: Unknown type for function step\n"));


        if( !loopCounter.get().isInt() )
            Report.error(forLoop.counter.position,
                    String.format("ERROR: For loop counter type has to be an integer not `%s`\n",
                            loopCounter.get()));

        if( !loopHighType.get().isInt() )
            Report.error(forLoop.high.position,
                    String.format("ERROR: For loop high limit type has to be an integer not `%s`\n",
                            loopHighType.get()));

        if( !loopLowType.get().isInt() )
            Report.error(forLoop.low.position,
                    String.format("ERROR: For loop low limit type has to be an integer not `%s`\n",
                            loopLowType.get()));

        if( !loopStepType.get().isInt() )
            Report.error(forLoop.step.position,
                    String.format("ERROR: For loop step type has to be an integer not `%s`\n",
                            loopStepType.get()));

        forLoop.body.accept(this);

        types.store(new Type.Atom(Type.Atom.Kind.VOID), forLoop);
    }

    @Override
    public void visit(Name name) {
        var nameDef = definitions.valueFor(name);

        if( nameDef.isEmpty() )
            Report.error(name.position, String.format("ERROR: No definition found for variable `%s`\n", name.name));

        var nameType = types.valueFor(nameDef.get());

        if( nameType.isEmpty() )
            nameDef.get().accept(this);

        nameType = types.valueFor(nameDef.get());

        if( nameType.isEmpty() )
            Report.error(name.position, String.format("ERROR: No type found for variable `%s`\n", name.name));

        types.store(nameType.get(), name);
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        var ifCondType = types.valueFor(ifThenElse.condition);

        if( ifCondType.isEmpty() )
            Report.error(ifThenElse.position,
                    String.format("ERROR: Unknown condition type in IF block\n"));

        ifThenElse.thenExpression.accept(this);

        if( ifThenElse.elseExpression.isPresent() )
            ifThenElse.elseExpression.get().accept(this);

        types.store(new Type.Atom(Type.Atom.Kind.VOID), ifThenElse);
    }

    @Override
    public void visit(Literal literal) {
        Type.Atom literalType = null;
        if( literal.type == Atom.Type.INT )
            literalType = new Type.Atom(Type.Atom.Kind.INT);
        if( literal.type == Atom.Type.LOG )
            literalType = new Type.Atom(Type.Atom.Kind.LOG);
        if( literal.type == Atom.Type.STR )
            literalType = new Type.Atom(Type.Atom.Kind.STR);

        types.store(literalType, literal);
    }

    @Override
    public void visit(Unary unary) {
        unary.expr.accept(this);

        var unaryExprType = types.valueFor(unary.expr);

        if( unaryExprType.isEmpty() )
            Report.error(unary.position,
                    String.format("ERROR: Unknown type for unary expression\n"));

        if( unary.operator == Unary.Operator.ADD || unary.operator == Unary.Operator.SUB ) {
            if( !unaryExprType.get().isInt() )
                Report.error(unary.position,
                        "ERROR: Unary operators `+` and `-` only support integer values\n");

            types.store(new Type.Atom(Type.Atom.Kind.INT), unary);
            return;
        }

        if( unary.operator == Unary.Operator.NOT ) {
            if( !unaryExprType.get().isLog() )
                Report.error(unary.position, "ERROR: Unary operator `!` only supports logical values\n");

            types.store(new Type.Atom(Type.Atom.Kind.LOG), unary);
            return;
        }

        Report.error(unary.position,
                String.format("ERROR: Unsupported operator `%s` for unary operation\n", unary.operator.toString()));
    }

    @Override
    public void visit(While whileLoop) {
        whileLoop.condition.accept(this);
        var whileCondType = types.valueFor(whileLoop.condition);

        if( whileCondType.isEmpty() )
            Report.error(whileLoop.position,
                    String.format("ERROR: Unknown condition expression type in while loop\n"));

        if( !whileCondType.get().isLog() )
            Report.error(whileLoop.position,
                    String.format("ERROR: Loop condition has to be of logical type not `%s`\n",
                            whileCondType.get().toString()));

        whileLoop.body.accept(this);

        types.store(new Type.Atom(Type.Atom.Kind.VOID), whileLoop);
    }

    @Override
    public void visit(Where where) {
        where.defs.accept(this);
        where.expr.accept(this);

        var whereType = types.valueFor(where.expr);

        if( whereType.isEmpty() )
            Report.error(where.position, "ERROR: Unknown type of expression in WHERE block\n");

        types.store(whereType.get(), where);
    }

    @Override
    public void visit(Defs defs) {
        defs.definitions.stream().forEach( (def) -> {
            def.accept(this);
        });
    }

    @Override
    public void visit(FunDef funDef) {
        funDef.parameters.stream().forEach(param -> {param.accept(this);});
        funDef.type.accept(this);

        var funReturnType = types.valueFor(funDef.type);

        if( funReturnType.isEmpty() )
            Report.error(funDef.position,
                    String.format("ERROR: Unknown return type for function `%s`\n", funDef.type));

        var paramsType = funDef.parameters.stream().map(param -> {
            var paramType = types.valueFor(param);
            if( paramType.isEmpty() )
                Report.error(param.position,
                        String.format("ERROR: Unknown type `%s` for parameter `%s`\n", param.type, param.name));
            return paramType.get();
        }).collect(Collectors.toList());

        var funType = new Type.Function(paramsType, funReturnType.get());
        types.store(funType, funDef);

        funDef.body.accept(this);
        var bodyType = types.valueFor(funDef.body);

        if( bodyType.isEmpty() )
            Report.error(funDef.body.position, String.format("ERROR: Unknown type for function body\n"));

        if( !(funReturnType.get().equals(bodyType.get())) )
            Report.error(funDef.position,
                    String.format("ERROR: Function return type doesn't match function body return type: `%s`!=`%s`\n",
                            funReturnType.get(), bodyType.get()));
    }

    @Override
    public void visit(TypeDef typeDef) {
        try {
            typeDef.type.accept(this);
        } catch (StackOverflowError e1) {
            Report.error(typeDef.position, "ERROR: Cycle detected");
        }

        var typeDefType = types.valueFor(typeDef.type);

        if( typeDefType.isEmpty() )
            Report.error(typeDef.position,
                    String.format("ERROR: Unknown type `%s` for typedef `%s`\n", typeDef.type, typeDef.name));

        types.store(typeDefType.get(), typeDef);
    }

    @Override
    public void visit(VarDef varDef) {
        varDef.type.accept(this);

        var varType = types.valueFor(varDef.type);

        if( varType.isEmpty() )
            Report.error(varDef.position,
                    String.format("ERROR: Unknown type `%s` for variable `%s`\n", varDef.type, varDef.name));

        types.store(varType.get(), varDef);
    }

    @Override
    public void visit(Parameter parameter) {
        parameter.type.accept(this);

        var paramType = types.valueFor(parameter.type);

        if( paramType.isEmpty() )
            Report.error(parameter.position,
                    String.format("ERROR: Unknown type `%s` for parameter `%s`\n", parameter.type, parameter.name));

        types.store(paramType.get(), parameter);
    }

    @Override
    public void visit(Array array) {
        array.type.accept(this);

        var arrayElementType = types.valueFor(array.type);

        if( arrayElementType.isEmpty() )
            Report.error(array.position,
                    String.format("ERROR: Unknown type `%s` for array\n", array.type));

        var arrayType = new Type.Array(array.size, arrayElementType.get());

        types.store(arrayType, array);
    }

    @Override
    public void visit(Atom atom) {
        if( atom.type == Atom.Type.INT ) {
            types.store(new Type.Atom(Type.Atom.Kind.INT), atom);
            return;
        }
        if( atom.type == Atom.Type.LOG ) {
            types.store(new Type.Atom(Type.Atom.Kind.LOG), atom);
            return;
        }
        if( atom.type == Atom.Type.STR ) {
            types.store(new Type.Atom(Type.Atom.Kind.STR), atom);
            return;
        }
        Report.error(atom.position, String.format("ERROR: Unknown type for atom `%s`\n", atom));
    }

    @Override
    public void visit(TypeName name) {
        Optional<Def> nameDef = null;

        try {
            nameDef = definitions.valueFor(name);
        } catch (StackOverflowError e1) {
            Report.error(name.position, "ERROR: Cycle detected");
        }


        if( nameDef.isEmpty() )
            Report.error(name.position,
                    String.format("ERROR: No definition found for variable `%s`\n", name.identifier));

        var nameType = types.valueFor(nameDef.get());

        if( nameType.isEmpty() ) {
            try {
                nameDef.get().accept(this);
            } catch (StackOverflowError e1) {
                Report.error(name.position, "ERROR: Cycle detected");
            }
        }

        nameType = types.valueFor(nameDef.get());

        types.store(nameType.get(), name);
    }
}

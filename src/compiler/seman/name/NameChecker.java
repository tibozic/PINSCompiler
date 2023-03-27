/**
 * @ Author: turk
 * @ Description: Preverjanje in razreševanje imen.
 */

package compiler.seman.name;

import static common.RequireNonNull.requireNonNull;

import common.Report;
import compiler.common.Visitor;
import compiler.lexer.Position;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;
import compiler.seman.common.NodeDescription;
import compiler.seman.name.env.SymbolTable;
import compiler.seman.name.env.SymbolTable.DefinitionAlreadyExistsException;

import java.util.Optional;

public class NameChecker implements Visitor {
    /**
     * Opis vozlišč, ki jih povežemo z njihovimi
     * definicijami.
     */
    private NodeDescription<Def> definitions;

    /**
     * Simbolna tabela.
     */
    private SymbolTable symbolTable;

    /**
     * Ustvari nov razreševalnik imen.
     */
    public NameChecker(
        NodeDescription<Def> definitions,
        SymbolTable symbolTable
    ) {
        requireNonNull(definitions, symbolTable);
        this.definitions = definitions;
        this.symbolTable = symbolTable;
    }

    @Override
    public void visit(Call call) {
         var funDef = symbolTable.definitionFor(call.name);

         // Check that the variable is defined
         if( funDef.isEmpty() )
             Report.error(call.position,
                     String.format("ERROR: Function with name `%s` doesn't exist\n", call.name));

         // Check that  the variable is a function
        if( !(funDef.get() instanceof FunDef) )
            Report.error(call.position,
                    String.format("ERROR: `%s` is not a function\n", call.name));

        definitions.store(funDef.get(), call);

        call.arguments.stream().forEach(arg -> arg.accept(this));
    }

    @Override
    public void visit(Binary binary) {
        if( binary.operator == Binary.Operator.ARR ) {
            // Check if left side of array call is a variable
            if( !(binary.left instanceof Name) )
                Report.error(binary.position,
                        String.format("ERROR: Left side of array call has to be a Name\n"));

            var arrayDefinition = symbolTable.definitionFor(((Name) binary.left).name);

            // Check if the variable was defined
            if( arrayDefinition.isEmpty() )
                Report.error(binary.position,
                        String.format("ERROR: Unknown name `%s` in array call", ((Name) binary.left).name));

            // Check if the variable is of array type

            if( arrayDefinition.get() instanceof VarDef arrayType ) {
                System.out.println("A");
                if( !(arrayType.type instanceof Array) )
                    Report.error(arrayDefinition.get().position,
                            String.format("ERROR: Variable `%s` is not of type `ARRAY`\n", arrayDefinition.get().name));

                definitions.store(arrayDefinition.get(), binary.left);
            }
            else if( arrayDefinition.get() instanceof Parameter arrayType ) {
                System.out.println("B");
                if( !(arrayType.type instanceof Array) )
                    Report.error(arrayDefinition.get().position,
                            String.format("ERROR: Variable `%s` is not of type `ARRAY`\n", arrayDefinition.get().name));

                definitions.store(arrayDefinition.get(), binary.left);
            }
            else {
                System.out.println("C");
                Report.error(arrayDefinition.get().position,
                        String.format("ERROR: Variable `%s` is not of type `ARRAY`\n", arrayDefinition.get().name));
            }

            binary.right.accept(this);
        }
        else {
            if( binary.left instanceof Name binaryName ) {
                var binaryNameLocation = symbolTable.definitionFor(binaryName.name);

                if( binaryNameLocation.isEmpty() )
                    Report.error(binaryName.position,
                            String.format("ERROR: Unkown variable in binary operation: `%s`\n", binaryName.name));

                definitions.store(binaryNameLocation.get(), binaryName);
            }
            else {
                binary.left.accept(this);
            }

            if( binary.right instanceof Name binaryName ) {
                var binaryNameLocation = symbolTable.definitionFor(binaryName.name);

                if( binaryNameLocation.isEmpty() )
                    Report.error(binaryName.position,
                            String.format("ERROR: Unkown variable in binary operation: `%s`\n", binaryName.name));

                definitions.store(binaryNameLocation.get(), binaryName);
            }
            else {
                binary.right.accept(this);
            }
        }
    }

    @Override
    public void visit(Block block) {
        block.expressions.stream()
                .forEach(expr -> expr.accept(this));
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
        Optional<Def> def = symbolTable.definitionFor(name.name);

        if( def.isEmpty() ) {
            Report.error(name.position,
                    String.format("ERROR: No definition found for `%s`\n",
                            name.name));
        }

        definitions.store(def.get(), name);
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);
        if( ifThenElse.elseExpression.isPresent() )
            ifThenElse.elseExpression.get().accept(this);
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
        // first pass is through all definitions
        /*
        where.defs.definitions.stream().
                forEach(def -> {
                    if( def instanceof VarDef varDef && varDef.type instanceof TypeName varDefType ) {
                        var varDefTypeDefinition = symbolTable.definitionFor(varDefType.identifier);

                        if( varDefTypeDefinition.isEmpty() )
                            Report.error(varDefType.position,
                                    String.format("ERROR: Unknown definition type `%s`\n", varDefType));

                        definitions.store(varDefTypeDefinition.get(), varDefType);
                    }

                    // TODO: Same as above for function & type definitions
                });

         */

        // FIXME: Possibly have to precheck all the definitions (same as parameters in function definition)

        symbolTable.inNewScope(() -> {
            // first pass is through all definitions
            where.defs.definitions.stream().
                    forEach(def -> {
                        try {
                            symbolTable.insert(def);
                        } catch (DefinitionAlreadyExistsException e) {
                            Report.error(def.position,
                                    String.format("ERROR: Definition for `%s` already exists in scope.\n",
                                            def.name));
                        }
                    });

            // second pass goes into depth
            where.defs.definitions.stream().forEach(def -> def.accept(this));


            where.expr.accept(this);
        });
    }

    @Override
    public void visit(Defs defs) {
        symbolTable.inNewScope(() -> {
            // first pass is through all definitions
            defs.definitions.stream().
                    forEach(def -> {
                        try {
                            symbolTable.insert(def);
                        } catch (DefinitionAlreadyExistsException e) {
                            Report.error(def.position,
                                    String.format("ERROR: Definition for `%s` already exists in scope.\n",
                                            def.name));
                        }
                    });

            // second pass goes into depth
            defs.definitions.stream().forEach(def -> def.accept(this));
        });

    }

    public void functionDefinitionPrecheck(FunDef funDef) {
        // Check that parameters aren't of invalid type
        funDef.parameters.stream()
                .forEach(param -> {
                    if( param.type instanceof TypeName parameterType ) {
                        var parameterTypeDefinition = symbolTable.definitionFor(parameterType.identifier);

                        if( parameterTypeDefinition.isEmpty() )
                            Report.error(parameterType.position,
                                    String.format("ERROR: Unknown parameter type `%s`\n", parameterType.identifier));

                        definitions.store(parameterTypeDefinition.get(), parameterType);
                    }
                });

        // check that the return type of function is valid
        if( funDef.type instanceof TypeName typeDefName ) {
            var typeDef = symbolTable.definitionFor(typeDefName.identifier);

            if( typeDef.isEmpty() )
                Report.error(typeDefName.position,
                        String.format("ERROR: Unknown function return type `%s`\n", typeDefName.identifier));

            definitions.store(typeDef.get(), typeDefName);
        }
    }

    @Override
    public void visit(FunDef funDef) {

        functionDefinitionPrecheck(funDef);

        // go deep into the function
        symbolTable.inNewScope(() -> {

            funDef.parameters.stream()
                            .forEach(param -> {
                                try {
                                    symbolTable.insert(param);
                                } catch (DefinitionAlreadyExistsException e) {
                                    Report.error(param.position,
                                        String.format("ERROR: Parameter definition `%s` already exists in scope.\n",
                                                param.name));
                                }
                            });


            funDef.body.accept(this);
        });
    }

    @Override
    public void visit(TypeDef typeDef) {
        if( typeDef.type instanceof TypeName typeDefName) {
            var typeDefNameLocation = symbolTable.definitionFor(typeDefName.identifier);

            if( typeDefNameLocation.isEmpty() )
                Report.error(typeDefName.position,
                        String.format("ERROR: Unknown TypeDef defintion `%s`\n", typeDefName.identifier));

            definitions.store(typeDefNameLocation.get(), typeDefName);
        }
    }

    @Override
    public void visit(VarDef varDef) {
        if( varDef.type instanceof TypeName typeDefName ) {
            var typeDefNameLocation = symbolTable.definitionFor(typeDefName.identifier);

            if( typeDefNameLocation.isEmpty() )
                Report.error(typeDefName.position,
                        String.format("ERROR: Unknown variable type `%s`\n",
                                typeDefName.identifier));

            definitions.store(typeDefNameLocation.get(), typeDefName);
        }
    }

    @Override
    public void visit(Parameter parameter) {
        try {
            symbolTable.insert(parameter);
        } catch (DefinitionAlreadyExistsException e1) {
            Report.error(parameter.position,
                    String.format("ERROR: Definition for parameter `%s` already exists in scope.\n",
                            parameter.name));
        }
    }

    @Override
    public void visit(Array array) {
        if( array.type instanceof TypeName arrayType) {
            var arrayTypeDefinition = symbolTable.definitionFor(arrayType.identifier);

            if( arrayTypeDefinition.isEmpty() )
                Report.error(arrayType.position,
                        String.format("ERROR: Unknown type in array defintion `%s`\n", arrayType.identifier));

            definitions.store(arrayTypeDefinition.get(), arrayType);
        }
    }

    @Override
    public void visit(Atom atom) {
        return;
    }

    @Override
    public void visit(TypeName name) {
        Optional<Def> def = symbolTable.definitionFor(name.identifier);

        if( def.isEmpty() ) {
            Report.error(name.position,
                    String.format("ERROR: No definition found for `%s`\n",
                            name.identifier));
        }
    }
}

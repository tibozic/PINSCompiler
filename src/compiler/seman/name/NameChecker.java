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
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

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
			SymbolTable symbolTable) {
		requireNonNull(definitions, symbolTable);
		this.definitions = definitions;
		this.symbolTable = symbolTable;
	}

	@Override
	public void visit(Call call) {

		if(is_std_function(call.name)) {
			var funPosition = new Position(new Position.Location(-1, -1),
										   new Position.Location(-1, -1));

			List<Parameter> params = new ArrayList<>();
			Type type = null;
			Expr funBody = null;

			switch(call.name) {
				case "print_int": {
					params.add(new Parameter(funPosition,
											 "p1",
											 Atom.INT(funPosition)));
					type = Atom.INT(funPosition);
					funBody = new Literal(funPosition, "0", Atom.Type.INT);
					break;
				}
				case "print_str": {
					params.add(new Parameter(funPosition,
											 "p1",
											 Atom.STR(funPosition)));
					type = Atom.STR(funPosition);
					funBody = new Literal(funPosition, "0", Atom.Type.STR);
					break;
				}
				case "print_log": {
					params.add(new Parameter(funPosition,
											 "p1",
											 Atom.LOG(funPosition)));
					type = Atom.LOG(funPosition);
					funBody = new Literal(funPosition, "false", Atom.Type.LOG);
					break;
				}
				case "rand_int": {
					params.add(new Parameter(funPosition,
											 "p1",
											 Atom.INT(funPosition)));
					params.add(new Parameter(funPosition,
											 "p2",
											 Atom.INT(funPosition)));
					type = Atom.INT(funPosition);
					funBody = new Literal(funPosition, "0", Atom.Type.INT);
					break;
				}
				case "seed": {
					params.add(new Parameter(funPosition,
											 "p1",
											 Atom.INT(funPosition)));
					type = Atom.INT(funPosition);
					funBody = new Literal(funPosition, "0", Atom.Type.INT);
					break;
				}
				default: {
					Report.error(funPosition,
								 String.format("Unknown type for std function: `%s`\n", call.name));
				}
			}

			var funDef = new FunDef(funPosition, call.name, params, type, funBody);
			definitions.store(funDef, call);
			call.arguments.stream().forEach(arg -> arg.accept(this));
			return;
		}

		var funDef = symbolTable.definitionFor(call.name);

		// Check that the variable is defined
		if (funDef.isEmpty())
			Report.error(call.position,
					String.format("ERROR: Function with name `%s` doesn't exist\n", call.name));

		// Check that  the variable is a function
		if (!(funDef.get() instanceof FunDef))
			Report.error(call.position,
					String.format("ERROR: `%s` is not a function\n", call.name));

		definitions.store(funDef.get(), call);

		call.arguments.stream().forEach(arg -> arg.accept(this));
	}

	@Override
	public void visit(Binary binary) {
		binary.left.accept(this);
		binary.right.accept(this);
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

		if (def.isEmpty()) {
			Report.error(name.position,
					String.format("ERROR: No definition found for `%s`\n",
							name.name));
		}

		if ((def.get() instanceof VarDef) || (def.get() instanceof Parameter)) {
			definitions.store(def.get(), name);
			return;
		}

		if (def.get() instanceof FunDef)
			Report.error(name.position,
					String.format("ERROR: Function `%s` used as a normal variable\n", def.get().name));

		if (def.get() instanceof TypeDef)
			Report.error(name.position,
					String.format("ERROR: Type `%s` used as a normal variable\n", def.get().name));

	}

	@Override
	public void visit(IfThenElse ifThenElse) {
		ifThenElse.condition.accept(this);
		ifThenElse.thenExpression.accept(this);
		if (ifThenElse.elseExpression.isPresent())
			ifThenElse.elseExpression.get().accept(this);
	}

	@Override
	public void visit(Literal literal) {
		return;
	}

	@Override
	public void visit(Unary unary) {
		if (unary.expr instanceof Name exprName) {
			var exprNameDefinition = symbolTable.definitionFor(exprName.name);

			if (exprNameDefinition.isEmpty())
				Report.error(exprName.position,
						String.format("ERROR: Unkown variable in binary operation: `%s`\n", exprName.name));

			if ((exprNameDefinition.get() instanceof VarDef) || (exprNameDefinition.get() instanceof Parameter)) {
				definitions.store(exprNameDefinition.get(), exprName);
			} else {
				Report.error(unary.position,
						String.format("ERROR: `%s` is not a valid variable\n",
								exprNameDefinition.get().name));
			}
		} else {
			unary.expr.accept(this);
		}
	}

	@Override
	public void visit(While whileLoop) {
		whileLoop.condition.accept(this);
		whileLoop.body.accept(this);
	}

	@Override
	public void visit(Where where) {
		symbolTable.inNewScope(() -> {
			// first pass is through all definitions
			where.defs.definitions.stream().forEach(def -> {
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
			defs.definitions.stream().forEach(def -> {
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
					if (param.type instanceof TypeName parameterType) {
						parameterType.accept(this);
					} else if (param.type instanceof Array paramArray) {
						paramArray.accept(this);
					}
				});

		// check that the return type of function is valid
		if (funDef.type instanceof TypeName typeDefName) {
			var typeDefDefinition = symbolTable.definitionFor(typeDefName.identifier);

			if (typeDefDefinition.isEmpty())
				Report.error(typeDefName.position,
						String.format("ERROR: Unknown function return type `%s`\n", typeDefName.identifier));

			if (!(typeDefDefinition.get() instanceof TypeDef))
				Report.error(typeDefName.position,
						String.format("ERROR: Function return type `%s` is not a valid type\n",
								typeDefDefinition.get().name));

			definitions.store(typeDefDefinition.get(), typeDefName);
		} else if (funDef.type instanceof Array typeDefArray) {
			typeDefArray.accept(this);
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
		if (typeDef.type instanceof TypeName typeDefName) {
			var typeDefNameLocation = symbolTable.definitionFor(typeDefName.identifier);

			if (typeDefNameLocation.isEmpty())
				Report.error(typeDefName.position,
						String.format("ERROR: Unknown TypeDef defintion `%s`\n", typeDefName.identifier));

			definitions.store(typeDefNameLocation.get(), typeDefName);
		} else if (typeDef.type instanceof Array arrayDef) {
			arrayDef.accept(this);
		}
	}

	@Override
	public void visit(VarDef varDef) {
		if (varDef.type instanceof TypeName typeDefName) {
			var typeDefNameDefinition = symbolTable.definitionFor(typeDefName.identifier);

			if (typeDefNameDefinition.isEmpty())
				Report.error(typeDefName.position,
						String.format("ERROR: Unknown variable type `%s`\n",
								typeDefName.identifier));

			if (!(typeDefNameDefinition.get() instanceof TypeDef))
				Report.error(varDef.position,
						String.format("ERROR: Variable type `%s` is not a type\n", typeDefNameDefinition.get().name));

			definitions.store(typeDefNameDefinition.get(), typeDefName);
		} else if (varDef.type instanceof Array arrayDef) {
			arrayDef.accept(this);
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
		if (array.type instanceof TypeName arrayType) {
			var arrayTypeDefinition = symbolTable.definitionFor(arrayType.identifier);

			if (arrayTypeDefinition.isEmpty())
				Report.error(arrayType.position,
						String.format("ERROR: Unknown type in array defintion `%s`\n", arrayType.identifier));

			definitions.store(arrayTypeDefinition.get(), arrayType);
			return;
		} else if (array.type instanceof Array arrayType) {
			arrayType.accept(this);
		}
	}

	@Override
	public void visit(Atom atom) {
		return;
	}

	@Override
	public void visit(TypeName name) {
		Optional<Def> def = symbolTable.definitionFor(name.identifier);

		if (def.isEmpty()) {
			Report.error(name.position,
					String.format("ERROR: No definition found for `%s`\n",
							name.identifier));
		}

		definitions.store(def.get(), name);
	}

	public boolean is_std_function(String function) {
		String[] std_functions = new String[] { "print_str", "print_int", "print_log", "rand_int", "seed" };

		if( Arrays.asList(std_functions).contains(function) ) {
			return true;
		}
		return false;
	}
}

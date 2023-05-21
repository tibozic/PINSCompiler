/**
 * @ Author: turk
 * @ Description: Generator vmesne kode.
 */

package compiler.ir;

import static common.RequireNonNull.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import common.Constants;
import compiler.common.Visitor;
import compiler.frm.Access;
import compiler.frm.Frame;
import compiler.frm.Frame.Label;
import compiler.ir.chunk.Chunk;
import compiler.ir.code.IRNode;
import compiler.ir.code.expr.*;
import compiler.ir.code.stmt.*;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.expr.Binary.Operator;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

import java.util.Arrays;

public class IRCodeGenerator implements Visitor {
    /**
     * Preslikava iz vozlišč AST v vmesno kodo.
     */
    private NodeDescription<IRNode> imcCode;

    /**
     * Razrešeni klicni zapisi.
     */
    private final NodeDescription<Frame> frames;

    /**
     * Razrešeni dostopi.
     */
    private final NodeDescription<Access> accesses;

    /**
     * Razrešene definicije.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Razrešeni tipi.
     */
    private final NodeDescription<Type> types;

    /**
     * **Rezultat generiranja vmesne kode** - seznam fragmentov.
     */
    public List<Chunk> chunks = new ArrayList<>();

	/**
	 * Steje staticni nivo na katerem smo trenutno
	 */
	private int currentStaticLevel = 0;
	

    public IRCodeGenerator(
        NodeDescription<IRNode> imcCode,
        NodeDescription<Frame> frames, 
        NodeDescription<Access> accesses,
        NodeDescription<Def> definitions,
        NodeDescription<Type> types
    ) {
        requireNonNull(imcCode, frames, accesses, definitions, types);
        this.types = types;
        this.imcCode = imcCode;
        this.frames = frames;
        this.accesses = accesses;
        this.definitions = definitions;
    }

	public boolean is_std_function(String function) {
		String[] std_functions = new String[] { "print_str", "print_int", "print_log", "rand_int", "seed" };

		if( Arrays.asList(std_functions).contains(function) ) {
			return true;
		}
		return false;
	}

    @Override
    public void visit(Call call) {

		if( is_std_function(call.name) ) {
			List<IRExpr> args = new ArrayList<>();

			args.add(new ConstantExpr(-1));

			call.arguments.stream().forEach(arg -> {
					arg.accept(this);

					var argExpr = imcCode.valueFor(arg);

					assert argExpr.isPresent(): "ASSERT FAILED";

					if( argExpr.get() instanceof NameExpr ) {
						args.add(new MemExpr((IRExpr)argExpr.get()));
					}
					else {
						args.add((IRExpr)argExpr.get());
					}

				});

			var callLabel = Label.named(call.name);
			var callExpr = new CallExpr(callLabel, args);

		
			var offset = new BinopExpr(NameExpr.SP(),
										new ConstantExpr(Constants.WordSize),
										BinopExpr.Operator.SUB);

			var memExpr = new MemExpr(offset);

			var moveStmt = new MoveStmt(memExpr,
										NameExpr.FP());

			var eseq = new EseqExpr(moveStmt, callExpr);

			// var saveRes = new ExpStmt(new MoveStmt(new MemExpr(NameExpr.FP()), eseq));
			
			imcCode.store(eseq, call);


			return;
		}

		/*
		  Before we start the call of function we have to save the
		  current FP into the space that is allocated in the frame
		  of the next function (current SP - OLD_FP offset)
		 */

		// Get frame of the function that is getting called
		// (so we can get the old FP offset)
		var funDef = definitions.valueFor(call);
		assert funDef.isPresent(): String.format("ASSERT FAILED: No definition found for function call `%s`\n",
												 call.name);
		var frame = frames.valueFor(funDef.get());
		assert frame.isPresent(): String.format("ASSERT FAILED: No frame found for call `%s`\n",
												call.name);

		
		// this is the oldFP location
		var offset = new BinopExpr(NameExpr.SP(),
									new ConstantExpr(frame.get().oldFPOffset()),
									BinopExpr.Operator.SUB);

		// MoveStmt has to have MemExpr on the left (where we are writing)
		var memExpr = new MemExpr(offset);

		// write the FP to oldFP location
		var moveStmt = new MoveStmt(memExpr,
									NameExpr.FP());


		/*
		  Calculate the static link
		 */
		IRExpr staticLink;
		if( this.currentStaticLevel == 0 ) {
			staticLink = new ConstantExpr(-1);
		}
		else {
			int staticLinkDelta = Math.abs(this.currentStaticLevel
								   - frame.get().staticLevel);

			IRExpr staticLinkMemExpr = NameExpr.FP();

			for(int i = 0; i < staticLinkDelta; ++i) {
				staticLinkMemExpr = new MemExpr(staticLinkMemExpr);
			}

			staticLink = staticLinkMemExpr;
		}

		/*
		  Translate all the args to IMC and pass them
		 */
		List<IRExpr> args = new ArrayList<>();

		// the first argument is always static link
		args.add(staticLink);

		call.arguments.stream().forEach(arg -> {
				arg.accept(this);

				var argExpr = imcCode.valueFor(arg);

				assert argExpr.isPresent(): "ASSERT FAILED";

				if( argExpr.get() instanceof NameExpr ) {
					args.add(new MemExpr((IRExpr)argExpr.get()));
				}
				else {
					args.add((IRExpr)argExpr.get());
				}

			});

		var callLabel = frame.get().label;
		var callExpr = new CallExpr(callLabel, args);

		var eseq = new EseqExpr(moveStmt, callExpr);
		imcCode.store(eseq, call);
	}

    @Override
    public void visit(Binary binary) {
		binary.left.accept(this);
		binary.right.accept(this);

		var leftIMC = imcCode.valueFor(binary.left);
		assert leftIMC.isPresent(): String.format("ASSERT FAILED: No IMC found for left side of binary operation `%s`\n",
												binary.toString());

		var rightIMC = imcCode.valueFor(binary.right);
		assert rightIMC.isPresent(): String.format("ASSERT FAILED: No IMC found for right side of binary operation `%s`\n",
												 binary.toString());

		assert leftIMC.get() instanceof IRExpr: String.format("ASSERT FAILED: Left side of binary expr is not IRExpr\n",
															  binary.toString());

		assert rightIMC.get() instanceof IRExpr: String.format("ASSERT FAILED: Right side of binary expr is not IRExpr\n",
															   binary.toString());


		if( binary.operator == Operator.ARR ) {
			var arrType = types.valueFor(binary.left);

			assert arrType.isPresent() : String.format("ASSERT FAILED: No type found for binary expresion of array\n");

			var arr = arrType.get().asArray();

			assert arr.isPresent() : String.format("ASSERT FAILED: Binary expression is not array but using array operator\n");

			IRExpr elementSize = new ConstantExpr(Constants.WordSize);

			if( arr.get().type.isAtom() ) {
				elementSize = new ConstantExpr(arr.get().type.sizeInBytes());
			} else {
				elementSize = new ConstantExpr(arr.get().type.sizeInBytes());
				// elementSize = (IRExpr)leftIMC.get();
			}

			var indexOffset = new BinopExpr((IRExpr)rightIMC.get(),
											(IRExpr)elementSize,
											BinopExpr.Operator.MUL);

			var goToIndex = new BinopExpr((IRExpr)leftIMC.get(),
										  (IRExpr)indexOffset,
										  BinopExpr.Operator.ADD);

			// var memAccess = new MemExpr(goToIndex);
			imcCode.store(goToIndex, binary);
			return;
		}
		else if( binary.operator == Operator.ASSIGN ) {

			// FIXME: NOTE: This is a hack
			// var memExpr = new MemExpr((IRExpr)leftIMC.get());
			var memExpr = (IRExpr)leftIMC.get();
			if( !(memExpr instanceof MemExpr) ) {
				memExpr = new MemExpr(memExpr);
			}

			var moveStmt = new MoveStmt(memExpr, (IRExpr) rightIMC.get());

			var eseqExpr = new EseqExpr(moveStmt, memExpr);

			imcCode.store(eseqExpr, binary);
			return;
		}
		else {
			var binaryIMC = new BinopExpr((IRExpr)leftIMC.get(),
									  (IRExpr)rightIMC.get(),
									  BinopExpr.convertOperator(binary.operator));
			imcCode.store(binaryIMC, binary);
			return;
		}
		
    }

    @Override
    public void visit(Block block) {
		List<IRStmt> stmts = new ArrayList<>();
		block.expressions.stream().forEach(expr -> {

			expr.accept(this);

			var exprNode = imcCode.valueFor(expr);

			assert exprNode.isPresent() : String.format("ASSERT FAILED: No IMC Code found for expr in block\n");

			if( exprNode.get() instanceof IRExpr ) {
				var exprStmt = new ExpStmt((IRExpr)exprNode.get());
				stmts.add(exprStmt);
			}
			else {
				stmts.add((IRStmt)exprNode.get());
			}

		});

		var lastExpr = imcCode.valueFor(block.expressions.get(block.expressions.size()-1));
		stmts.remove(stmts.size() - 1);
		var blockSeqStmts = new SeqStmt(stmts);

		var esqExpr = new EseqExpr((IRStmt)blockSeqStmts, (IRExpr)lastExpr.get());

		imcCode.store(esqExpr, block);
    }

    @Override
    public void visit(For forLoop) {
		forLoop.counter.accept(this);
		forLoop.low.accept(this);
		forLoop.high.accept(this);
		forLoop.step.accept(this);
		forLoop.body.accept(this);

		var counterIMC = imcCode.valueFor(forLoop.counter);
		var lowIMC = imcCode.valueFor(forLoop.low);
		var highIMC = imcCode.valueFor(forLoop.high);
		var stepIMC = imcCode.valueFor(forLoop.step);
		var bodyIMC = imcCode.valueFor(forLoop.body);

		assert counterIMC.isPresent() : "ASSERTION FAILED: ";
		assert lowIMC.isPresent() : "ASSERTION FAILED: ";
		assert highIMC.isPresent() : "ASSERTION FAILED: ";
		assert stepIMC.isPresent() : "ASSERTION FAILED: ";
		assert bodyIMC.isPresent() : "ASSERTION FAILED: ";

		var bodyExprStmt = new ExpStmt((IRExpr)bodyIMC.get());

		var labelBefore = new LabelStmt(Label.nextAnonymous());
		var labelInside = new LabelStmt(Label.nextAnonymous());
		var labelAfter = new LabelStmt(Label.nextAnonymous());

		List<IRStmt> stmts = new ArrayList<>();

		// initialization
		/*
		// this should be wrong, as counterIMC should always be
		// a Name, which always returns MemExpr
		stmts.add(new MoveStmt(new MemExpr((IRExpr)counterIMC.get()),
							   (IRExpr)lowIMC.get()));
		*/
		stmts.add(new MoveStmt((IRExpr)counterIMC.get(),
							   (IRExpr)lowIMC.get()));


		stmts.add(labelBefore);
		// comparison
		/*
		// same logic as with initialization
		var cond = new BinopExpr(new MemExpr((IRExpr)counterIMC.get()),
								 (IRExpr)highIMC.get(),
								 BinopExpr.Operator.LT);
		*/
		var cond = new BinopExpr((IRExpr)counterIMC.get(),
								 (IRExpr)highIMC.get(),
								 BinopExpr.Operator.LT);


		// CJUMP
		stmts.add(new CJumpStmt(cond,
								labelInside.label,
								labelAfter.label));

		stmts.add(labelInside);
		stmts.add(bodyExprStmt);
		// Increment
		/*
		// same logic as with initialization
		stmts.add(new MoveStmt(new MemExpr((IRExpr)counterIMC.get()),
							   new BinopExpr(new MemExpr((IRExpr)counterIMC.get()),
											 (IRExpr)stepIMC.get(),
											 BinopExpr.Operator.ADD)));
		*/
		stmts.add(new MoveStmt((IRExpr)counterIMC.get(),
							   new BinopExpr((IRExpr)counterIMC.get(),
											 (IRExpr)stepIMC.get(),
											 BinopExpr.Operator.ADD)));


		// Jump
		stmts.add(new JumpStmt(labelBefore.label));
		stmts.add(labelAfter);

		var forSeqStmts = new SeqStmt(stmts);

		imcCode.store(forSeqStmts, forLoop);
    }

    @Override
    public void visit(Name name) {
		var nameDef = definitions.valueFor(name);

		assert nameDef.isPresent() : String.format("ASSERT FAILED: No definition found for name `%s`\n",
												   name.name);

		var nameAccess = accesses.valueFor(nameDef.get());

		assert nameAccess.isPresent(): String.format("ASSERT FAILED: No access for name `%s`\n",
													  name.name);

		if( nameAccess.get() instanceof Access.Global nameAccessGlobal )
		{
			var nameIMC = new NameExpr(nameAccessGlobal.label);
			imcCode.store(new MemExpr(nameIMC), name);
			return;
		}

		if( nameAccess.get() instanceof Access.Stack nameAccessLocal ) {
			int deltaSL = Math.abs(nameAccessLocal.staticLevel
								   - this.currentStaticLevel);

			IRExpr fpDereferenced = new MemExpr(NameExpr.FP());

			for(int i = 0; i < deltaSL; ++i) {
				fpDereferenced = new MemExpr(fpDereferenced);
			}

			IRExpr binOpDerefrenced = new BinopExpr(fpDereferenced,
													new ConstantExpr(nameAccessLocal.offset),
													BinopExpr.Operator.ADD);

			var localRead = new MemExpr(binOpDerefrenced);

			imcCode.store(localRead, name);
			return;
		}

		assert false : "In Name name IRCodeGeneretor.java where I shouldn't be";
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
		ifThenElse.condition.accept(this);
		ifThenElse.thenExpression.accept(this);

		Optional<IRNode> elseIMC = Optional.empty();

		if( ifThenElse.elseExpression.isPresent() ) {
			ifThenElse.elseExpression.get().accept(this);
			elseIMC = imcCode.valueFor(ifThenElse.elseExpression.get());
		}
		
		var condIMC = imcCode.valueFor(ifThenElse.condition);
		var thenIMC = imcCode.valueFor(ifThenElse.thenExpression);

		assert condIMC.isPresent() : "ASSERT FAILED";
		assert thenIMC.isPresent() : "ASSERT FAILED";

		var thenLabel = new LabelStmt(Label.nextAnonymous());
		LabelStmt elseLabel = null;
		if( elseIMC.isPresent() ) 
			elseLabel = new LabelStmt(Label.nextAnonymous());
		var endLabel = new LabelStmt(Label.nextAnonymous());

		List<IRStmt> stmts = new ArrayList<>();

		// CJUMP
		if( elseIMC.isPresent() ) {
			stmts.add(new CJumpStmt((IRExpr)condIMC.get(),
								thenLabel.label,
								elseLabel.label));
		}
		else {
				stmts.add(new CJumpStmt((IRExpr)condIMC.get(),
								thenLabel.label,
								endLabel.label));
		}

		// then label
		stmts.add(thenLabel);
		// then code
		if( thenIMC.get() instanceof IRStmt ) {
			stmts.add((IRStmt)thenIMC.get());
		}
		else if( thenIMC.get() instanceof IRExpr ) {
			stmts.add(new ExpStmt((IRExpr)thenIMC.get()));
		}
		else {
			assert false : String.format("ASSERT FAILED: IfThenElse `then` statement is NOT IRExpr or IRStmt\n");
		}

		// jump end
		stmts.add(new JumpStmt(endLabel.label));

		if( elseIMC.isPresent() ) {
			// else label
			stmts.add(elseLabel);

			// else code
			if( elseIMC.get() instanceof IRStmt ) {
				stmts.add((IRStmt)elseIMC.get());
			}
			else if( elseIMC.get() instanceof IRExpr ) {
				stmts.add(new ExpStmt((IRExpr)elseIMC.get()));
			}
			else {
				assert false : String.format("ASSERT FAILED: IfThenElse `else` statement is NOT IRExpr or IRStmt\n");
			}
			
			// jump end
			stmts.add(new JumpStmt(endLabel.label));
		}

		// end label
		stmts.add(endLabel);

		var seqStmt = new SeqStmt(stmts);
		imcCode.store(seqStmt, ifThenElse);
    }

    @Override
    public void visit(Literal literal) {
		switch( literal.type ) {
			case INT: {
				imcCode.store(new ConstantExpr(Integer.valueOf(literal.value)),
							  literal);
				break;
			}
			case LOG: {
				int value;

				if (literal.value.equals("false"))
					value = 0;
				else
					value = 1;

				imcCode.store(new ConstantExpr(value), literal);
				break;
			}
			case STR: {
				var stringLabel = Label.nextAnonymous();
				var stringAccess = new Chunk.DataChunk(new Access.Global(4, stringLabel),
													   literal.value);

				chunks.add(stringAccess);

				imcCode.store(new NameExpr(stringLabel), literal);
				break;
			}
			default: {
				assert false : String.format("ASSERT FAILED: Unknown literal type `%s`\n",
											 literal.type);
				System.exit(99);
			}
		}
	}

    @Override
    public void visit(Unary unary) {
		unary.expr.accept(this);
		var expr = imcCode.valueFor(unary.expr);

		assert expr.isPresent(): "ASSERT FAILED";
		
		switch (unary.operator) {
			case ADD: {
				var addIMC = new BinopExpr(new ConstantExpr(0),
										   (IRExpr)expr.get(),
										   BinopExpr.Operator.ADD);
				imcCode.store(addIMC, unary);
				break;
			}
			case SUB: {
				var subIMC = new BinopExpr(new ConstantExpr(0),
										   (IRExpr)expr.get(),
										   BinopExpr.Operator.SUB);
				imcCode.store(subIMC, unary);
				break;
			}
			case NOT: {
				var value = expr.get();
				var notIMC = new BinopExpr(new ConstantExpr(1),
										   (IRExpr)value,
										   BinopExpr.Operator.SUB);
				imcCode.store(notIMC, unary);
				break;
			}
			default: {
				assert false: "ASSERT FAILED";
			}
		}
    }

    @Override
    public void visit(While whileLoop) {
		whileLoop.condition.accept(this);
		whileLoop.body.accept(this);

		var conditionIMC = imcCode.valueFor(whileLoop.condition);
		var bodyIMC = imcCode.valueFor(whileLoop.body);

		assert conditionIMC.isPresent() : "ASSERT FAILED";
		assert bodyIMC.isPresent() : "ASSERT FAILED";

		var labelBefore = new LabelStmt(Label.nextAnonymous());
		var labelInside = new LabelStmt(Label.nextAnonymous());
		var labelAfter = new LabelStmt(Label.nextAnonymous());

		var condCheckJump = new CJumpStmt((IRExpr)conditionIMC.get(),
								  labelInside.label,
								  labelAfter.label);

		var jumpBackToCondCheck = new JumpStmt(labelBefore.label);

		List<IRStmt> stmts = new ArrayList<>();
		stmts.add(labelBefore);

		if( conditionIMC.get() instanceof IRStmt ) {
			stmts.add((IRStmt)conditionIMC.get());
		}
		else if( conditionIMC.get() instanceof IRExpr ) {
			stmts.add(new ExpStmt((IRExpr)conditionIMC.get()));
		}
		else {
			assert false : String.format("ASSERT FAILED: While condition is not an IRStmt or IRExpr\n");
		}

		stmts.add(condCheckJump);
		stmts.add(labelInside);
		stmts.add(new ExpStmt((IRExpr)bodyIMC.get()));
		stmts.add(jumpBackToCondCheck);
		stmts.add(labelAfter);

		var whileStmts = new SeqStmt(stmts);
		imcCode.store(whileStmts, whileLoop);
    }

    @Override
    public void visit(Where where) {
		where.defs.accept(this);
		where.expr.accept(this);

		var bodyExpr = imcCode.valueFor(where.expr);

		assert bodyExpr.isPresent() : String.format("ASSERT FAILED: No IMC found for body of where\n");

		imcCode.store(bodyExpr.get(), where);
    }

    @Override
    public void visit(Defs defs) {
		defs.definitions.stream().forEach(def -> {
				def.accept(this);
			});
    }

    @Override
    public void visit(FunDef funDef) {
		var funDefFrame = frames.valueFor(funDef);

		assert funDefFrame.isPresent(): String.format("ASSERT FAILED: No frame found for function definition `%s`\n",
													  funDef.name);

		this.currentStaticLevel += 1;
		funDef.parameters.stream().forEach(param -> {
				param.accept(this);
			});
		funDef.body.accept(this);
		funDef.type.accept(this);
		this.currentStaticLevel -= 1;

		var funBodyIMC = imcCode.valueFor(funDef.body);

		assert funBodyIMC.isPresent(): String.format("ASSERT FAILED: No code found for body of founction `%s`\n",
			funDef.name);

		assert (funBodyIMC.get() instanceof IRExpr): String.format("ASSERT FAILED: Body of function `%s` is not an IRExpr.\n", funDef.name);

		// var funBodyExprToStmt = new ExpStmt((IRExpr)funBodyIMC.get());

		// var saveResultToRV = new MoveStmt(new MemExpr(NameExpr.FP()), (IRExpr)funBodyIMC.get());


		// List<IRStmt> functionStmts = new ArrayList<>();

		// functionStmts.add(funBodyExprToStmt);
		// functionStmts.add(saveResultToRV);
		
		// var functionSeqStmt = new SeqStmt(functionStmts);

		var saveResultToRV = new MoveStmt(new MemExpr(NameExpr.FP()),
										  (IRExpr)funBodyIMC.get());

		var funDefIMC = new Chunk.CodeChunk(funDefFrame.get(), saveResultToRV);
		chunks.add(funDefIMC);
    }

    @Override
    public void visit(TypeDef typeDef) {
		typeDef.type.accept(this);
		return;
    }

    @Override
    public void visit(VarDef varDef) {
		varDef.type.accept(this);
		var varDefAccess = accesses.valueFor(varDef);

		assert (varDefAccess.isPresent()): String.format("ASSERT FAILED: No access found for variable `%s`\n",
													   varDef.name);

		if( varDefAccess.get() instanceof Access.Global ) {
			var varDefIMC = new Chunk.GlobalChunk((Access.Global)varDefAccess.get());
			chunks.add(varDefIMC);
		}

    }

    @Override
    public void visit(Parameter parameter) {
		parameter.type.accept(this);
		return;
    }

    @Override
    public void visit(Array array) {
		array.type.accept(this);
		return;
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

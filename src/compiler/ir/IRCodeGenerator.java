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

    @Override
    public void visit(Call call) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
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
			// TODO: FIXME: here we should probably go into visitor for array, but not sure how
			var arrType = types.valueFor(binary.left);

			assert arrType.isPresent() : String.format("ASSERT FAILED: No type found for binary expresion of array\n");

			var arr = arrType.get().asArray();

			assert arr.isPresent() : String.format("ASSERT FAILED: Binary expression is not array but using array operator\n");

			var elementSize = arr.get().type.sizeInBytes();

			var indexOffset = new BinopExpr((IRExpr)rightIMC.get(),
											(IRExpr)(new ConstantExpr(elementSize)),
											BinopExpr.Operator.MUL);

			var goToIndex = new BinopExpr((IRExpr)leftIMC.get(),
										  (IRExpr)indexOffset,
										  BinopExpr.Operator.ADD);

			var memAccess = new MemExpr(goToIndex);
			imcCode.store(memAccess, binary);
			return;
		}
		else if( binary.operator == Operator.ASSIGN ) {
			var memExpr = new MemExpr((IRExpr)leftIMC.get());
			var moveExpr = new MoveStmt(memExpr, (IRExpr) rightIMC.get());

			imcCode.store(moveExpr, binary);
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

		var counterExprStmt = new ExpStmt((IRExpr)counterIMC.get());
		var lowExprStmt = new ExpStmt((IRExpr)lowIMC.get());
		var highExprStmt = new ExpStmt((IRExpr)highIMC.get());
		var stepExprStmt = new ExpStmt((IRExpr)stepIMC.get());
		var bodyExprStmt = new ExpStmt((IRExpr)bodyIMC.get());

		var labelBefore = new LabelStmt(Label.nextAnonymous());
		var labelInside = new LabelStmt(Label.nextAnonymous());
		var labelAfter = new LabelStmt(Label.nextAnonymous());

		List<IRStmt> stmts = new ArrayList<>();
		stmts.add(labelBefore);
		// CJUMP
		stmts.add(labelInside);
		stmts.add(counterExprStmt);
		stmts.add(lowExprStmt);
		stmts.add(highExprStmt);
		stmts.add(stepExprStmt);
		stmts.add(bodyExprStmt);
		// Jump
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
			imcCode.store(nameIMC, name);
			return;
		}

		if( nameAccess.get() instanceof Access.Stack nameAccessLocal ) {
			int deltaSL = Math.abs(nameAccessLocal.staticLevel
								   - this.currentStaticLevel);


			IRExpr FPIMC = NameExpr.FP();

			for(int i = 0; i < deltaSL; ++i) {
				FPIMC = new MemExpr(FPIMC);
			}

			var binOp = new BinopExpr(FPIMC,
									  new ConstantExpr(nameAccessLocal.offset),
									  BinopExpr.Operator.ADD);

			var localRead = new MemExpr(binOp);

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
		stmts.add(new CJumpStmt((IRExpr)condIMC.get(),
								thenLabel.label,
								elseLabel.label));

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


		// JUMP END
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
				// TODO
				throw new UnsupportedOperationException("Unimplemented method literal for STR");
				// break;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
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

		/*
		// Should the condition code be included also
		if( bodyIMC.get() instanceof IRStmt ) {
			stmts.add((IRStmt)bodyIMC.get());
		}
		else if( bodyIMC.get() instanceof IRExpr ) {
			stmts.add(new ExpStmt((IRExpr)bodyIMC.get()));
		}
		else {
			assert false : "Body of while is not Stmt or Expr";
		}
		*/

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
		funDef.body.accept(this);
		this.currentStaticLevel -= 1;

		var funBodyIMC = imcCode.valueFor(funDef.body);

		assert funBodyIMC.isPresent(): String.format("ASSERT FAILED: No code found for body of founction `%s`\n",
			funDef.name);

		assert (funBodyIMC.get() instanceof IRExpr): String.format("ASSERT FAILED: Body of function `%s` is not an IRExpr.\n", funDef.name);

		var funBodyExprToStmt = new ExpStmt((IRExpr)funBodyIMC.get());

		var funDefIMC = new Chunk.CodeChunk(funDefFrame.get(), (IRStmt)funBodyExprToStmt);
		chunks.add(funDefIMC);
    }

    @Override
    public void visit(TypeDef typeDef) {
		return;
    }

    @Override
    public void visit(VarDef varDef) {
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Array array) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Atom atom) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(TypeName name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }
}

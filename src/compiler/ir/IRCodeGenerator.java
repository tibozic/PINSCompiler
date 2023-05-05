/**
 * @ Author: turk
 * @ Description: Generator vmesne kode.
 */

package compiler.ir;

import static common.RequireNonNull.requireNonNull;

import java.util.ArrayList;
import java.util.List;

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


		var binaryIMC = new BinopExpr((IRExpr)leftIMC.get(),
									  (IRExpr)rightIMC.get(),
									  BinopExpr.convertOperator(binary.operator));

		imcCode.store(binaryIMC, binary);
    }

    @Override
    public void visit(Block block) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(For forLoop) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
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

		if( nameAccess.get() instanceof Access.Local nameAccessLocal ) {
			int deltaSL = Math.abs(nameAccessLocal.staticLevel
								   - this.currentStaticLevel);



			IRExpr FPIMC = NameExpr.FP();

			for(int i = 0; i < deltaSL; ++i) {
				FPIMC = new MemExpr(FPIMC);
			}

			var binOp = new BinopExpr(FPIMC,
									  new ConstantExpr(nameAccessLocal.offset),
									  BinopExpr.Operator.ADD);
		}
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(Where where) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
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

		assert funBodyIMC.get() instanceof IRStmt: String.format("ASSERT FAILED: Body of function `%s` is not an expression.\n", funDef.name);

		var funDefIMC = new Chunk.CodeChunk(funDefFrame.get(), (IRStmt)funBodyIMC.get());
		chunks.add(funDefIMC);
    }

    @Override
    public void visit(TypeDef typeDef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public void visit(VarDef varDef) {
		accesses.printStorage();

		var varDefAccess = accesses.valueFor(varDef);
		System.out.println(varDefAccess.isPresent());

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

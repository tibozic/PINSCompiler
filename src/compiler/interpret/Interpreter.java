/**
 * @ Author: turk
 * @ Description: Navidezni stroj (intepreter).
 */

package compiler.interpret;

import static common.RequireNonNull.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Random;

import common.Constants;
import compiler.frm.Frame;
import compiler.gen.Memory;
import compiler.ir.chunk.Chunk.CodeChunk;
import compiler.ir.code.IRNode;
import compiler.ir.code.expr.*;
import compiler.ir.code.stmt.*;
import compiler.ir.IRPrettyPrint;

public class Interpreter {
    /**
     * Pomnilnik navideznega stroja.
     */
    private Memory memory;
    
    /**
     * Izhodni tok, kamor izpisujemo rezultate izvajanja programa.
     * 
     * V primeru, da rezultatov ne želimo izpisovati, nastavimo na `Optional.empty()`.
     */
    private Optional<PrintStream> outputStream;

    /**
     * Generator naključnih števil.
     */
    private Random random;

    /**
     * Skladovni kazalec (kaže na dno sklada).
     */
    private int stackPointer;

    /**
     * Klicni kazalec (kaže na vrh aktivnega klicnega zapisa).
     */
    private int framePointer;

    public Interpreter(Memory memory, Optional<PrintStream> outputStream) {
        requireNonNull(memory, outputStream);
        this.memory = memory;
        this.outputStream = outputStream;
        this.stackPointer = memory.size - Constants.WordSize;
        this.framePointer = memory.size - Constants.WordSize;
    }

    // --------- izvajanje navideznega stroja ----------

    public void interpret(CodeChunk chunk) {
        memory.stM(framePointer + Constants.WordSize, 0); // argument v funkcijo main
        memory.stM(framePointer - chunk.frame.oldFPOffset(), framePointer); // oldFP
        internalInterpret(chunk);
    }

    private void internalInterpret(CodeChunk chunk) {
        // @TODO: Nastavi FP in SP na nove vrednosti!
		int old_fp_address_in_new_function = this.framePointer - chunk.frame.oldFPOffset();
		memory.stM(old_fp_address_in_new_function, this.framePointer);
		this.framePointer = this.stackPointer;
		this.stackPointer = this.framePointer - chunk.frame.size();
        
        Object result = null;
        if (chunk.code instanceof SeqStmt seq) {
            for (int pc = 0; pc < seq.statements.size(); pc++) {
                var stmt = seq.statements.get(pc);
                result = execute(stmt);
                if (result instanceof Frame.Label label) {
                    for (int q = 0; q < seq.statements.size(); q++) {
                        if (seq.statements.get(q) instanceof LabelStmt labelStmt && labelStmt.label.equals(label)) {
                            pc = q;
                            break;
                        }
                    }
                }
            }
        } else {
            throw new RuntimeException("Linearize code!");
        }

        // @TODO: Ponastavi FP in SP na stare vrednosti!
		this.stackPointer = this.framePointer;
		this.framePointer = toInt(memory.ldM(this.framePointer
											 - chunk.frame.oldFPOffset()));
    }

    private Object execute(IRStmt stmt) {
        if (stmt instanceof CJumpStmt cjump) {
            return execute(cjump);
        } else if (stmt instanceof ExpStmt exp) {
            return execute(exp);
        } else if (stmt instanceof JumpStmt jump) {
            return execute(jump);
        } else if (stmt instanceof LabelStmt label) {
            return null;
        } else if (stmt instanceof MoveStmt move) {
            return execute(move);
        } else {
            throw new RuntimeException("Cannot execute this statement!");
        }
    }

    private Object execute(CJumpStmt cjump) {
        var cond = execute(cjump.condition);

		if( toInt(cond) > 0 ) {
			return cjump.thenLabel;
		}
		else {
			return cjump.elseLabel;
		}
    }

    private Object execute(ExpStmt exp) {
        return execute(exp.expr);
    }

    private Object execute(JumpStmt jump) {
        return jump.label;
    }

    private Object execute(MoveStmt move) {
		memory_write(move);
		return null;
    }

    private Object execute(IRExpr expr) {
        if (expr instanceof BinopExpr binopExpr) {
            return execute(binopExpr);
        } else if (expr instanceof CallExpr callExpr) {
            return execute(callExpr);
        } else if (expr instanceof ConstantExpr constantExpr) {
            return execute(constantExpr);
        } else if (expr instanceof EseqExpr eseqExpr) {
            throw new RuntimeException("Cannot execute ESEQ; linearize code!");
        } else if (expr instanceof MemExpr memExpr) {
            // return execute(memExpr);
			return memory_read(memExpr);
        } else if (expr instanceof NameExpr nameExpr) {
			// return variable_read(nameExpr);
            return execute(nameExpr);
        } else if (expr instanceof TempExpr tempExpr) {
            // return execute(tempExpr);
			return temp_read(tempExpr);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

	private Object execute(BinopExpr binop) {
		var left1 = execute(binop.lhs);
		var right1 = execute(binop.rhs);

		Object left;
		Object right;

		if( binop.lhs instanceof MemExpr
			|| binop.lhs instanceof NameExpr
			|| binop.lhs instanceof TempExpr )
		{
			left = general_read(binop.lhs);
		}
		else {
			left = left1;
		}

		if( binop.rhs instanceof MemExpr
			|| binop.rhs instanceof NameExpr
			|| binop.rhs instanceof TempExpr )
		{
			right = general_read(binop.rhs);
		}
		else {
			right = right1;
		}


		switch( binop.op ) {
			case ADD: {
				return toInt(left) + toInt(right);
			}
			case SUB: {
				return toInt(left) - toInt(right);
			}
			case MUL: {
				return toInt(left) * toInt(right);
			}
			case DIV: {
				return toInt(left) / toInt(right);
			}
			case MOD: {
				return toInt(left) % toInt(right);
			}
			case AND: {
				return toBool(left) && toBool(right);
			}
			case OR: {
				return toBool(left) || toBool(right);
			}
			case EQ: {
				return toInt(left) == toInt(right);
			}
			case NEQ: {
				return toInt(left) !=  toInt(right);
			}
			case GT: {
				return toInt(left) > toInt(right);
			}
			case LT: {
				return toInt(left) < toInt(right);
			}
			case GEQ: {
				return toInt(left) >= toInt(right);
			}
			case LEQ: {
				return toInt(left) <= toInt(right);
			}
			default: {
				throw new IllegalArgumentException("Unknown operandd in binary expression");
			}
		}
    }

	private Object execute(CallExpr call) {
        if (call.label.name.equals(Constants.printIntLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var arg = execute(call.args.get(1));
            outputStream.ifPresent(stream -> stream.println(arg));
            return 0;
        } else if (call.label.name.equals(Constants.printStringLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            // var address = execute(call.args.get(1));
            var address = call.args.get(1);
            // var res = memory.ldM(toInt(memory.address(((TempExpr)address).temp.)));
			// var res = memory.ldT(((TempExpr)address).temp);
			var addr_of_string = general_read(address);
			var res = memory.ldM(toInt(addr_of_string));
            outputStream.ifPresent(stream -> stream.println("\""+res+"\""));
            return 0;
        } else if (call.label.name.equals(Constants.printLogLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var arg = execute(call.args.get(1));
            outputStream.ifPresent(stream -> stream.println(toBool(arg)));
            return 0;
        } else if (call.label.name.equals(Constants.randIntLabel)) {
            if (call.args.size() != 3) { throw new RuntimeException("Invalid argument count!"); }
            var min = toInt(execute(call.args.get(1)));
            var max = toInt(execute(call.args.get(2)));
            return random.nextInt(min, max);
        } else if (call.label.name.equals(Constants.seedLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var seed = toInt(execute(call.args.get(1)));
            random = new Random(seed);
            return 0;
        } else if (memory.ldM(call.label) instanceof CodeChunk chunk) {
            internalInterpret(chunk);
			return memory.ldM(this.stackPointer);
        } else {
            throw new RuntimeException("Only functions can be called!");
        }
    }

	/**
	   Vrne vrednost konstante
	 */
    private Object execute(ConstantExpr constant) {
        return constant.constant;
    }

	/**
	   Vrne naslov na katerega se nanasa MemExpr
	 */
    private Object execute(MemExpr mem) {
        return execute(mem.expr);
    }

	/**
	   Vrne naslov spremenljivke
	 */
    private Object execute(NameExpr name) {
		if( name.label.name.equals(Constants.framePointer)  ) {
			return this.framePointer;
		}
		if( name.label.name.equals(Constants.stackPointer)  ) {
			return this.stackPointer;
		}

		var addr = memory.address(name.label);

		assert(addr.isPresent()): "No addres found for this label.";

		return addr.get();
    }
	
	/**
	   Vrne vrednost, ki se nahaja v zacasni spremenljivki
	 */
    private Object execute(TempExpr temp) {
		return memory.ldT(temp.temp);
    }

    // ----------- pomožne funkcije -----------

    private int toInt(Object obj) {
        if (obj instanceof Integer integer) {
            return integer;
        }
        throw new IllegalArgumentException("Could not convert obj to integer!");
    }

    private boolean toBool(Object obj) {
        return toInt(obj) == 0 ? false : true;
    }

    private int toInt(boolean bool) {
        return bool ? 1 : 0;
    }

    private String prettyDescription(IRNode ir, int indent) {
        var os = new ByteArrayOutputStream();
        var ps = new PrintStream(os);
        new IRPrettyPrint(ps, indent).print(ir);
        return os.toString(Charset.defaultCharset());
    }

    private String prettyDescription(IRNode ir) {
        return prettyDescription(ir, 2);
    }

    private void prettyPrint(IRNode ir, int indent) {
        System.out.println(prettyDescription(ir, indent));
    }

    private void prettyPrint(IRNode ir) {
        System.out.println(prettyDescription(ir));
    }


	/**
	   Iz pomnilnika prebere vrednost in jo vrne
	 */
    private Object memory_read(MemExpr mem) {
        return memory.ldM(toInt(execute(mem.expr)));
    }

	/**
	   Izvede MoveStmt kot pisanje v pomnilnik
	 */
    private void memory_write(MoveStmt move) {

		assert (move.dst instanceof MemExpr || move.dst instanceof TempExpr)
				: "ASSERT ERROR: Can only write to memory if destination of MoveStmt is MemExpr or TempExpr";

		var src = execute(move.src);

		if( move.dst instanceof MemExpr memExpr ) {
			// var dst = execute(move.dst);
            var dst = execute(memExpr.expr);
			if( memExpr.expr instanceof NameExpr nameExpr )
				variable_write(nameExpr, src);
			else {
				var addr = toInt(dst);
				memory.stM(addr, src);
			}
		}
		else if( move.dst instanceof TempExpr tempExpr ) {
			memory.stT(tempExpr.temp, src);
		}
    }

	/**
	   Iz spremenljivke prebere vrednost in jo vrne
	   Ce je spremenljivka SP ali FP vrne njuno vrednost
	 */
	private Object variable_read(NameExpr name) {
		if( name.label.name.equals(Constants.framePointer)  ) {
			return this.framePointer;
		}
		if( name.label.name.equals(Constants.stackPointer)  ) {
			return this.stackPointer;
		}
	
		return memory.ldM(toInt(execute(name)));
	}

	/**
	   Na naslov spremenljivke zapise vrednost
	 */
	private void variable_write(NameExpr name, Object value) {

		if( name.label.name.equals(Constants.framePointer) )
		{
			memory.stM(this.framePointer, value);
			return;
		}

		if( name.label.name.equals(Constants.stackPointer) ) {
			memory.stM(this.stackPointer, value);
			return;
		}

		var address = memory.address(name.label);

		// TODO: FIXME: How to get the offset
		int offset = 0;

		if( address.isEmpty() )
			memory.registerLabel(name.label,
								 offset);

		memory.stM(name.label, value);
	}

	/**
	   Prebere vrednost iz zacasne spremenljivke
	 */
	private Object temp_read(TempExpr temp) {
		return memory.ldT(temp.temp);
	}

	/**
	   V zacasno spremenljivko shrani vrednost
	 */
	private void temp_write(TempExpr temp, Object value) {
		memory.stT(temp.temp, value);
	}

	/**
	   Iz katerekoli lokacije v pomnilniku (zacasna spremenljivka,
	   naslov, labela) prebere vrednost
	 */
	private Object general_read(IRExpr location) {
		if( location instanceof MemExpr memExpr )
			return memory_read(memExpr);
		if( location instanceof TempExpr tempExpr )
			return temp_read(tempExpr);
		if( location instanceof NameExpr nameExpr )
			return variable_read(nameExpr);

		assert false : "ASSERT FAILED: Can't read from this location";
		throw new RuntimeException("Can't read from this location (in general_read)");
	}
}

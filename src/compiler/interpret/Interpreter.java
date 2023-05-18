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
import java.util.Map;
import java.util.HashMap;

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
        internalInterpret(chunk, new HashMap<>());
    }

    private void internalInterpret(CodeChunk chunk, Map<Frame.Temp, Object> temps) {
        // @TODO: Nastavi FP in SP na nove vrednosti!
		int old_fp_address_in_new_function = this.framePointer - chunk.frame.oldFPOffset();
		memory.stM(old_fp_address_in_new_function, this.framePointer);
		this.framePointer = this.stackPointer;
		this.stackPointer = this.framePointer - chunk.frame.size();
        
        Object result = null;
        if (chunk.code instanceof SeqStmt seq) {
            for (int pc = 0; pc < seq.statements.size(); pc++) {
                var stmt = seq.statements.get(pc);
                result = execute(stmt, temps);
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

    private Object execute(IRStmt stmt, Map<Frame.Temp, Object> temps) {
        if (stmt instanceof CJumpStmt cjump) {
            return execute(cjump, temps);
        } else if (stmt instanceof ExpStmt exp) {
            return execute(exp, temps);
        } else if (stmt instanceof JumpStmt jump) {
            return execute(jump, temps);
        } else if (stmt instanceof LabelStmt label) {
            return null;
        } else if (stmt instanceof MoveStmt move) {
            return execute(move, temps);
        } else {
            throw new RuntimeException("Cannot execute this statement!");
        }
    }

    private Object execute(CJumpStmt cjump, Map<Frame.Temp, Object> temps) {
        var cond = execute(cjump.condition, temps);

		if( (boolean)cond ) {
			return cjump.thenLabel;
		}
		else {
			return cjump.elseLabel;
		}
    }

    private Object execute(ExpStmt exp, Map<Frame.Temp, Object> temps) {
        return execute(exp.expr, temps);
    }

    private Object execute(JumpStmt jump, Map<Frame.Temp, Object> temps) {
        return jump.label;
    }

    private Object execute(MoveStmt move, Map<Frame.Temp, Object> temps) {
		memory_write(move, temps);
		return null;
    }

    private Object execute(IRExpr expr, Map<Frame.Temp, Object> temps) {
        if (expr instanceof BinopExpr binopExpr) {
            return execute(binopExpr, temps);
        } else if (expr instanceof CallExpr callExpr) {
            return execute(callExpr, temps);
        } else if (expr instanceof ConstantExpr constantExpr) {
            return execute(constantExpr);
        } else if (expr instanceof EseqExpr eseqExpr) {
            throw new RuntimeException("Cannot execute ESEQ; linearize code!");
        } else if (expr instanceof MemExpr memExpr) {
			// return memory_read(memExpr);
            return execute(memExpr, temps);
        } else if (expr instanceof NameExpr nameExpr) {
			// return variable_read(nameExpr);
            return execute(nameExpr);
        } else if (expr instanceof TempExpr tempExpr) {
			// return temp_read(tempExpr);
            return execute(tempExpr, temps);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

	private Object execute(BinopExpr binop, Map<Frame.Temp, Object> temps) {
		var left1 = execute(binop.lhs, temps);
		var right1 = execute(binop.rhs, temps);

		Object left;
		Object right;

		if( binop.lhs instanceof MemExpr
			|| binop.lhs instanceof NameExpr
			|| binop.lhs instanceof TempExpr )
		{
			left = general_read(binop.lhs, temps);
		}
		else {
			left = left1;
		}

		if( binop.rhs instanceof MemExpr
			|| binop.rhs instanceof NameExpr
			|| binop.rhs instanceof TempExpr )
		{
			right = general_read(binop.rhs, temps);
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

	private Object execute(CallExpr call, Map<Frame.Temp, Object> temps) {
        if (call.label.name.equals(Constants.printIntLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var arg = execute(call.args.get(1), temps);

			/*
			if( call.args.get(1) instanceof TempExpr ) {
				// This function was passed a variable
				var res = memory.ldM(toInt(arg));
				outputStream.ifPresent(stream -> stream.println(res));
				return 0;
			}
			*/

			outputStream.ifPresent(stream -> stream.println(arg));
            return 0;
        } else if (call.label.name.equals(Constants.printStringLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            // var address = execute(call.args.get(1));
            var address = call.args.get(1);
            // var res = memory.ldM(toInt(memory.address(((TempExpr)address).temp.)));
			// var res = memory.ldT(((TempExpr)address).temp);

			// var addr_of_string = general_read(address, temps);
			// var res = memory.ldM(toInt(addr_of_string));

			var res = general_read(address, temps);

            outputStream.ifPresent(stream -> stream.println("\""+res+"\""));
            return 0;
        } else if (call.label.name.equals(Constants.printLogLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var arg = execute(call.args.get(1), temps);
            outputStream.ifPresent(stream -> stream.println(toBool(arg)));
            return 0;
        } else if (call.label.name.equals(Constants.randIntLabel)) {
            if (call.args.size() != 3) { throw new RuntimeException("Invalid argument count!"); }
            var min = toInt(execute(call.args.get(1), temps));
            var max = toInt(execute(call.args.get(2), temps));
            return random.nextInt(min, max);
        } else if (call.label.name.equals(Constants.seedLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var seed = toInt(execute(call.args.get(1), temps));
            random = new Random(seed);
            return 0;
        } else if (memory.ldM(call.label) instanceof CodeChunk chunk) {
			for (int i = 0; i < call.args.size(); ++i) {
				var value = execute(call.args.get(i), temps);
				memory.stM((this.stackPointer + 4*(i)), value);
			}
            internalInterpret(chunk, new HashMap<>());
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
	   Prebere vrednost, ki se nahaja na naslovu na katerega se nanasa MemExpr
	 */
    private Object execute(MemExpr mem, Map<Frame.Temp, Object> temps) {
		if( mem.expr instanceof NameExpr nameExpr ) {
			if( nameExpr.label.name.equals(Constants.framePointer) ) {
				return this.framePointer;
			}
			if( nameExpr.label.name.equals(Constants.stackPointer) ) {
				return this.stackPointer;
			}
		}
        return memory.ldM(toInt(execute(mem.expr, temps)));
    }

	/**
	   Vrne naslov na katerega se nanasa MemExpr
	   (Namenjeno da se uporablja pri pisanju (ko je MemExpr na levi strani MoveStmt))
	 */
    private Object get_addr(MemExpr mem, Map<Frame.Temp, Object> temps) {
		if( mem.expr instanceof NameExpr nameExpr ) {
			if( nameExpr.label.name.equals(Constants.framePointer) ) {
				return this.framePointer;
			}
			if( nameExpr.label.name.equals(Constants.stackPointer) ) {
				return this.stackPointer;
			}
		}
        return execute(mem.expr, temps);
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
    private Object execute(TempExpr temp, Map<Frame.Temp, Object> temps) {
		return memory.ldT(temps, temp.temp);
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
    private Object memory_read(MemExpr mem, Map<Frame.Temp, Object> temps) {
        // return memory.ldM(toInt(execute(mem.expr, temps)));
		// return memory.ldM(toInt(execute(mem.expr, temps)));
		return execute(mem, temps);
    }

	/**
	   Izvede MoveStmt kot pisanje v pomnilnik
	 */
    private void memory_write(MoveStmt move, Map<Frame.Temp, Object> temps) {

		assert (move.dst instanceof MemExpr || move.dst instanceof TempExpr)
				: "ASSERT ERROR: Can only write to memory if destination of MoveStmt is MemExpr or TempExpr";

		var src = execute(move.src, temps);

		Object dst;
		if( move.dst instanceof MemExpr memExpr ) {
			// var dst = execute(move.dst, temps);
			if( memExpr.expr instanceof MemExpr ) {
				dst = get_addr((MemExpr)memExpr.expr, temps);
			}
			else {
				dst = execute(memExpr.expr, temps);
			}

			if( memExpr.expr instanceof NameExpr nameExpr )
				variable_write(nameExpr, src);
			else {
				var addr = toInt(dst);
				memory.stM(addr, src);
			}
		}
		else if( move.dst instanceof TempExpr tempExpr ) {
			memory.stT(temps, tempExpr.temp, src);
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
	private Object temp_read(TempExpr temp, Map<Frame.Temp, Object> temps) {
		return memory.ldT(temps, temp.temp);
	}

	/**
	   V zacasno spremenljivko shrani vrednost
	 */
	private void temp_write(Map<Frame.Temp, Object> temps, TempExpr temp, Object value) {
		memory.stT(temps, temp.temp, value);
	}

	/**
	   Iz katerekoli lokacije v pomnilniku (zacasna spremenljivka,
	   naslov, labela) prebere vrednost
	 */
	private Object general_read(IRExpr location, Map<Frame.Temp, Object> temps) {
		if( location instanceof MemExpr memExpr )
			return memory_read(memExpr, temps);
		if( location instanceof TempExpr tempExpr )
			return temp_read(tempExpr, temps);
		if( location instanceof NameExpr nameExpr )
			return variable_read(nameExpr);

		assert false : "ASSERT FAILED: Can't read from this location";
		throw new RuntimeException("Can't read from this location (in general_read)");
	}
}

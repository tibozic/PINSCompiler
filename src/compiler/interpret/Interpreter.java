/**
 * @ Author: turk
 * @ Description: Navidezni stroj (intepreter).
 */

package compiler.interpret;

import static common.RequireNonNull.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
        memory.stM(framePointer + Constants.WordSize, 999); // argument v funkcijo main
        memory.stM(framePointer - chunk.frame.oldFPOffset(), framePointer); // oldFP
        internalInterpret(chunk, new HashMap<>());
    }

    private void internalInterpret(CodeChunk chunk, Map<Frame.Temp, Object> temps) {
        // @TODO: Nastavi FP in SP na nove vrednosti!
		int old_fp_address_in_new_function = this.stackPointer - chunk.frame.oldFPOffset();
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
            throw new RuntimeException("Linearize IR!");
        }
      
        // @TODO: Ponastavi FP in SP na stare vrednosti!
		var old_fp_address = this.framePointer - chunk.frame.oldFPOffset();
		this.stackPointer = this.framePointer;
		this.framePointer = toInt(memory.ldM(old_fp_address));
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

		// if( (boolean)cond ) {
		if( toBool(cond) ) {
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
		var src = execute(move.src, temps);

        if( move.dst instanceof MemExpr memExpr) {
			// FIXME: possibly have to check if the inside expr is also memory and in that case
			// dont execute it (check github)
			if( memExpr.expr instanceof MemExpr nestedMemExpr ) {
				var addr = execute(nestedMemExpr.expr, temps);
				memory.stM(toInt(addr), src);
			}
			else if( memExpr.expr instanceof NameExpr nameExpr ) {
				int addr;
				if( nameExpr.label.name.equals(Constants.framePointer)  ) {
					addr = this.framePointer;
				}
				else if( nameExpr.label.name.equals(Constants.stackPointer)  ) {
					addr = this.stackPointer;
				}
				else {
					addr = memory.address(nameExpr.label).get();
				}

				memory.stM(addr, src);
			}
			else if( memExpr.expr instanceof TempExpr tempExpr ) {
				memory.stT(temps, tempExpr.temp, src);
			}
			else {
				var addr = execute(memExpr.expr, temps);
				memory.stM(toInt(addr), src);
			}
			/*
			var addr = execute(memExpr.expr, temps);
			memory.stM(toInt(addr), src);
			*/
		}
		else if( move.dst instanceof TempExpr tempExpr) {
			memory.stT(temps, tempExpr.temp, src);
		}
		else {
            throw new IllegalArgumentException("ERROR: Left side of move expression has to be MemExpr or TempExpr");
		}

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
            throw new RuntimeException("Cannot execute ESEQ; linearize IRCode!");
        } else if (expr instanceof MemExpr memExpr) {
            return execute(memExpr, temps);
        } else if (expr instanceof NameExpr nameExpr) {
            return execute(nameExpr);
        } else if (expr instanceof TempExpr tempExpr) {
            return execute(tempExpr, temps);
        } else {
            throw new IllegalArgumentException("Unknown expr type");
        }
    }

    private Object execute(BinopExpr binop, Map<Frame.Temp, Object> temps) {
		var left = execute(binop.lhs, temps);
		var right = execute(binop.rhs, temps);


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
				// return toBool(left) && toBool(right);
				return (toBool(left) && toBool(right)) ? 1 : 0;
			}
			case OR: {
				// return toBool(left) || toBool(right);
				return (toBool(left) || toBool(right)) ? 1 : 0;
			}
			case EQ: {
				// return toInt(left) == toInt(right);
				return (toInt(left) == toInt(right)) ? 1 : 0;
			}
			case NEQ: {
				// return toInt(left) !=  toInt(right);
				return (toInt(left) != toInt(right)) ? 1 : 0;
			}
			case GT: {
				// return toInt(left) > toInt(right);
				return (toInt(left) > toInt(right)) ? 1 : 0;
			}
			case LT: {
				// return toInt(left) < toInt(right);
				return (toInt(left) < toInt(right)) ? 1 : 0;
			}
			case GEQ: {
				// return toInt(left) >= toInt(right);
				return (toInt(left) >= toInt(right)) ? 1 : 0;
			}
			case LEQ: {
				// return toInt(left) <= toInt(right);
				return (toInt(left) <= toInt(right)) ? 1 : 0;
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
            outputStream.ifPresent(stream -> stream.println(arg));
            return arg;
        } else if (call.label.name.equals(Constants.printStringLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var address = execute(call.args.get(1), temps);
            // var res = memory.ldM(toInt(address));
			var res = address;
            outputStream.ifPresent(stream -> stream.println("\""+res+"\""));
            return res;
        } else if (call.label.name.equals(Constants.printLogLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var arg = execute(call.args.get(1), temps);
            outputStream.ifPresent(stream -> stream.println(toBool(arg)));
            return arg;
        } else if (call.label.name.equals(Constants.randIntLabel)) {
            if (call.args.size() != 3) { throw new RuntimeException("Invalid argument count!"); }
            var min = toInt(execute(call.args.get(1), temps));
            var max = toInt(execute(call.args.get(2), temps));
            return random.nextInt(min, max);
        } else if (call.label.name.equals(Constants.seedLabel)) {
            if (call.args.size() != 2) { throw new RuntimeException("Invalid argument count!"); }
            var seed = toInt(execute(call.args.get(1), temps));
            random = new Random(seed);
            return seed;
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

    private Object execute(ConstantExpr constant) {
		return constant.constant;
    }

    private Object execute(MemExpr mem, Map<Frame.Temp, Object> temps) {
		if( mem.expr instanceof NameExpr nameExpr && nameExpr.label.name.equals(Constants.framePointer))
			return this.framePointer;

		if( mem.expr instanceof NameExpr nameExpr && nameExpr.label.name.equals(Constants.stackPointer))
			return this.stackPointer;

		/*
		if( mem.expr instanceof MemExpr )
			return memory.ldM(toInt(execute(mem.expr, temps)));
		else {
			return execute(mem.expr, temps);
		}
		*/

		if( mem.expr instanceof NameExpr nameExpr ) {
			return execute(nameExpr);
		}

		var value = execute(mem.expr, temps);
		if( value instanceof String ) {
			return value;
		}
		else {
			return memory.ldM(toInt(value));
		}
	}

    private Object execute(NameExpr name) {
		if( name.label.name.equals(Constants.framePointer) )
			return this.framePointer;

		if( name.label.name.equals(Constants.stackPointer) )
			return this.stackPointer;

		return memory.ldM(name.label);
		// return memory.address(name.label);
    }

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
}

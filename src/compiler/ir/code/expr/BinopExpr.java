/**
 * @ Author: turk
 * @ Description: Binarni izraz.
 */

package compiler.ir.code.expr;

import static common.RequireNonNull.requireNonNull;
import compiler.parser.ast.expr.Binary;

public class BinopExpr extends IRExpr {
    /**
     * Levi operand.
     */
    public final IRExpr lhs;

    /**
     * Desni operand.
     */
    public final IRExpr rhs;

    /**
     * Operator.
     */
    public final Operator op;

    public BinopExpr(IRExpr lhs, IRExpr rhs, Operator op) {
        requireNonNull(lhs, rhs, op);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

	public static BinopExpr.Operator convertOperator(Binary.Operator op) {
		switch(op) {
			case ADD: {
				return BinopExpr.Operator.ADD;
			}
			case SUB: {
				return BinopExpr.Operator.SUB;
			}
			case MUL: {
				return BinopExpr.Operator.MUL;
			}
			case DIV: {
				return BinopExpr.Operator.DIV;
			}
			case MOD: {
				return BinopExpr.Operator.MOD;
			}
			case AND: {
				return BinopExpr.Operator.AND;
			}
			case OR: {
				return BinopExpr.Operator.OR;
			}
			case EQ: {
				return BinopExpr.Operator.EQ;
			}
			case NEQ: {
				return BinopExpr.Operator.NEQ;
			}
			case LT: {
				return BinopExpr.Operator.LT;
			}
			case GT: {
				return BinopExpr.Operator.GT;
			}
			case LEQ: {
				return BinopExpr.Operator.LEQ;
			}
			case GEQ: {
				return BinopExpr.Operator.GEQ;
			}
			default: {
				assert false : "Unknown operator in binary expr";
				System.exit(99);
				return BinopExpr.Operator.ADD;
			}
		}
	} 

    public static enum Operator {
        ADD, SUB, MUL, DIV, MOD, // aritmetični
        AND, OR, // logični
        EQ, NEQ, LT, GT, LEQ, GEQ // primerjalni
    }
}

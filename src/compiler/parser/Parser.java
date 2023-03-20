/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;
import compiler.parser.ast.*;
import compiler.parser.ast.type.*;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.def.*;

import java.util.Iterator;

public class Parser {
    /**
     * Seznam leksikalnih simbolov.
     */
    private final List<Symbol> symbols;

    /**
     * Ciljni tok, kamor izpisujemo produkcije. Če produkcij ne želimo izpisovati,
     * vrednost opcijske spremenljivke nastavimo na Optional.empty().
     */
    private final Optional<PrintStream> productionsOutputStream;

    public Parser(List<Symbol> symbols, Optional<PrintStream> productionsOutputStream) {
        requireNonNull(symbols, productionsOutputStream);
        this.symbols = symbols;
        this.productionsOutputStream = productionsOutputStream;
    }

    Iterator<Symbol> symbolIterator;
    Symbol currentSymbol;

    /**
     * Izvedi sintaksno analizo.
     */
    public Ast parse() {

        symbolIterator = this.symbols.iterator();
        currentSymbol = symbolIterator.next();
        var ast = parseSource();
        return ast;
    }

    /**
     * Izpiše produkcijo na izhodni tok.
     */
    private void dump(String production) {
        if (productionsOutputStream.isPresent()) {
            productionsOutputStream.get().println(production);
        }
    }

    private TokenType check() {
        // System.out.println("TokenType: " + currentSymbol.tokenType.toString());
        return currentSymbol.tokenType;
    }

    private void skip() {
        // System.out.println(String.format("Skipped: %s (%s)", check(), currentSymbol.lexeme));
        currentSymbol = symbolIterator.next();
    }

    private void error(String message) {
        System.out.printf("ERROR: %s\n", message);
        System.exit(99);
    }

    private Ast parseSource() {
        dump("source -> definitions");
        var defs = parseDefinitions();
        if( check() != EOF ) error(String.format("Expected `EOF`, got `%s`\n", this.currentSymbol.lexeme));
        return defs;
    }

    private Defs parseDefinitions() {
        dump("definitions -> definition definitions'");

        List<Def> defs = new ArrayList<Def>();

        Position.Location start = currentSymbol.position.start;

        defs.add(parseDefinition());
        parseDefinitions2(defs);

        Position newPosition = new Position(start, currentSymbol.position.end);

        return new Defs(newPosition, defs);
    }

    private void parseDefinitions2(List<Def> defs) {
        if( (check() == OP_SEMICOLON) ) {
            dump("definitions' -> ; definitions");
            skip();

            defs.add(parseDefinition());
            parseDefinitions2(defs);
        }
        else {
            dump("definitions' -> e");
        }
    }

    private Def parseDefinition() {
        switch( check() ) {
            case KW_FUN: {
                dump("definition -> function_definition");
                return parseFunctionDefinition();
            }
            case KW_VAR: {
                dump("definition -> variable_definition");
                return parseVariableDefinition();
            }
            case KW_TYP: {
                dump("definition -> type_definition");
                return parseTypeDefinition();
            }
            default: {
                error("Definition symbols unknown: " + check() + " (" + currentSymbol.lexeme + ")");
                return null;
            }
        }
    }


    private FunDef parseFunctionDefinition() {
        dump("function_definition -> fun identifier ( parameters ) : type = expression");
        Position.Location start = currentSymbol.position.start;
        if( (check() != KW_FUN) ) error(String.format("Expected: `KW_FUN`, got `%s`\n", check()));
        skip();

        if( (check() != IDENTIFIER) ) error(String.format("Expected: `IDENTIFIER`, got `%s`\n", check()));
        String functionName = currentSymbol.lexeme;
        skip();

        if( (check() != OP_LPARENT) ) error(String.format("Expected: `OP_LPARENT`, got `%s`\n", check()));
        skip();
        List<FunDef.Parameter> functionParameters = parseParameters();

        if( (check() != OP_RPARENT) ) error(String.format("2) Expected: `OP_RPARENT`, got `%s`\n", check()));
        skip();

        if( (check() != OP_COLON) ) error(String.format("Expected: `OP_COLON`, got `%s`\n", check()));
        skip();
        Type functionType = parseType();

        if( (check() != OP_ASSIGN) ) error(String.format("Expected: `OP_ASSIGN`, got `%s`\n", check()));
        skip();

        Expr functionBody = parseExpression();
        Position newPosition = new Position(start, functionBody.position.end);

        return new FunDef(newPosition, functionName, functionParameters, functionType, functionBody);
    }

    private List<FunDef.Parameter> parseParameters() {
        dump("parameters -> parameter parameters'");
        List<FunDef.Parameter> parameters = new ArrayList<FunDef.Parameter>();

        parameters.add(parseParameter());
        parseParameters2(parameters);

        return parameters;
    }

    private FunDef.Parameter parseParameter() {
        dump("parameter -> identifier : type");

        Position.Location start = currentSymbol.position.start;

        if( (check() != IDENTIFIER) ) error(String.format("Expected: `IDENTIFIER`, got `%s`\n", check()));
        String parameterName = currentSymbol.lexeme;
        skip();

        if( (check() != OP_COLON) ) error(String.format("Expected: `COLON`, got `%s`\n", check()));
        skip();

        var paramType = parseType();

        Position newPosition = new Position(start, paramType.position.end);

        return new FunDef.Parameter(newPosition, parameterName, paramType);
    }
    private void parseParameters2(List<FunDef.Parameter> parameters) {
        if( check() == OP_COMMA ) {
            dump("parameters -> , parameters");
            skip();

            parameters.add(parseParameter());
            parseParameters2(parameters);
        }
        else {
            dump("parameters -> e");
        }
    }

    private Def parseTypeDefinition() {
        dump("type_definition -> typ identifier : type");
        Position.Location start = currentSymbol.position.start;
        skip();

        if( (check() != IDENTIFIER) ) error("Unknown parameter symbol: " + check() + " (should be IDENTIFIER)");
        String typeName = currentSymbol.lexeme;
        skip();

        if( (check() != OP_COLON) ) error("Unknown TypeDefinition symbol: " + check());
        skip();

        var typeType = parseType();

        Position newPosition = new Position(start, typeType.position.end);

        return new TypeDef(newPosition, typeName, typeType);
    }

    private Type parseType() {
        switch( check() ) {
            case IDENTIFIER: {
                dump("type -> identifier");
                Type type = new TypeName(currentSymbol.position, currentSymbol.lexeme);
                skip();
                return type;
            }
            case AT_LOGICAL: {
                dump("type -> logical");
                Type type = Atom.LOG(currentSymbol.position);
                skip();
                return type;
            }
            case AT_INTEGER: {
                dump("type -> integer");
                Type type = Atom.INT(currentSymbol.position);
                skip();
                return type;
            }
            case AT_STRING: {
                dump("type -> string");
                Type type = Atom.STR(currentSymbol.position);
                skip();
                return type;
            }
            case KW_ARR: {
                dump("type -> arr [ int_conts ] type");
                Position.Location start = currentSymbol.position.start;
                skip();

                if( check() != OP_LBRACKET ) error(String.format("Expected: `OP_LBRACKET`, got `%s`\n", check()));
                skip();

                if( check() != C_INTEGER ) error(String.format("Expected: `C_INTEGER`, got `%s`\n", check()));
                int arraySize = Integer.parseInt(currentSymbol.lexeme);
                skip();

                if( check() != OP_RBRACKET ) error(String.format("Expected: `OP_RBRACKET`, got `%s`\n", check()));
                skip();

                Type arrayType = parseType();

                Position newPosition = new Position(start, arrayType.position.end);

                return new Array(newPosition, arraySize, arrayType);
            }
            default: {
                error(String.format("Unknown type: `%s`\n", check()));
                return null; // we never get here, because error() exits the program
            }
        }
    }

    private Def parseVariableDefinition() {
        dump("variable_definition -> var identifier : type");
        Position.Location start = currentSymbol.position.start;
        skip();

        if( (check() != IDENTIFIER) ) error("Unknown variableDefinition symbol: " + check() + " (should be IDENTIFIER)");
        String varName = currentSymbol.lexeme;
        skip();

        if( (check() != OP_COLON) ) error("Unknown variableDefinition symbol: " + check() + " (expected OP_COLON)");
        skip();

        var varType = parseType();

        Position newPosition = new Position(start, varType.position.end);

        return new VarDef(newPosition, varName, varType);
    }

    private Expr parseExpression() {
        dump("expression -> logical_ior_expression expression'");
        Expr left = parseLogicalIorExpression();
        return parseExpression2(left);
    }

    private Expr parseExpression2(Expr left) {
        if( check() == OP_LBRACE ) {
            dump("expression2 -> { WHERE definitions }");
            Position.Location start = currentSymbol.position.start;
            skip();

            if( check() != KW_WHERE ) error(String.format("Expected `KW_WHERE`, got: `%s`\n", check()));
            skip();

            var innerDefs = parseDefinitions();

            if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got: `%s`\n", check()));
            skip();

            Position newPosition = new Position(start, currentSymbol.position.end);

            return new Where(newPosition, left, innerDefs);
        }
        else {
            dump("expression' -> e");
            return left;
        }
    }

    private Expr parseLogicalIorExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");
        var left = parseLogicalAndExpression();
        return parseLogicalIorExpression2(left);
    }

    private Expr parseLogicalIorExpression2(Expr left) {
        if( check() == OP_OR ) {
            dump("logical_ior_expression' -> | logical_ior_expression");
            Position.Location start = currentSymbol.position.start;
            skip();

            var right = parseLogicalAndExpression();
            Position newPosition = new Position(start, right.position.end);
            Binary bin = new Binary(newPosition, left, Binary.Operator.OR, right);

            return parseLogicalIorExpression2(bin);
        }
        else {
            dump("logical_ior_expression' -> e");
            return left;
        }
    }

    private Expr parseLogicalAndExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression'");
        var left = parseCompareExpression();
        return parseLogicalAndExpression2(left);
    }

    private Expr parseLogicalAndExpression2(Expr left) {
        if( check() == OP_AND ) {
            dump("logical_and_expression2 -> & logical_and_expression");
            Position.Location start = currentSymbol.position.start;
            skip();

            var right = parseCompareExpression();
            Position newPosition = new Position(start, right.position.end);
            Binary bin = new Binary(newPosition, left, Binary.Operator.AND, right);

            return parseLogicalAndExpression2(bin);
        }
        else {
            dump("logical_and_expression' -> e");
            return left;
        }
    }

    private Expr parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression'");
        var left = parseAdditiveExpression();
        return parseCompareExpression2(left);
    }

    private Expr parseCompareExpression2(Expr left) {
        switch( check() ) {
            case OP_EQ: {
                dump("compare_expression' -> == additive_expression");
                skip();

                var right = parseAdditiveExpression();
                Position newPosition = new Position(left.position.start, right.position.end);

                return new Binary(newPosition, left, Binary.Operator.EQ, right);
            }
            case OP_NEQ: {
                dump("compare_expression' -> != additive_expression");
                skip();

                var right = parseAdditiveExpression();
                Position newPosition = new Position(left.position.start, right.position.end);

                return new Binary(newPosition, left, Binary.Operator.NEQ, right);
            }
            case OP_LEQ: {
                dump("compare_expression' -> <= additive_expression");
                skip();

                var right = parseAdditiveExpression();
                Position newPosition = new Position(left.position.start, right.position.end);

                return new Binary(newPosition, left, Binary.Operator.LEQ, right);
            }
            case OP_GEQ: {
                dump("compare_expression' -> >= additive_expression");
                skip();

                var right = parseAdditiveExpression();
                Position newPosition = new Position(left.position.start, right.position.end);

                return new Binary(newPosition, left, Binary.Operator.GEQ, right);
            }
            case OP_LT: {
                dump("compare_expression' -> < additive_expression");
                skip();

                var right = parseAdditiveExpression();
                Position newPosition = new Position(left.position.start, right.position.end);

                return new Binary(newPosition, left, Binary.Operator.LT, right);
            }
            case OP_GT: {
                dump("compare_expression' -> > additive_expression");
                skip();

                var right = parseAdditiveExpression();
                Position newPosition = new Position(left.position.start, right.position.end);

                return new Binary(newPosition, left, Binary.Operator.GT, right);
            }
            default: {
                dump("compare_expression' -> e");
                return left;
            }
        }
    }

    private Expr parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression'");
        var left = parseMultiplicativeExpression();
        return parseAdditiveExpression2(left);
    }

    private Expr parseAdditiveExpression2(Expr left) {
        switch( check() ) {
            case OP_ADD: {
                dump("additive_expression' -> + additive_expression");
                skip();

                var right = parseMultiplicativeExpression();
                Position newPosition = new Position(left.position.start, right.position.end);
                Binary bin = new Binary(newPosition, left, Binary.Operator.ADD, right);

                return parseAdditiveExpression2(bin);
            }
            case OP_SUB: {
                dump("additive_expression' -> - additive_expression");
                skip();

                var right = parseMultiplicativeExpression();
                Position newPosition = new Position(left.position.start, right.position.end);
                Binary bin = new Binary(newPosition, left, Binary.Operator.SUB, right);

                return parseAdditiveExpression2(bin);
            }
            default: {
                dump("additive_expression' -> e");
                return left;
            }
        }
    }

    private Expr parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
        var left = parsePrefixExpression();
        return parseMultiplicativeExpression2(left);
    }

    private Expr parseMultiplicativeExpression2(Expr left) {
        switch( check() ) {
            case OP_MUL: {
                dump("multiplicative_expression' -> * multiplicative_expression");
                skip();

                var right = parsePrefixExpression();
                Position newPosition = new Position(left.position.start, right.position.end);
                Binary bin = new Binary(newPosition, left, Binary.Operator.MUL, right);

                return parseMultiplicativeExpression2(bin);
            }
            case OP_DIV: {
                dump("multiplicative_expression' -> / multiplicative_expression");
                skip();

                var right = parsePrefixExpression();
                Position newPosition = new Position(left.position.start, right.position.end);
                Binary bin = new Binary(newPosition, left, Binary.Operator.DIV, right);

                return parseMultiplicativeExpression2(bin);
            }
            case OP_MOD: {
                dump("multiplicative_expression' -> % multiplicative_expression");
                skip();

                var right = parsePrefixExpression();
                Position newPosition = new Position(left.position.start, right.position.end);
                Binary bin = new Binary(newPosition, left, Binary.Operator.MOD, right);

                return parseMultiplicativeExpression2(bin);
            }
            default: {
                dump("multiplicative_expression' -> e");
                return left;
            }
        }
    }

    private Expr parsePrefixExpression() {
        switch(check()) {
            case OP_ADD: {
                dump("prefix_expression -> + prefix_expression");
                Position.Location start = currentSymbol.position.start;
                skip();

                var innerExpr = parsePrefixExpression();
                Position newPosition = new Position(start, innerExpr.position.end);

                return new Unary(newPosition, innerExpr, Unary.Operator.ADD);
            }
            case OP_SUB: {
                dump("prefix_expression -> - prefix_expression");
                Position.Location start = currentSymbol.position.start;
                skip();

                var innerExpr = parsePrefixExpression();
                Position newPosition = new Position(start, innerExpr.position.end);

                return new Unary(newPosition, innerExpr, Unary.Operator.SUB);
            }
            case OP_NOT: {
                dump("prefix_expression -> ! prefix_expression");
                Position.Location start = currentSymbol.position.start;
                skip();

                var innerExpr = parsePrefixExpression();
                Position newPosition = new Position(start, innerExpr.position.end);

                return new Unary(newPosition, innerExpr, Unary.Operator.NOT);
            }
            default: {
                dump("prefix_expression -> postfix_expression");
                return parsePostfixExpression();
            }
        }
    }

    private Expr parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression'");
        var left = parseAtomExpression();
        return parsePostfixExpression2(left);
    }

    private Expr parsePostfixExpression2(Expr left) {
        if( check() == OP_LBRACKET ) {
            dump("postfix_expression' -> [ expression ] postfix_expression'");
            Position.Location start = currentSymbol.position.start;
            skip();

            var right = parseExpression();

            if( check() != OP_RBRACKET ) error(String.format("Expected `OP_RBRACKET`, got `%s`\n", check()));
            skip();

            Position newPosition = new Position(start, currentSymbol.position.end);
            Binary bin = new Binary(newPosition, left, Binary.Operator.ARR, right);

            return parsePostfixExpression2(bin);
        }
        else {
            dump("postfix_expression' -> e");
            return left;
        }
    }

    private Expr parseAtomExpression() {
        Position.Location start = currentSymbol.position.start;
        switch(check()) {
            case C_LOGICAL: {
                dump("atom_expression -> log_constant");
                var symbol =  currentSymbol.lexeme;
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new Literal(newPosition, symbol, Atom.Type.LOG);
            }
            case C_INTEGER: {
                dump("atom_expression -> int_constant");
                var symbol =  currentSymbol.lexeme;
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new Literal(newPosition, symbol, Atom.Type.INT);
            }
            case C_STRING: {
                dump("atom_expression -> str_constant");
                var symbol =  currentSymbol.lexeme;
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new Literal(newPosition, symbol, Atom.Type.STR);
            }
            case OP_LPARENT: {
                dump("atom_expression -> ( expression )");
                skip();

                var innerExprs = parseExpressions();

                if( check() != OP_RPARENT ) error(String.format("Expected `OP_RPARENT`, got %s\n", check()));
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new Block(newPosition, innerExprs);
            }
            case OP_LBRACE: {
                // TODO: Check if the position here needs to be fixed
                return parseAtomExpression2();
            }
            case IDENTIFIER: {
                dump("atom_expression -> identifier atom_expression4");
                var name = currentSymbol;
                skip();

                var exprs = parseAtomExpression4();

                if( exprs != null ) {
                    // NOTE: We are dealing with a function call
                    Position newPosition = new Position(start, currentSymbol.position.end);
                    return new Call(newPosition, exprs, name.lexeme);
                }
                else {
                    return new Name(name.position, name.lexeme);
                }
            }
            default: {
                error(String.format("Unknown symbol in atomExpression: `%s`\n", check()));
                return null;
            }
        }
    }

    private List<Expr> parseAtomExpression4() {
        /*
            null is returned when this is not a function call
         */
        if( check() == OP_LPARENT ) {
            // NOTE: this is a function call
            dump("atom_expression4 -> ( expressions )");
            Position.Location start = currentSymbol.position.start;
            skip();

            var innerExprs = parseExpressions();

            if( check() != OP_RPARENT ) error(String.format("Expected `OP_RPARENT`, got %s\n", check()));
            skip();

            return innerExprs;

        }
        else {
            dump("atom_expression4 -> e");

            return null;
        }
    }

    private Expr parseAtomExpression2() {
        dump("atom_expression -> { atom_expression2");
        Position.Location start = currentSymbol.position.start;
        skip();

        switch( check() ) {
            case KW_WHILE: {
                dump("atom_expression2 -> while expression : expression }");
                skip();

                var cond = parseExpression();

                if( check() != OP_COLON ) error(String.format("Expected `OP_COLON`, got %s\n", check()));
                skip();

                var body = parseExpression();

                if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got %s\n", check()));
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new While(newPosition, cond, body);
            }
            case KW_FOR: {
                dump("atom_expression2 -> for identifier = expression , expression , expression : expression }");
                skip();

                if( check() != IDENTIFIER ) error(String.format("Expected `IDENTIFIER`, got %s\n", check()));
                var counterName = new Name(currentSymbol.position, currentSymbol.lexeme);
                skip();

                if( check() != OP_ASSIGN ) error(String.format("Expected `OP_ASSIGN`, got %s\n", check()));
                skip();

                var low = parseExpression();

                if( check() != OP_COMMA) error(String.format("Expected `OP_COMMA`, got %s\n", check()));
                skip();

                var high = parseExpression();

                if( check() != OP_COMMA) error(String.format("Expected `OP_COMMA`, got %s\n", check()));
                skip();

                var step = parseExpression();

                if( check() != OP_COLON ) error(String.format("Expected `OP_COLON`, got %s\n", check()));
                skip();

                var body = parseExpression();

                if( check() != OP_RBRACE) error(String.format("Expected `OP_RBRACE`, got %s\n", check()));
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new For(newPosition, counterName, low, high, step, body);
            }
            case KW_IF: {
                dump("atom_expression2 -> if expression then expression atom_expression5");
                skip();

                var ifCond = parseExpression();

                if( check() != KW_THEN ) error(String.format("Expected `KW_THEN`, got `%s`\n", check()));
                skip();

                var ifThen = parseExpression();

                var ifElse = parseAtomExpression5();


                IfThenElse ifComplete;

                if( ifElse != null ) {
                    Position newPosition = new Position(start, ifElse.position.end);
                    ifComplete = new IfThenElse(newPosition, ifCond, ifThen, ifElse);
                }
                else {
                    Position newPosition = new Position(start, currentSymbol.position.end);
                    ifComplete = new IfThenElse(newPosition, ifCond, ifThen);
                }

                return ifComplete;
            }
            default: {
                dump("atom_expression2 -> expression = expression }");
                var left = parseExpression();

                if( check() != OP_ASSIGN ) error(String.format("Expected `OP_ASSIGN`, got `%s`\n", check()));
                skip();

                var right = parseExpression();

                if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got `%s`\n", check()));
                skip();

                Position newPosition = new Position(start, currentSymbol.position.end);

                return new Binary(newPosition, left, Binary.Operator.ASSIGN, right);
            }
        }
    }

    private Expr parseAtomExpression5() {
        /*
            null is returned when there is no else expression
         */

        if( check() == OP_RBRACE ) {
            dump("atom_expression5 -> }");
            skip();
            return null;
        }
        else if ( check() == KW_ELSE ) {
            dump("atom_expression5 -> else expression }");
            skip();

            var ifElse = parseExpression();

            if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got `%s`\n", check()));
            skip();

            return ifElse;
        }
        else {
            error(String.format("Unknown atomExpression5 symbol: `%s`\n", check()));
            return null; // this is never returned, in this case the program exits
        }
    }

    private List<Expr> parseExpressions() {
        dump("expressions -> expression expressions'");
        Position.Location start = currentSymbol.position.start;
        List<Expr> exprs = new ArrayList<Expr>();
        exprs.add(parseExpression());
        parseExpressions2(exprs);

        return exprs;
    }

    private void parseExpressions2(List<Expr> exprs) {
        if( check() == OP_COMMA ) {
            dump("expressions' -> , expressions");
            skip();
            exprs.add(parseExpression());
            parseExpressions2(exprs);
        }
        else {
            dump("expressions' -> e");
        }
    }
}

/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;

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
    public void parse() {
        System.out.println(this.symbols);
        symbolIterator = this.symbols.iterator();
        currentSymbol = symbolIterator.next();

        parseSource();
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
        currentSymbol = symbolIterator.next();
    }

    private void error(String message) {
        System.out.printf("ERROR: %s\n", message);
        System.exit(99);
    }

    private void parseSource() {
        // TODO: - rekurzivno spuščanje
        dump("source -> definitions");
        parseDefinitions();
    }

    private void parseDefinitions() {
        dump("definitions -> definition definitions'");
        parseDefinition();
        skip();
        parseDefinitions2();
    }

    private void parseDefinition() {
        switch( check() ) {
            case KW_FUN: {
                dump("definition -> function_definition");
                parseFunctionDefinition();
                break;
            }
            case KW_VAR: {
                dump("definition -> variable_definition");
                parseVariableDefinition();
                break;
            }
            case KW_TYP: {
                dump("definition -> type_definition");
                parseTypeDefinition();
                break;
            }
            default: {
                error("1) Definition symbols unknown: " + check() + " (" + currentSymbol.lexeme + ")");
            }
        }
    }

    private void parseFunctionDefinition() {
        dump("function_definition -> fun identifier ( parameters ) : type = expression");
        if( (check() != KW_FUN) ) error(String.format("Expected: `KW_FUN`, got `%s`\n", check()));
        skip();
        if( (check() != IDENTIFIER) ) error(String.format("Expected: `IDENTIFIER`, got `%s`\n", check()));
        skip();
        if( (check() != OP_LPARENT) ) error(String.format("Expected: `OP_LPARENT`, got `%s`\n", check()));
        skip();
        parseParameters();
        if( (check() != OP_RPARENT) ) error(String.format("2) Expected: `OP_RPARENT`, got `%s`\n", check()));
        skip();
        if( (check() != OP_COLON) ) error(String.format("Expected: `OP_COLON`, got `%s`\n", check()));
        skip();
        parseType();
        if( (check() != OP_ASSIGN) ) error(String.format("Expected: `OP_ASSIGN`, got `%s`\n", check()));
        skip();
        parseExpression();
    }

    private void parseDefinitions2() {
        skip();
        if( (check() == OP_SEMICOLON) ) {
            dump("definitions' -> ; definitions");
            skip();
            parseDefinitions();
        }
        else {
            dump("definitions' -> e");
            if( check() != EOF ) error(String.format("Expected `EOF`, got `%s`\n", check()));
        }
    }

    private void parseParameters() {
        dump("parameters -> parameter parameters'");
        parseParameter();
        parseParameters2();
    }

    private void parseParameter() {
        dump("parameter -> identifier : type");
        if( (check() != IDENTIFIER) ) error(String.format("Expected: `IDENTIFIER`, got `%s`\n", check()));
        skip();
        if( (check() != OP_COLON) ) error(String.format("Expected: `COLON`, got `%s`\n", check()));
        skip();
        parseType();
    }
    private void parseParameters2() {
        if( check() == OP_COMMA ) {
            dump("parameters -> , parameters");
            skip();
            parseParameters();
        }
        else {
            dump("parameters -> e");
        }
    }

    private void parseTypeDefinition() {
        dump("type_definition -> identifier : type");
        skip();
        if( (check() != IDENTIFIER) ) error("Unknown parameter symbol: " + check() + " (should be IDENTIFIER)");
        skip();
        if( (check() != OP_COLON) ) error("Unknown TypeDefinition symbol: " + check());
        skip();
        parseType();
    }

    private void parseType() {
        switch( check() ) {
            case IDENTIFIER: {
                dump("type -> identifier");
                skip();
                break;
            }
            case AT_LOGICAL: {
                dump("type -> logical");
                skip();
                break;
            }
            case AT_INTEGER: {
                dump("type -> integer");
                skip();
                break;
            }
            case AT_STRING: {
                dump("type -> string");
                skip();
                break;
            }
            case KW_ARR: {
                dump("type -> arr [ int_conts ] type");
                skip();
                if( check() != OP_LBRACKET ) error(String.format("Expected: `OP_LBRACKET`, got `%s`\n", check()));
                skip();
                if( check() != C_INTEGER ) error(String.format("Expected: `C_INTEGER`, got `%s`\n", check()));
                skip();
                if( check() != OP_RBRACKET ) error(String.format("Expected: `OP_RBRACKET`, got `%s`\n", check()));
                skip();
                parseType();
                break;
            }
        }
    }

    private void parseVariableDefinition() {
        dump("variable_definition -> var identifier : type");
        if( (check() != KW_VAR) ) error("Unknown variableDefinition symbol: " + check() + " (expected KW_VAR)");
        skip();
        if( (check() != IDENTIFIER) ) error("Unknown variableDefinition symbol: " + check() + " (should be IDENTIFIER)");
        skip();
        if( (check() != OP_COLON) ) error("Unknown variableDefinition symbol: " + check() + " (expected OP_COLON)");
        skip();
        parseType();
    }

    private void parseExpression() {
        dump("expression -> logical_ior_expression expression'");
        parseLogicalIorExpression();
        parseExpression2();
    }

    private void parseExpression2() {
        if( check() == OP_LBRACE ) {
            dump("expression2 -> { WHERE definitions }");
            skip();
            if( check() != KW_WHERE ) error(String.format("Expected `KW_WHERE`, got: `%s`\n", check()));
            skip();
            parseDefinitions();
            if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got: `%s`\n", check()));
            skip();
        }
        else {
            dump("expressions' -> e");
        }
    }

    private void parseLogicalIorExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");
        parseLogicalAndExpression();
        parseLogicalIorExpression2();
    }

    private void parseLogicalIorExpression2() {
        if( check() == OP_OR ) {
            dump("logical_ior_expression' -> | logical_ior_expression");
            skip();
            parseLogicalIorExpression();
        }
        else {
            dump("logical_ior_expression' -> e");
        }
    }

    private void parseLogicalAndExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression'");
        parseCompareExpression();
        parseLogicalAndExpression2();
    }

    private void parseLogicalAndExpression2() {
        if( check() == OP_AND ) {
            dump("logical_and_expression2 -> & logical_and_expression");
            skip();
            parseLogicalAndExpression();
        }
        else {
            dump("logical_and_expression' -> e");
        }
    }

    private void parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression'");
        parseAdditiveExpression();
        parseCompareExpression2();
    }

    private void parseCompareExpression2() {
        switch( check() ) {
            case OP_EQ: {
                dump("compare_expression' -> == additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            case OP_NEQ: {
                dump("compare_expression' -> != additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            case OP_LEQ: {
                dump("compare_expression' -> <= additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            case OP_GEQ: {
                dump("compare_expression' -> >= additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            case OP_LT: {
                dump("compare_expression' -> < additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            case OP_GT: {
                dump("compare_expression' -> > additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            default: {
                dump("compare_expression' -> e");
                break;
            }
        }
    }

    private void parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression'");
        parseMultiplicativeExpression();
        parseAdditiveExpression2();
    }

    private void parseAdditiveExpression2() {
        switch( check() ) {
            case OP_ADD: {
                dump("additive_expression' -> + additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            case OP_SUB: {
                dump("additive_expression' -> - additive_expression");
                skip();
                parseAdditiveExpression();
                break;
            }
            default: {
                dump("additive_expression' -> e");
                break;
            }
        }
    }

    private void parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
        parsePrefixExpression();
        parseMultiplicativeExpression2();
    }

    private void parseMultiplicativeExpression2() {
        switch( check() ) {
            case OP_MUL: {
                dump("multiplicative_expression' -> * multiplicative_expression");
                skip();
                parseMultiplicativeExpression();
                break;
            }
            case OP_DIV: {
                dump("multiplicative_expression' -> / multiplicative_expression");
                skip();
                parseMultiplicativeExpression();
                break;
            }
            case OP_MOD: {
                dump("multiplicative_expression' -> % multiplicative_expression");
                skip();
                parseMultiplicativeExpression();
                break;
            }
            default: {
                dump("multiplicative_expression' -> e");
                break;
            }
        }
    }

    private void parsePrefixExpression() {
        switch(check()) {
            case OP_ADD: {
                dump("prefix_expression -> + prefix_expression");
                parsePrefixExpression();
                break;
            }
            case OP_SUB: {
                dump("prefix_expression -> - prefix_expression");
                parsePrefixExpression();
                break;
            }
            case OP_NOT: {
                dump("prefix_expression -> ! prefix_expression");
                parsePrefixExpression();
                break;
            }
            default: {
                parsePostfixExpression();
            }
        }
    }

    private void parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression'");
        parseAtomExpression();
        parsePostfixExpression2();
    }

    private void parsePostfixExpression2() {
        if( check() == OP_LBRACKET ) {
            dump("postfix_expression' -> [ expression ] postfix_expression'");
            skip();
            parseExpression();
            if( check() != OP_RBRACKET ) error(String.format("Expected `OP_RBRACKET`, got `%s`\n", check()));
            skip();
            parsePostfixExpression2();
        }
        else {
            dump("postfix_expression' -> e");
        }
    }

    private void parseAtomExpression() {
        switch(check()) {
            case C_LOGICAL: {
                dump("atom_expression -> log_constant");
                skip();
                break;
            }
            case C_INTEGER: {
                dump("atom_expression -> int_constant");
                skip();
                break;
            }
            case C_STRING: {
                dump("atom_expression -> str_constant");
                skip();
                break;
            }
            case OP_LPARENT: {
                dump("atom_expression -> ( expression )");
                skip();
                parseExpression();
                if( check() != OP_RPARENT ) error(String.format("Expected `OP_RPARENT`, got %s\n", check()));
                skip();
                break;
            }
            case OP_RBRACE: {
                dump("atom_expression -> { atom_expression2");
                parseAtomExpression2();
                break;
            }
            case IDENTIFIER: {
                dump("atom_expression -> identifier atom_expression4");
                skip();
                parseAtomExpression4();
                break;
            }
        }
    }

    private void parseAtomExpression4() {
        skip();
        if( check() == OP_LPARENT ) {
            dump("atom_expression4 -> ( expressions )");
            skip();
            parseExpressions();
            if( check() != OP_RPARENT ) error(String.format("Expected `OP_RPARENT`, got %s\n", check()));
            skip();
        }
        else {
            dump("atom_expression4 -> e");
        }
    }

    private void parseAtomExpression2() {
        switch( check() ) {
            case KW_WHILE: {
                dump("atom_expression2 -> while expression : expression }");
                skip();
                parseExpression();
                if( check() != OP_COLON ) error(String.format("Expected `OP_COLON`, got %s\n", check()));
                skip();
                parseExpression();
                if( check() != OP_RBRACKET ) error(String.format("Expected `OP_RBRACKET`, got %s\n", check()));
                break;
            }
            case KW_FOR: {
                dump("atom_expression2 -> for identifier = expression , expression , expression : expression }");
                skip();
                if( check() != IDENTIFIER ) error(String.format("Expected `IDENTIFIER`, got %s\n", check()));
                skip();
                if( check() != OP_ASSIGN ) error(String.format("Expected `OP_ASSIGN`, got %s\n", check()));
                skip();
                parseExpression();
                if( check() != OP_COMMA) error(String.format("Expected `OP_COMMA`, got %s\n", check()));
                skip();
                parseExpression();
                if( check() != OP_COMMA) error(String.format("Expected `OP_COMMA`, got %s\n", check()));
                skip();
                parseExpression();
                if( check() != OP_ASSIGN ) error(String.format("Expected `OP_ASSIGN`, got %s\n", check()));
                skip();
                parseExpression();
                if( check() != OP_RBRACE) error(String.format("Expected `OP_RBRACE`, got %s\n", check()));
                skip();
                break;
            }
            case KW_IF: {
                dump("atom_expression2 -> if expression atom_expression3");
                skip();
                parseExpression();
                parseAtomExpression3();
            }
            default: {
                dump("atom_expression2 -> expression = expression }");
                parseExpression();
                if( check() != OP_ASSIGN ) error(String.format("Expected `OP_ASSIGN`, got `%s`\n", check()));
                skip();
                parseExpression();
                if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got `%s`\n", check()));
            }
        }
    }

    private void parseAtomExpression3() {
        if( check() == OP_COLON ) {
            dump("atom_expression3 -> : expression }");
            skip();
            parseExpression();
            if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got `%s`\n", check()));
            skip();
        }
        else if ( check() == KW_THEN ) {
            dump("atom_expression3 -> then expression else expression }");
            skip();
            parseExpression();
            if( check() != KW_ELSE ) error(String.format("Expected `KW_ELSE`, got `%s`\n", check()));
            skip();
            parseExpression();
            if( check() != OP_RBRACE ) error(String.format("Expected `OP_RBRACE`, got `%s`\n", check()));
            skip();
        }
        else {
            error(String.format("Unknown atomExpression3 symbol: `%s`\n", check()));
        }
    }

    private void parseExpressions() {
        dump("expressions -> expression expressions'");
        parseExpression();
        parseExpressions2();
    }

    private void parseExpressions2() {
        if( check() == OP_COMMA ) {
            dump("expressions' -> , expressions");
            skip();
            parseExpressions();
        }
        else {
            dump("expressions' -> e");
        }
    }
}

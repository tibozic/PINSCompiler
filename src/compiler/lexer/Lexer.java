/**
 * @Author: turk
 * @Description: Leksikalni analizator.
 */

package compiler.lexer;

import static common.RequireNonNull.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    /**
     * Izvorna koda.
     */
    private final String source;

    /**
     * Preslikava iz ključnih besed v vrste simbolov.
     */
    private final static Map<String, TokenType> keywordMapping;

    static {
        keywordMapping = new HashMap<>();
        for (var token : TokenType.values()) {
            var str = token.toString();
            if (str.startsWith("KW_")) {
                keywordMapping.put(str.substring("KW_".length()).toLowerCase(), token);
            }
            if (str.startsWith("AT_")) {
                keywordMapping.put(str.substring("AT_".length()).toLowerCase(), token);
            }
            if (str.startsWith("OP")) {
                keywordMapping.put(str.substring("OP".length()).toLowerCase(), token);
            }
        }
    }

    /**
     * Ustvari nov analizator.
     * 
     * @param source Izvorna koda programa.
     */
    public Lexer(String source) {
        requireNonNull(source);
        this.source = source;
    }

    /**
     * Izvedi leksikalno analizo.
     * 
     * @return seznam leksikalnih simbolov.
     */


    public enum LexerStates {
        Lex_Default,
        Lex_ID,
        Lex_INT,
        Lex_Comment,
        Lex_String,
        Lex_Operator,
    }

    static int current_column = 0;
    static int current_line = 1;
    static int token_start_column = 1;
    static int token_start_line = 1;

    public List<Symbol> scan() {
        var symbols = new ArrayList<Symbol>();

       current_column = 0;
       current_line = 1;
       token_start_column = 1;
       token_start_line = 1;

       StringBuilder token = new StringBuilder();

        LexerStates currentState = LexerStates.Lex_Default;

        for( int i = 0; i < this.source.length(); ++i ) {
            current_column++;
            char ch = this.source.charAt(i);


            switch (currentState) {
                case Lex_Default: {
                    token_start_line = current_line;
                    token_start_column = current_column;

                    if (isASCIIchar(ch) || ch == '_') {
                        currentState = LexerStates.Lex_ID;
                        token.append(ch);
                    } else if (Character.isDigit(ch)) {
                        currentState = LexerStates.Lex_INT;
                        token.append(ch);
                    } else if (ch == '#') {
                        currentState = LexerStates.Lex_Comment;
                    } else if (ch == '\'') {
                        currentState = LexerStates.Lex_String;
                    } else if (isOperator(ch)) {
                        currentState = LexerStates.Lex_Operator;
                        token.append(ch);
                    } else if (checkWhitespace(ch)) {
                        continue;
                    } else {
                        // throw new Exception("ERROR: unknown symbol `" + ch + "`");
                        // System.out.printf("ERROR: Invalid symbol `%c` (%d:%d)\n", ch, current_line, current_column);
                        System.exit(99);
                    }

                    break;
                }
                case Lex_ID: {
                    if (isASCIIchar(ch) || ch == '_' || Character.isDigit(ch)) {
                        token.append(ch);
                    } else {
                        symbols.add(createToken(token.toString()));
                        token_start_line = current_line;
                        token_start_column = current_column;

                        currentState = LexerStates.Lex_Default;
                        token = new StringBuilder();
                        i--;
                        current_column--;

                        continue;
                    }

                    break;
                }
                case Lex_INT: {
                    if (Character.isDigit(ch)) {
                        token.append(ch);
                    } else {
                        symbols.add(createToken(token.toString()));
                        token_start_line = current_line;
                        token_start_column = current_column;

                        currentState = LexerStates.Lex_Default;
                        token = new StringBuilder();
                        i--;
                        current_column--;

                        continue;
                    }
                    break;
                }
                case Lex_Comment: {
                    if (ch == 10 || ch == 13) {
                        currentState = LexerStates.Lex_Default;
                        checkWhitespace(ch);
                    }
                    break;
                }
                case Lex_String: {
                    if (ch == '\'' && (i < this.source.length()-1) && (this.source.charAt(i + 1)) != '\'') {
                        // FIXME: Sometimes strings have wrong location.
                        current_column++;
                        i++;
                        symbols.add(createToken(token.toString(), true));

                        token_start_line = current_line;
                        token_start_column = current_column;

                        currentState = LexerStates.Lex_Default;
                        token = new StringBuilder();
                    } else if (ch == '\'' && (i < this.source.length()-1) && (this.source.charAt(i + 1)) == '\'') {
                        token.append(ch);
                        i++;
                        current_column++;
                    } else if (ch >= 32 && ch <= 126) {
                        token.append(ch);
                    } else {
                        // System.out.printf("ERROR: Invalid character in string `%c` (%d:%d)", ch, current_line, current_column);
                        System.exit(99);
                    }
                    break;
                }
                case Lex_Operator: {
                    if( !isValidOperator(token.append(ch).toString()) ) {
                        // delete the last char (when appended, token was not valid)
                        token.setLength(token.length() - 1);

                        symbols.add(createToken(token.toString()));
                        currentState = LexerStates.Lex_Default;
                        token = new StringBuilder();
                        token_start_line = current_line;
                        token_start_column = current_column;

                        i--;
                        current_column--;
                        continue;
                    }

                    break;
                }
            }
        }

        // System.out.println(currentState);
        if( currentState != LexerStates.Lex_Default && currentState != LexerStates.Lex_Comment ) {
            current_column++;
            symbols.add(createToken(token.toString()));
            token_start_line = current_line;
            token_start_column = current_column;
        }

        // TODO: Lexer should throw EOF as last token
        Position position = new Position(token_start_line, token_start_column, current_line, current_column);
        Symbol token_symbol = new Symbol(position, TokenType.EOF, "$");
        symbols.add(token_symbol);

        if( currentState == LexerStates.Lex_String ) {
            // System.out.println("ERROR: EOF Reached while parsing string");
            System.exit(99);
        }

        return symbols;
    }

    public boolean checkWhitespace(char ch) {
       /*
        This function returns true if ch is a whitespace character (ASCII: 9, 10, 13, 32),
        return false otherwise.
        This function also includes logic for increments column (tab -> col += 4) and row (newline -> row +=1 ) count.
        */

        if( ch == 9 ) {
            // each tab is 4 wide, 1 was already counted with new character
            current_column += 3;
            return true;
        }
        else if( ch == 10 ) {
            // if there is a new line, we reset the column count and increment line count
            current_column = 0;
            current_line++;
            token_start_line = current_line;
           return true;
        }
        else if( ch == 32 || ch == 13 )
            return true;

        return false;
    }

    public boolean isOperator(char ch) {
        char symbols[] = new char[]{'+', '-', '*', '/', '%', '&', '|', '!', '<', '>', '(', ')', '[', ']', '{', '}', ':',
                                    ';', '.', ',', '='};
        if( (new String(symbols).contains(String.valueOf(ch))) )
            return true;

        return false;
    }

    public boolean isValidOperator(String op) {
        String symbols[] = new String[]{"+", "-", "*", "/", "%", "&", "|", "!", "<", "<=", ">", ">=", "(", ")", "[",
                                        "]", "{", "}", ":", ";", ".", ",", "=", "==", "!="};

        for( String s : symbols ) {
            if( s.equals(op))
                return true;
        }

        return false;
    }

    public Symbol createToken(String token) {
        Position position = new Position(token_start_line, token_start_column, current_line, current_column);
        TokenType type = getTokenType(token);
        Symbol token_symbol = new Symbol(position, type, token);
        return token_symbol;
    }

    public Symbol createToken(String token, boolean isString) {
        if( isString ) {
            Position position = new Position(token_start_line, token_start_column, current_line, current_column);
            Symbol token_symbol = new Symbol(position, TokenType.C_STRING, token);
            return token_symbol;
        }
        return createToken(token);
    }

    public TokenType getTokenType(String token) {
        if( Character.isDigit(token.charAt(0)) )
            return TokenType.C_INTEGER;

        if( isOperator(token.charAt(0)) )
            return getOperatorType((token));

        for( HashMap.Entry<String, TokenType> entry : keywordMapping.entrySet() ) {
            if( entry.getKey().equals(token) )
                return entry.getValue();
        }

        if( token.equals("true") || token.equals("false") )
            return TokenType.C_LOGICAL;

        return TokenType.IDENTIFIER;
    }

    public TokenType getOperatorType(String  op) {
        switch(op) {
            case "+": {
                return TokenType.OP_ADD;
            }
            case "-": {
                return TokenType.OP_SUB;
            }
            case "*": {
                return TokenType.OP_MUL;
            }
            case "/": {
                return TokenType.OP_DIV;
            }
            case "%": {
                return TokenType.OP_MOD;
            }
            case "&": {
                return TokenType.OP_AND;
            }
            case "|": {
                return TokenType.OP_OR;
            }
            case "!": {
                return TokenType.OP_NOT;
            }
            case "==": {
                return TokenType.OP_EQ;
            }
            case "!=": {
                return TokenType.OP_NEQ;
            }
            case "<": {
                return TokenType.OP_LT;
            }
            case ">": {
                return TokenType.OP_GT;
            }
            case "<=": {
                return TokenType.OP_LEQ;
            }
            case ">=": {
                return TokenType.OP_GEQ;
            }
            case "{": {
                return TokenType.OP_LBRACE;
            }
            case "}": {
                return TokenType.OP_RBRACE;
            }
            case "[": {
                return TokenType.OP_LBRACKET;
            }
            case "]": {
                return TokenType.OP_RBRACKET;
            }
            case "(": {
                return TokenType.OP_LPARENT;
            }
            case ")": {
                return TokenType.OP_RPARENT;
            }
            case ":": {
                return TokenType.OP_COLON;
            }
            case ";": {
                return TokenType.OP_SEMICOLON;
            }
            case ".": {
                return TokenType.OP_DOT;
            }
            case ",": {
                return TokenType.OP_COMMA;
            }
            case "=": {
                return TokenType.OP_ASSIGN;
            }
            default: {
                return TokenType.IDENTIFIER;
            }
        }
    }

    public boolean isASCIIchar(char ch) {
        if( (ch >= 65 && ch <= 90) || (ch >= 97 && ch <= 122) )
            return true;

        return false;
    }

    public boolean isValidASCIIstringChar(char ch) {
        if (ch <= 32 && ch >= 126)
            return true;
        return false;
    }

}

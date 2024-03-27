// package com.palande.aum.scanner;
package co2;

import co2.Token.Kind;

public class Token {

    public enum Kind {
        // boolean operators
        AND("and"),
        OR("or"),
        NOT("not"),

        // arithmetic operators
        POW("^"),

        MUL("*"),
        DIV("/"),
        MOD("%"),

        ADD("+"),
        SUB("-"),

        // relational operators
        EQUAL_TO("=="),
        NOT_EQUAL("!="),
        LESS_THAN("<"),
        LESS_EQUAL("<="),
        GREATER_EQUAL(">="),
        GREATER_THAN(">"),

        // assignment operators
        ASSIGN("="),
        ADD_ASSIGN("+="),
        SUB_ASSIGN("-="),
        MUL_ASSIGN("*="),
        DIV_ASSIGN("/="),
        MOD_ASSIGN("%="),
        POW_ASSIGN("^="),

        // unary increment/decrement
        UNI_INC("++"),
        UNI_DEC("--"),

        // primitive types
        VOID("void"),
        BOOL("bool"),
        INT("int"),
        FLOAT("float"),

        // boolean literals
        TRUE("true"),
        FALSE("false"),

        // region delimiters
        OPEN_PAREN("("),
        CLOSE_PAREN(")"),
        OPEN_BRACE("{"),
        CLOSE_BRACE("}"),
        OPEN_BRACKET("["),
        CLOSE_BRACKET("]"),

        // field/record delimiters
        COMMA(","),
        COLON(":"),
        SEMICOLON(";"),
        PERIOD("."),

        // control flow statements
        IF("if"),
        THEN("then"),
        ELSE("else"),
        FI("fi"),

        WHILE("while"),
        DO("do"),
        OD("od"),

        REPEAT("repeat"),
        UNTIL("until"),

        CALL("call"),
        RETURN("return"),

        // keywords
        MAIN("main"),
        FUNC("function"),

        // special cases
        INT_VAL(),
        FLOAT_VAL(),
        IDENT(),
        EMPTY(),

        EOF(),

        ERROR();

        private String defaultLexeme;

        Kind () {
            defaultLexeme = "";
        }

        Kind (String lexeme) {
            defaultLexeme = lexeme;
        }

        public boolean hasStaticLexeme () {
            return defaultLexeme != null;
        }

        // OPTIONAL: convenience function - boolean matches (String lexeme)
        //           to report whether a Token.Kind has the given lexeme
        //           may be useful
    }

    private int lineNum;
    private int charPos;
    Kind kind;  // package-private
    private String lexeme = "";

    public static Token EOF (int linePos, int charPos) {
        Token tok = new Token(linePos, charPos);
        tok.kind = Kind.EOF;
        return tok;
    }

    private Token (int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;

        // no lexeme provide, signal error
        this.kind = Kind.ERROR;
        this.lexeme = "No Lexeme Given";
    }

    public Token (String lexeme, int lineNum, int charPos) {
        // System.out.println("New Token: [" + lexeme + "]");
        this.lexeme = lexeme;
        this.lineNum = lineNum;
        this.charPos = charPos;
        
        switch (lexeme) {
            case "":
                this.kind = Kind.EMPTY;
                break;
            case "and":
                this.kind = Kind.AND;
                break;
            case "AND":
                this.kind = Kind.AND;
                break;
            case "or":
                this.kind = Kind.OR;
                break;
            case "OR":
                this.kind = Kind.OR;
                break;
            case "not":
                this.kind = Kind.NOT;
                break;
            case "^":
                this.kind = Kind.POW;
                break;
            case "*":
                this.kind = Kind.MUL;
                break;
            case "/":
                this.kind = Kind.DIV;
                break;
            case "%":
                this.kind = Kind.MOD;
                break;
            case "+":
                this.kind = Kind.ADD;
                break;
            case "-":
                this.kind = Kind.SUB;
                break;
            case "==":
                this.kind = Kind.EQUAL_TO;
                break;
            case "!":
                this.kind = Kind.EMPTY;
                break;
            case "!=":
                this.kind = Kind.NOT_EQUAL;
                break;
            case "<":
                this.kind = Kind.LESS_THAN;
                break;
            case "<=":
                this.kind = Kind.LESS_EQUAL;
                break;
            case ">=":
                this.kind = Kind.GREATER_EQUAL;
                break;
            case ">":
                this.kind = Kind.GREATER_THAN;
                break;
            case "=":
                this.kind = Kind.ASSIGN;
                break;
            case "-=":
                this.kind = Kind.SUB_ASSIGN;
                break;
            case "+=":
                this.kind = Kind.ADD_ASSIGN;
                break;
            case "*=":
                this.kind = Kind.MUL_ASSIGN;
                break;
            case "/=":
                this.kind = Kind.DIV_ASSIGN;
                break;
            case "%=":
                this.kind = Kind.MOD_ASSIGN;
                break;
            case "^=":
                this.kind = Kind.POW_ASSIGN;
                break;
            case "++":
                this.kind = Kind.UNI_INC;
                break;
            case "--":
                this.kind = Kind.UNI_DEC;
                break;
            case "void":
                this.kind = Kind.VOID;
                break;
            case "bool":
                this.kind = Kind.BOOL;
                break;
            case "int":
                this.kind = Kind.INT;
                break;
            case "float":
                this.kind = Kind.FLOAT;
                break;
            case "true":
                this.kind = Kind.TRUE;
                break;
            case "false":
                this.kind = Kind.FALSE;
                break;
            case "(":
                this.kind = Kind.OPEN_PAREN;
                break;
            case ")":
                this.kind = Kind.CLOSE_PAREN;
                break;
            case "{":
                this.kind = Kind.OPEN_BRACE;
                break;
            case "}":
                this.kind = Kind.CLOSE_BRACE;
                break;
            case "[":
                this.kind = Kind.OPEN_BRACKET;
                break;
            case "]":
                this.kind = Kind.CLOSE_BRACKET;
                break;
            case ",":
                this.kind = Kind.COMMA;
                break;
            case ":":
                this.kind = Kind.COLON;
                break;
            case ";":
                this.kind = Kind.SEMICOLON;
                break;
            case ".":
                this.kind = Kind.PERIOD;
                break;
            case "if":
                this.kind = Kind.IF;
                break;
            case "then":
                this.kind = Kind.THEN;
                break;
            case "else":
                this.kind = Kind.ELSE;
                break;
            case "fi":
                this.kind = Kind.FI;
                break;
            case "while":
                this.kind = Kind.WHILE;
                break;
            case "do":
                this.kind = Kind.DO;
                break;
            case "od":
                this.kind = Kind.OD;
                break;
            case "repeat":
                this.kind = Kind.REPEAT;
                break;
            case "until":
                this.kind = Kind.UNTIL;
                break;
            case "call":
                this.kind = Kind.CALL;
                break;
            case "return":
                this.kind = Kind.RETURN;
                break;
            case "main":
                this.kind = Kind.MAIN;
                break;
            case "function":
                this.kind = Kind.FUNC;
                break;
            case "EOF":
                this.kind = Kind.EOF;
                break;
            default:
                // check if int value
                if (isInt(lexeme)) {
                    this.kind = Kind.INT_VAL;
                    break;
                }

                // check if float value
                if (isFloat(lexeme)) {
                    this.kind = Kind.FLOAT_VAL;
                    break;
                }

                // check if variable name
                if (isIdent(lexeme)) {
                    this.kind = Kind.IDENT;
                    break;
                }

                // if we don't match anything, signal error   
                this.kind = Kind.ERROR;
                this.lexeme = "Unrecognized lexeme: [" + lexeme + "]";
                break;
        }
    }

    public int lineNumber () {
        return lineNum;
    }

    public int charPosition () {
        return charPos;
    }

    public String lexeme () {
        return this.lexeme;
    }

    public Kind kind () {
        return this.kind;
    }

    public boolean is(Kind k) {
        return (this.kind.equals(k));
    }

    // OPTIONAL: add any additional helper or convenience methods
    //           that you find make for a cleaner design
    public void setLexeme(String lexeme) {
        this.lexeme = lexeme;
    }

    private boolean isIdent(String lexeme) {
        for (int i = 0; i < lexeme.length(); i++) {
            if (!((Character.isLetter(lexeme.charAt(i))) || (Character.isDigit(lexeme.charAt(i)) || (lexeme.charAt(i) == '_')))) {
                return false;
            }
        }

        if (!Character.isLetter(lexeme.charAt(0))) {
            return false;
        }

        if (lexeme.charAt(0) == '_') {
            return false;
        }

        return true;
    }

    private boolean isInt(String lexeme) {
        try {
            Integer.valueOf(lexeme);

            if (lexeme.charAt(0) == '+') {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isFloat(String lexeme) {
        try {
            Float.parseFloat(lexeme);

            if (lexeme.indexOf('.') == -1) {
                // System.out.println("case 1");
                return false;
            }

            if (lexeme.indexOf('.') == 0) {
                // System.out.println("case 2");
                return false;
            }

            for (int i = 0; i < lexeme.length(); i++) {
                if ((!Character.isDigit(0)) && (!(lexeme.charAt(i) == '.'))) {
                    return true;
                }
            }

            if (lexeme.charAt(lexeme.length()-1) == 'f') {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString () {
        return "Line: " + lineNum + ", Char: " + charPos + ", Lexeme: " + lexeme;
    }
}

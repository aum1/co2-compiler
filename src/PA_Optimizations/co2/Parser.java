package co2;

import java.io.*;
import java.util.*;

import co2.Token.Kind;

public class Parser {

    // Error Reporting ============================================================
    private StringBuilder errorBuffer = new StringBuilder();

    private String reportSyntaxError (NonTerminal nt) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected a token from " + nt.name() + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportSyntaxError (Token.Kind kind) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected " + kind + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    public String errorReport () {
        return errorBuffer.toString();
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }

    private class QuitParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public QuitParseException (String errorMessage) {
            super(errorMessage);
        }
    }

    private int lineNumber () {
        return currentToken.lineNumber();
    }

    private int charPosition () {
        return currentToken.charPosition();
    }

// Parser ============================================================
    private co2.Scanner scanner;
    private Token currentToken;

    private BufferedReader reader;
    private StringTokenizer st;

    // TODO: add maps from Token IDENT to int/float/bool
    private Map<Token.Kind, Token.Kind> identMap = new HashMap<Token.Kind, Token.Kind>();

    public Parser (co2.Scanner scanner, InputStream in) {
        this.scanner = scanner;
        currentToken = this.scanner.next();

        reader = new BufferedReader(new InputStreamReader(in));
        st = null;
    }

    public void parse () {
        try {
            computation();
        }
        catch (QuitParseException q) {
            // too verbose
            // errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            // errorBuffer.append("[Could not complete parsing.]");
        }
    }

// Helper Methods =============================================================
    private boolean have (Token.Kind kind) {
        return currentToken.is(kind);
    }

    private boolean have (NonTerminal nt) {
        return nt.firstSet().contains(currentToken.kind);
    }

    private boolean accept (Token.Kind kind) {
        if (have(kind)) {
            try {
                currentToken = scanner.next();
            }
            catch (NoSuchElementException e) {
                if (!kind.equals(Token.Kind.EOF)) {
                    String errorMessage = reportSyntaxError(kind);
                    throw new QuitParseException(errorMessage);
                }
            }
            return true;
        }
        return false;
    }

    private boolean accept (NonTerminal nt) {
        if (have(nt)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }

    private boolean expect (Token.Kind kind) {
        if (accept(kind)) {
            return true;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private boolean expect (NonTerminal nt) {
        if (accept(nt)) {
            return true;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (Token.Kind kind) {
        Token tok = currentToken;
        if (accept(kind)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (NonTerminal nt) {
        Token tok = currentToken;
        if (accept(nt)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

// Pre-defined Functions ======================================================
    private String nextInput () {
        while (st == null || !st.hasMoreElements()) {
            try {
                st = new StringTokenizer(reader.readLine());
            }
            catch (IOException e) {
                throw new QuitParseException("Interepter: Couldn't read data file\n" + e.getMessage());
            }
        }
        return st.nextToken();
    }

    private int readInt () {
        System.out.print("int? ");
        return Integer.parseInt(nextInput());
    }

    private float readFloat () {
        System.out.print("float? ");
        return Float.parseFloat(nextInput());
    }

    private boolean readBool () {
        System.out.print("true or false? ");
        return Boolean.parseBoolean(nextInput());
    }

    private void printInt (int x) {
        System.out.print(x + " ");
    }

    private void printFloat (float x) {
        System.out.printf("%.2f ",x);
    }

    private void printBool (boolean x) {
        System.out.print(x + " ");
    }

    private void println () {
        System.out.println();
    }

// Grammar Rules ==============================================================

    // function for matching rule that only expects nonterminal's FIRST set
    private Token matchNonTerminal (NonTerminal nt) {
        return expectRetrieve(nt);
    }

    // relOp = == | != | < | <= | > | >=
    private Token relOp() {
        return matchNonTerminal(NonTerminal.REL_OP);
    }

    private boolean acceptRelOp() {
        // match non terminal will throw error
        return (accept(Token.Kind.EQUAL_TO) || accept(Token.Kind.NOT_EQUAL)|| accept(Token.Kind.LESS_THAN)
            || accept(Token.Kind.LESS_EQUAL) || accept(Token.Kind.GREATER_THAN) || accept(Token.Kind.GREATER_EQUAL));
    }

    private Token powOp() {
        return matchNonTerminal(NonTerminal.POW_OP);
    }

    private boolean acceptPowOp() {
        return (accept(Token.Kind.POW));
    }

    private Token addOp() {
        return matchNonTerminal(NonTerminal.ADD_OP);
    }

    private boolean acceptAddOp() {
        return (accept(Token.Kind.ADD) || accept(Token.Kind.SUB) || accept(Token.Kind.OR));
    }

    private Token mulOp() {
        return matchNonTerminal(NonTerminal.MUL_OP);
    }
    
    private boolean acceptMulOp() {
        return (accept(Token.Kind.MUL) || accept(Token.Kind.DIV) || accept(Token.Kind.MOD) || accept(Token.Kind.AND));
    }

    private boolean acceptAssignOp() {
        return (accept(Token.Kind.ASSIGN) || accept(Token.Kind.ADD_ASSIGN) || accept(Token.Kind.SUB_ASSIGN) || accept(Token.Kind.MUL_ASSIGN) 
            || accept(Token.Kind.DIV_ASSIGN) || accept(Token.Kind.MOD_ASSIGN) || accept(Token.Kind.POW_ASSIGN));
    }

    // assignOp = = | += | -= | *= | /= | %= | ^=
    private Token assignOp() {
        return matchNonTerminal(NonTerminal.ASSIGN_OP);
    }

    // unaryOp = ++ | --
    private Token unaryOp() {
        return matchNonTerminal(NonTerminal.UNARY_OP);
    }

    private Token type() {
        return matchNonTerminal(NonTerminal.TYPE_DECL);
    }

    // literal = integerLit | floatLit
    private Token literal () {
        return matchNonTerminal(NonTerminal.LITERAL);
    }

    // designator = ident { "[" relExpr "]" }
    private void designator () {
        int lineNum = lineNumber();
        int charPos = charPosition();

        Token ident = expectRetrieve(Token.Kind.IDENT);
        while (have(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.OPEN_BRACKET);
            relExpr();
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }

    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private void computation () {
        expect(Token.Kind.MAIN);

        while (have(Token.Kind.INT) || have(Token.Kind.FLOAT) || have(Token.Kind.BOOL)) {
            varDecl();
        }

        while (have(Token.Kind.FUNC)) {
            funcDecl();
        }
        
        expect(Token.Kind.OPEN_BRACE);
        statSeq();
        expect(Token.Kind.CLOSE_BRACE);

        expect(Token.Kind.PERIOD);  
        expect(Token.Kind.EOF);     
    }
    
    private void funcDecl() {
        expect(Token.Kind.FUNC);
        expect(Token.Kind.IDENT);
        formalParam();
        expect(Token.Kind.COLON);
        if (!have(Token.Kind.VOID)) {
            type();
        }
        else {
            expect(Token.Kind.VOID);
        }
        funcBody();
    }

    private void funcBody() {
        expect(Token.Kind.OPEN_BRACE);
        if (have(Token.Kind.INT) || have(Token.Kind.FLOAT) || have(Token.Kind.BOOL)) {
            varDecl();
        }
        statSeq();
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.SEMICOLON);
    }

    private void formalParam() {
        expect(Token.Kind.OPEN_PAREN);
        do {
            paramDecl();
        } while (accept(Token.Kind.COMMA));
        expect(Token.Kind.CLOSE_PAREN);
    }

    private void paramDecl() {
        paramType();
        expect(Token.Kind.IDENT);
    }

    private void paramType() {
        type();
        while (have(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.OPEN_BRACKET);
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }

    private void varDecl() {
        typeDecl();
        do {
            expect(Token.Kind.IDENT);
        } while (accept(Token.Kind.COMMA));
        expect(Token.Kind.SEMICOLON);
    }
    
    private void typeDecl() {
        type();
        while (have(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.OPEN_BRACKET);
            expect(Token.Kind.INT_VAL);
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }

    private void statSeq() {
        do {
            statement();     
            // TODO: Case3, calling again on the else, call until found fi and od
        } while (accept(Token.Kind.SEMICOLON));
    }

    private void statement() {
        // System.out.println("token: " + currentToken.lexeme() + ", " + currentToken.kind());
        if (have(Token.Kind.IDENT)) {
            assign();
        } else if (have(Token.Kind.CALL)) {
            funcCall();
        } else if (have(Token.Kind.IF)) {
            ifStat();
        } else if (have(Token.Kind.WHILE)) {
            whileStat();
        } else if (have(Token.Kind.REPEAT)) {
            repeatStat();
        } else if (have(Token.Kind.RETURN)) {
            returnStat();
        } else if (have(Token.Kind.SEMICOLON)) {
            String errorMessage = reportSyntaxError(NonTerminal.STATEMENT);
            throw new QuitParseException(errorMessage);
        } else {

        }   
    }

    private void returnStat() {
        expect(Token.Kind.RETURN);
        while (!have(Token.Kind.SEMICOLON)) {
            relExpr();
        }
    }

    private void repeatStat() {
        expect(Token.Kind.REPEAT);
        statSeq();
        expect(Token.Kind.UNTIL);
        relation();
    }

    private void whileStat() {
        expect(Token.Kind.WHILE);
        relation();
        expect(Token.Kind.DO);
        statSeq();
        expect(Token.Kind.OD);
    }

    private void ifStat() {
        expect(Token.Kind.IF);
        relation();
        expect(Token.Kind.THEN);
        statSeq();

        if (have(Token.Kind.ELSE)) {
            expect(Token.Kind.ELSE);
            statSeq();
        }
        expect(Token.Kind.FI);
    }

    private void funcCall() {
        expect(Token.Kind.CALL);
        expect(Token.Kind.IDENT);
        expect(Token.Kind.OPEN_PAREN);

        if (!have(Token.Kind.CLOSE_PAREN)) {
            do {
                relExpr();
            } while (accept(Token.Kind.COMMA));    
        }
        
        expect(Token.Kind.CLOSE_PAREN);
    }

    private void assign() {
        designator();
        if (acceptAssignOp()) {
            relExpr();
        } else {
            unaryOp();
        }
    }

    private void relation() {
        expect(Token.Kind.OPEN_PAREN);
        relExpr();
        expect(Token.Kind.CLOSE_PAREN);
    }

    private void relExpr() {
        addExpr();
        while (acceptRelOp()) {
            addExpr();
        }
    }

    private void addExpr() {
        multExpr();
        while (acceptAddOp()) {
            multExpr();
        }
    }

    private void multExpr() {
        powExpr();
        while (acceptMulOp()) {
            powExpr();
        }
    }

    private void powExpr() {
        groupExpr();
        while (acceptPowOp()) {
            groupExpr();
        }
    }

    private void groupExpr() {
        if (have(Token.Kind.IDENT)) {
            designator();
        } else if (have(Token.Kind.NOT)) {
            expect(Token.Kind.NOT);
            relExpr();
        } else if (have(Token.Kind.OPEN_PAREN)) {
            relation();
        } else if (have(Token.Kind.CALL)) {
            funcCall();
        } else if (have(Token.Kind.MUL)) {
            String errorMessage = reportSyntaxError(NonTerminal.GROUP_EXPR);
            throw new QuitParseException(errorMessage);
        } else {
            literal();
        }
    }
}

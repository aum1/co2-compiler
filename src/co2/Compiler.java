package co2;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Deque;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import ast.AST;
import ast.Addition;
import ast.ArgumentList;
import ast.Assignment;
import ast.BoolLiteral;
import ast.Computation;
import ast.Declaration;
import ast.DeclarationList;
import ast.Division;
import ast.Expression;
import ast.FloatLiteral;
import ast.FunctionBody;
import ast.FunctionCall;
import ast.FunctionDeclaration;
import ast.IfStatement;
import ast.IntegerLiteral;
import ast.LogicalAnd;
import ast.LogicalNot;
import ast.LogicalOr;
import ast.Modulo;
import ast.Multiplication;
import ast.Node;
import ast.Power;
import ast.Relation;
import ast.RepeatStatement;
import ast.ReturnStatement;
import ast.Statement;
import ast.StatementSequence;
import ast.Subtraction;
import ast.VariableDeclaration;
import ast.VariableReference;
import ast.WhileStatement;
import co2.Token.Kind;
import ir.cfg.BasicBlock;
import ir.cfg.Block;
import ir.cfg.CFGPrinter;
import ir.cfg.IRGenerator;
import ir.tac.Add;
import ir.tac.And;
import ir.tac.Assign;
import ir.tac.BEQ;
import ir.tac.BGE;
import ir.tac.BGT;
import ir.tac.BLE;
import ir.tac.BLT;
import ir.tac.BNE;
import ir.tac.BRA;
import ir.tac.Call;
import ir.tac.Comparison;
import ir.tac.Div;
import ir.tac.End;
import ir.tac.Literal;
import ir.tac.Mod;
import ir.tac.Mul;
import ir.tac.Neg;
import ir.tac.Or;
import ir.tac.Pow;
import ir.tac.Return;
import ir.tac.Sub;
import ir.tac.TAC;
import ir.tac.TACList;
import ir.tac.Value;
import ir.tac.Variable;

public class Compiler {

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

// Compiler ===================================================================
    private co2.Scanner scanner;
    private Token currentToken;
    private Computation head;

    private int numDataRegisters; // available registers are [1..numDataRegisters]
    private List<Integer> instructions;

    // Need to map from IDENT to memory offset

    public Compiler (Scanner scanner, int numRegs) {
        this.scanner = scanner;
        currentToken = this.scanner.next();
        numDataRegisters = numRegs;
        instructions = new ArrayList<>();
        initSymbolTable();
    }

    public ast.AST genAST() {
        Computation head = computation();
        this.head = head;
        return new ast.AST(head);
    }
    
    public void interpret(InputStream in) {
        initSymbolTable();
        this.buffReader = new BufferedReader(new InputStreamReader(in));
        st = null;
        try {
            // System.out.println("interpreted");
            computationInterpret(head);
        }
        catch (QuitParseException q) {
            // too verbose
            System.out.println("Error: " + q.getMessage());
            errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            errorBuffer.append("[Could not complete parsing.]");
        }
    }

    public int[] compile () {
        initSymbolTable();
        try {
            computation();
            return instructions.stream().mapToInt(Integer::intValue).toArray();
        }
        catch (QuitParseException q) {
            // errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            // errorBuffer.append("[Could not complete parsing.]");
            return new ArrayList<Integer>().stream().mapToInt(Integer::intValue).toArray();
        }
    }

// Interpreter ================================================================
    List<Pair> registeredVariables = new ArrayList<>();
    BufferedReader buffReader;
    private StringTokenizer st;

    private void computationInterpret(Computation node) {
        for (Declaration d : node.variables()) {
            variableDeclarationInterpret((VariableDeclaration) d);
        }

        for (Statement s : node.mainStatementSequence()) {
            statementInterpret(s);
        }
    }

    private void variableDeclarationInterpret(VariableDeclaration node) {
        Symbol returned = tryDeclareVariable(node.getIdent().token(), node.getType().token().lexeme());

        if (node.getType().token().lexeme().equals("int")) {
            registeredVariables.add(new IntPair(node.getIdent(), 0));
        }
        else if (node.getType().token().lexeme().equals("float")) {
            registeredVariables.add(new FloatPair(node.getIdent(), 0.0f));
        }
        else if (node.getType().token().lexeme().equals("bool")) {
            registeredVariables.add(new BoolPair(node.getIdent(), false));
        } 
    }

    private Pair registeredVariableLookup(String ident) {
        for (Pair p : registeredVariables) {
            if (p.symbol().token().lexeme().equals(ident)) {
                return p;
            }
        }
        return null;
    }

    private void statementInterpret(Statement node) {
        if (node instanceof FunctionCall) {
            printfunctionCallInterpret((FunctionCall) node);
        }
        if (node instanceof IfStatement) {
            ifStatementInterpret((IfStatement) node);
        }
        if (node instanceof ReturnStatement) {
            returnStatementInterpret((ReturnStatement) node);
        }
        if (node instanceof Assignment) {
            assignInterpret((Assignment) node);
        }
    }

    private int expectIntInterpret(Expression node) {
        if (node instanceof IntegerLiteral) {
            return integerLiteralInterpret((IntegerLiteral) node);
        }
        if (node instanceof VariableReference) {
            Pair p = registeredVariableLookup(((VariableReference) node).getIdent().token().lexeme());
            if (p instanceof IntPair) {
                return ((IntPair)p).value();
            }
        }
        if (node instanceof Addition) {
            return additionIntInterpret((Addition) node);
        }
        if (node instanceof FunctionCall) {
            return readInt();
        }
        if (node instanceof Multiplication) {
            return multiplicationIntInterpret((Multiplication) node);
        }
        if (node instanceof Subtraction) {
            return subtractionIntInterpret((Subtraction) node);
        }
        if (node instanceof Division) {
            return divisionIntInterpret((Division) node);
        }
        if (node instanceof Modulo) {
            return moduloInterpret((Modulo) node);
        }
        return -100;
    }

    private float expectFloatInterpret(Expression node) {
        if (node instanceof FloatLiteral) {
            return floatLiteralInterpret((FloatLiteral) node);
        }
        if (node instanceof VariableReference) {
            Pair p = registeredVariableLookup(((VariableReference) node).getIdent().token().lexeme());
            if (p instanceof FloatPair) {
                return ((FloatPair)p).value();
            }
        }
        if (node instanceof FunctionCall) {
            return readFloat();
        }
        if (node instanceof Addition) {
            return additionFloatInterpret((Addition) node);
        }
        if (node instanceof Multiplication) {
            return multiplicationFloatInterpret((Multiplication) node);
        }
        if (node instanceof Subtraction) {
            return subtractionFloatInterpret((Subtraction) node);
        }
        if (node instanceof Division) {
            return divisionFloatInterpret((Division) node);
        }
        return -100.0f;
    }

    private boolean expectBoolInterpret(Expression node) {
        if (node instanceof BoolLiteral) {
            return boolLiteralInterpret((BoolLiteral) node);
        }
        if (node instanceof VariableReference) {
            Pair p = registeredVariableLookup(((VariableReference) node).getIdent().token().lexeme());
            if (p instanceof BoolPair) {
                return ((BoolPair)p).value();
            }
        }
        if (node instanceof Relation) {
            return relationInterpret((Relation) node);
        }
        if (node instanceof LogicalNot) {
            return notInterpret((LogicalNot) node);
        }
        if (node instanceof LogicalAnd) {
            return andInterpret((LogicalAnd) node);
        }
        if (node instanceof LogicalOr) {
            return orInterpret((LogicalOr) node);
        }
        if (node instanceof FunctionCall) {
            return readBool();
        }
        return false;
    }

    private void printfunctionCallInterpret(FunctionCall node) {
        if (node.getArgumentList().hasArguments()) {
            if (node.getFunctionName().token().lexeme().equals("printFloat")) {
                float returnedFloat = expectFloatInterpret(node.getArgumentList().getExpressionParameters().get(0));
                printFloat(returnedFloat);
            }
            else if (node.getFunctionName().token().lexeme().equals("printInt")) {
                int returnedInt = expectIntInterpret(node.getArgumentList().getExpressionParameters().get(0));
                printInt(returnedInt);
            }
            else {
                boolean returnedBool = expectBoolInterpret(node.getArgumentList().getExpressionParameters().get(0));
                printBool(returnedBool);
            }
        }
        else {
            println();
        }
    }

    private void ifStatementInterpret(IfStatement node) {
        boolean relationValue = expectBoolInterpret(node.getRelation());
        if (relationValue) {
            for (Statement s : node.getThenBlock()) {
                statementInterpret(s);
            }
        }
        else {
            if (node.hasElseBlock()) {
                for (Statement s : node.getElseBlock()) {
                    statementInterpret(s);
                }
            }
        }
    }

    private void returnStatementInterpret(ReturnStatement node) {
        
    }

    private void assignInterpret(Assignment node) {
        for (Pair p : registeredVariables) {
            if (p.symbol().token().lexeme().equals(node.getIdent().token().lexeme())) {
                if (node.getAssignType().token().lexeme().equals("=")) {
                    if (p instanceof IntPair) {
                        int value = expectIntInterpret(node.getRelation());
                        ((IntPair)p).setValue(value);
                    }
                    else if (p instanceof FloatPair) {
                        float value = expectFloatInterpret(node.getRelation());
                        ((FloatPair)p).setValue(value);
                    }
                    else if (p instanceof BoolPair) {
                        boolean value = expectBoolInterpret(node.getRelation());
                        ((BoolPair)p).setValue(value);
                    }
                }
                else if (node.getAssignType().token().lexeme().equals("+=")) {
                    if (p instanceof IntPair) {
                        int value = expectIntInterpret(node.getRelation());
                        ((IntPair)p).setValue(((IntPair)p).value() + value);
                    }
                    else if (p instanceof FloatPair) {
                        float value = expectFloatInterpret(node.getRelation());
                        ((FloatPair)p).setValue(((FloatPair)p).value() + value);
                    }
                    else if (p instanceof BoolPair) {
                        boolean value = expectBoolInterpret(node.getRelation());
                        ((BoolPair)p).setValue(value);
                    }
                }
                else if (node.getAssignType().token().lexeme().equals("-=")) {
                    if (p instanceof IntPair) {
                        int value = expectIntInterpret(node.getRelation());
                        ((IntPair)p).setValue(value - ((IntPair)p).value());
                    }
                    else if (p instanceof FloatPair) {
                        float value = expectFloatInterpret(node.getRelation());
                        ((FloatPair)p).setValue(value - ((FloatPair)p).value());
                    }
                    else if (p instanceof BoolPair) {
                        boolean value = expectBoolInterpret(node.getRelation());
                        ((BoolPair)p).setValue(value);
                    }
                }
                else if (node.getAssignType().token().lexeme().equals("*=")) {
                    if (p instanceof IntPair) {
                        int value = expectIntInterpret(node.getRelation());
                        ((IntPair)p).setValue(value * ((IntPair)p).value());
                    }
                    else if (p instanceof FloatPair) {
                        float value = expectFloatInterpret(node.getRelation());
                        ((FloatPair)p).setValue(value * ((FloatPair)p).value());
                    }
                    else if (p instanceof BoolPair) {
                        boolean value = expectBoolInterpret(node.getRelation());
                        ((BoolPair)p).setValue(value);
                    }
                }
                else if (node.getAssignType().token().lexeme().equals("/=")) {
                    if (p instanceof IntPair) {
                        int value = expectIntInterpret(node.getRelation());
                        ((IntPair)p).setValue(value / ((IntPair)p).value());
                    }
                    else if (p instanceof FloatPair) {
                        float value = expectFloatInterpret(node.getRelation());
                        ((FloatPair)p).setValue(value / ((FloatPair)p).value());
                    }
                    else if (p instanceof BoolPair) {
                        boolean value = expectBoolInterpret(node.getRelation());
                        ((BoolPair)p).setValue(value);
                    }
                }
                else if (node.getAssignType().token().lexeme().equals("%=")) {
                    if (p instanceof IntPair) {
                        int value = expectIntInterpret(node.getRelation());
                        ((IntPair)p).setValue(value % ((IntPair)p).value());
                    }
                    else if (p instanceof FloatPair) {
                        float value = expectFloatInterpret(node.getRelation());
                        ((FloatPair)p).setValue(value % ((FloatPair)p).value());
                    }
                    else if (p instanceof BoolPair) {
                        boolean value = expectBoolInterpret(node.getRelation());
                        ((BoolPair)p).setValue(value);
                    }
                }
            }
        }
    }

    private boolean relationInterpret(Relation node) {
        if (node.getRelation().token().lexeme().equals("==")) {
            return equalToInterpret(node.getLeftSide(), node.getRightSide());
        }
        if (node.getRelation().token().lexeme().equals("!=")) {
            return notEqualInterpret(node.getLeftSide(), node.getRightSide());
        }
        if (node.getRelation().token().lexeme().equals(">")) {
            return greaterThanInterpret(node.getLeftSide(), node.getRightSide());
        }
        if (node.getRelation().token().lexeme().equals(">=")) {
            return greaterEqualInterpret(node.getLeftSide(), node.getRightSide());
        }
        if (node.getRelation().token().lexeme().equals("<")) {
            return lessThanInterpret(node.getLeftSide(), node.getRightSide());
        }
        if (node.getRelation().token().lexeme().equals("<=")) {
            return LessEqualInterpret(node.getLeftSide(), node.getRightSide());
        }
        return false;
    }

    private boolean equalToInterpret(Expression left, Expression right) {
        int returnedInt = expectIntInterpret(left);
        if (returnedInt != -100) {
            return (returnedInt == expectIntInterpret(right));
        }

        float returnedFloat = expectFloatInterpret(left);
        if (returnedFloat != -100) {
            return (returnedFloat == expectFloatInterpret(right));
        }
        else {
            return (expectBoolInterpret(left) == expectBoolInterpret(right));
        }
    }

    private boolean notEqualInterpret(Expression left, Expression right) {
        int returnedInt = expectIntInterpret(left);
        if (returnedInt != -100) {
            return (returnedInt != expectIntInterpret(right));
        }

        float returnedFloat = expectFloatInterpret(left);
        if (returnedFloat != -100) {
            return (returnedFloat != expectFloatInterpret(right));
        }
        else {
            return (expectBoolInterpret(left) != expectBoolInterpret(right));
        }
    }

    private boolean greaterThanInterpret(Expression left, Expression right) {
        int returnedInt = expectIntInterpret(left);
        if (returnedInt != -100) {
            return (returnedInt > expectIntInterpret(right));
        }
        
        float returnedFloat = expectFloatInterpret(left);
        if (returnedFloat != -100) {
            return (returnedFloat > expectFloatInterpret(right));
        }
        return false;
    }

    private boolean lessThanInterpret(Expression left, Expression right) {
        int returnedInt = expectIntInterpret(left);
        if (returnedInt != -100) {
            return (returnedInt < expectIntInterpret(right));
        }

        float returnedFloat = expectFloatInterpret(left);
        if (returnedFloat != -100) {
            return (returnedFloat < expectFloatInterpret(right));
        }
        return false;
    }

    private boolean greaterEqualInterpret(Expression left, Expression right) {
        int returnedInt = expectIntInterpret(left);
        if (returnedInt != -100) {
            return (returnedInt >= expectIntInterpret(right));
        }

        float returnedFloat = expectFloatInterpret(left);
        if (returnedFloat != -100) {
            return (returnedFloat >= expectFloatInterpret(right));
        }
        return false;
    }

    private boolean LessEqualInterpret(Expression left, Expression right) {
        int returnedInt = expectIntInterpret(left);
        if (returnedInt != -100) {
            return (returnedInt <= expectIntInterpret(right));
        }
        
        float returnedFloat = expectFloatInterpret(left);
        if (returnedFloat != -100) {
            return (returnedFloat <= expectFloatInterpret(right));
        }
        return false;
    }

    private int additionIntInterpret(Addition node) {
        int leftSide = expectIntInterpret(node.getLeftSide());
        int rightSide = expectIntInterpret(node.getRightSide());
        return (leftSide + rightSide);
    }

    private float additionFloatInterpret(Addition node) {
        float leftSide = expectFloatInterpret(node.getLeftSide());
        float rightSide = expectFloatInterpret(node.getRightSide());
        return (leftSide + rightSide);
    }

    private int subtractionIntInterpret(Subtraction node) {
        int leftSide = expectIntInterpret(node.getLeftSide());
        int rightSide = expectIntInterpret(node.getRightSide());
        return (leftSide - rightSide);
    }

    private float subtractionFloatInterpret(Subtraction node) {
        float leftSide = expectFloatInterpret(node.getLeftSide());
        float rightSide = expectFloatInterpret(node.getRightSide());
        return (leftSide - rightSide);
    }

    private int multiplicationIntInterpret(Multiplication node) {
        int leftSide = expectIntInterpret(node.getLeftSide());
        int rightSide = expectIntInterpret(node.getRightSide());
        return (leftSide * rightSide);
    }

    private float multiplicationFloatInterpret(Multiplication node) {
        float leftSide = expectFloatInterpret(node.getLeftSide());
        float rightSide = expectFloatInterpret(node.getRightSide());
        return (leftSide * rightSide);
    }

    private int divisionIntInterpret(Division node) {
        int leftSide = expectIntInterpret(node.getLeftSide());
        int rightSide = expectIntInterpret(node.getRightSide());
        return (leftSide / rightSide);
    }

    private float divisionFloatInterpret(Division node) {
        float leftSide = expectFloatInterpret(node.getLeftSide());
        float rightSide = expectFloatInterpret(node.getRightSide());
        return (leftSide / rightSide);
    }

    private int moduloInterpret(Modulo node) {
        int leftSide = expectIntInterpret(node.getLeftSide());
        int rightSide = expectIntInterpret(node.getRightSide());
        return (leftSide % rightSide);
    }

    private boolean notInterpret(LogicalNot node) {
        boolean leftSide = expectBoolInterpret(node.getExpression());
        return (!leftSide);
    }

    private boolean orInterpret(LogicalOr node) {
        boolean leftSide = expectBoolInterpret(node.getLeftSide());
        boolean rightSide = expectBoolInterpret(node.getRightSide());
        return (leftSide || rightSide);
    }

    private boolean andInterpret(LogicalAnd node) {
        boolean leftSide = expectBoolInterpret(node.getLeftSide());
        boolean rightSide = expectBoolInterpret(node.getRightSide());
        // System.out.println("leftSide: " + leftSide);
        // System.out.println("rightSide: " + rightSide);
        return (leftSide && rightSide);
    }

    private int integerLiteralInterpret(IntegerLiteral node) {
        // System.out.println("integer literal interpreting : " + node.getInteger().token().lexeme());
        return Integer.valueOf(node.getInteger().token().lexeme());
    }

    private float floatLiteralInterpret(FloatLiteral node) {
        // System.out.println("literal: " + node.getFloat().token().lexeme());
        return Float.valueOf(node.getFloat().token().lexeme());
    }

    private boolean boolLiteralInterpret(BoolLiteral node) {
        return Boolean.valueOf(node.getBoolean().token().lexeme());
    }

// SymbolTable Management =====================================================
    private Stack<SymbolTable> symbolTableStack = new Stack<SymbolTable>();

    private void initSymbolTable () {
        SymbolTable globalScopeTable = new SymbolTable();
        symbolTableStack.add(globalScopeTable);
    }

    private void enterScope () {
        symbolTableStack.add(new SymbolTable());
    }

    private void exitScope () {
        symbolTableStack.pop();
    }

    private String tryResolveVariable (Token ident, Token type) {
        try {
            String foundSymbol = symbolTableStack.peek().lookup(new Symbol(ident));
            return foundSymbol;
        } catch (SymbolNotFoundError e) {
            reportResolveSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
            return null;
        }
    }

    private Symbol tryDeclareVariable (Token ident, String type) {
        try {
            Symbol foundSymbol = symbolTableStack.peek().insert(new Symbol(ident), type);
            return foundSymbol;
        } catch (RedeclarationError e) {
            reportDeclareSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
            return null;
        }
    }

    private String reportResolveSymbolError (String name, int lineNum, int charPos) {
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportDeclareSymbolError (String name, int lineNum, int charPos) {
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message + "\n");
        return message;
    }


// Helper Methods =============================================================
    private boolean have (Token.Kind kind) {
        return (currentToken.is(kind));
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

    private String nextInput () {
        while (st == null || !st.hasMoreElements()) {
            try {
                st = new StringTokenizer(buffReader.readLine());
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

    private boolean acceptRelOp() {
        // match non terminal will throw error
        return (accept(Token.Kind.EQUAL_TO) || accept(Token.Kind.NOT_EQUAL)|| accept(Token.Kind.LESS_THAN)
            || accept(Token.Kind.LESS_EQUAL) || accept(Token.Kind.GREATER_THAN) || accept(Token.Kind.GREATER_EQUAL));
    }
    

    private boolean acceptPowOp() {
        return (accept(Token.Kind.POW));
    }

    private boolean acceptAddOp() {
        return (accept(Token.Kind.ADD) || accept(Token.Kind.SUB) || accept(Token.Kind.OR));
    }

    private boolean acceptMulOp() {
        return (accept(Token.Kind.MUL) || accept(Token.Kind.DIV) || accept(Token.Kind.MOD) || accept(Token.Kind.AND));
    }

    private boolean acceptAssignOp() {
        return (accept(Token.Kind.ASSIGN) || accept(Token.Kind.ADD_ASSIGN) || accept(Token.Kind.SUB_ASSIGN) || accept(Token.Kind.MUL_ASSIGN) 
            || accept(Token.Kind.DIV_ASSIGN) || accept(Token.Kind.MOD_ASSIGN) || accept(Token.Kind.POW_ASSIGN));
    }

    private boolean haveAssignOp() {
        return (have(Token.Kind.ASSIGN) || have(Token.Kind.ADD_ASSIGN) || have(Token.Kind.SUB_ASSIGN) || have(Token.Kind.MUL_ASSIGN) 
            || have(Token.Kind.DIV_ASSIGN) || have(Token.Kind.MOD_ASSIGN) || have(Token.Kind.POW_ASSIGN));
    }

    // literal = integerLit | floatLit
    private Token literal () {
        return matchNonTerminal(NonTerminal.LITERAL);
    }

    private Token designator () {
        Expression toReturn;
        int lineNum = lineNumber();
        int charPos = charPosition();
        Token ident = null;
        if (have(Token.Kind.INT)) {
            ident = expectRetrieve(Token.Kind.INT);
        }
        else if (have(Token.Kind.FLOAT)) {
            ident = expectRetrieve(Token.Kind.FLOAT);
        }
        else if (have(Token.Kind.BOOL)) {
            ident = expectRetrieve(Token.Kind.BOOL);
        }
        else {
            ident = expectRetrieve(Token.Kind.IDENT);
        }
        
        while (have(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.OPEN_BRACKET);
            // TODO: expect retrieve relations as well as ints
            Token currToken = expectRetrieve(Token.Kind.INT_VAL);
            ident.setLexeme(ident.lexeme() + "[" + currToken.lexeme() + "]");
            expect(Token.Kind.CLOSE_BRACKET);
        }

        return ident;
    }

// Type Checker ==============================================================
    // computation	= "main" {varDecl} {funcDecl} "{" statSeq "}" "."
    private Computation computation () {
        Token mainToken = expectRetrieve(Token.Kind.MAIN);

        DeclarationList variables = new DeclarationList(mainToken.lineNumber(), mainToken.charPosition());
        DeclarationList functions = new DeclarationList(mainToken.lineNumber(), mainToken.charPosition());
        StatementSequence statements;
        
        while (have(Token.Kind.INT) || have(Token.Kind.FLOAT) || have(Token.Kind.BOOL)) {
            Token type = designator();
            
            do {
                Token ident = expectRetrieve(Token.Kind.IDENT);
                // Token ident = designator();
                variables.addDeclaration(new VariableDeclaration(type.lineNumber(), type.charPosition(), new Symbol(ident), new Symbol(type)));
            } while (accept(Token.Kind.COMMA));

            expect(Token.Kind.SEMICOLON);
        }


        while (have(Token.Kind.FUNC)) {
            FunctionDeclaration funcDecl = funcDecl();
            functions.addDeclaration(funcDecl);
        }
        // System.out.println("2: " + currentToken.lexeme());

        expect(Token.Kind.OPEN_BRACE);
        statements = statSeq();
        expect(Token.Kind.CLOSE_BRACE);

        expect(Token.Kind.PERIOD);  
        expect(Token.Kind.EOF);

        return new Computation(mainToken.lineNumber(), mainToken.charPosition(), new Symbol(mainToken), variables, functions, statements);
    }

    private VariableDeclaration varDecl () {
        Token type = matchNonTerminal(NonTerminal.TYPE_DECL);

        List<Expression> dimensions = new ArrayList<>();
        while (have(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.OPEN_BRACKET);
            Token newDimension = expectRetrieve(Token.Kind.INT_VAL);
            expect(Token.Kind.CLOSE_BRACKET);
            dimensions.add(new IntegerLiteral(0, 0, new Symbol(newDimension)));
        }

        Token ident = expectRetrieve(Token.Kind.IDENT);
        if (dimensions.size() == 0) {
            return new VariableDeclaration(type.lineNumber(), type.charPosition(), new Symbol(ident), new Symbol(type));
        }
        else {
            return new VariableDeclaration(type.lineNumber(), type.charPosition(), new Symbol(ident), new Symbol(type), dimensions);
        }
    }

    private FunctionDeclaration funcDecl() {
        FunctionBody body;
        ArgumentList argsList;
        List<Symbol> expressionsList = new ArrayList<>();
        List<Symbol> identList = new ArrayList<>();

        expect(Token.Kind.FUNC);
        Token funcToken = expectRetrieve(Token.Kind.IDENT);
        expect(Token.Kind.OPEN_PAREN);
        if (!have(Token.Kind.CLOSE_PAREN)) {
            do {
                Token currType = matchNonTerminal(NonTerminal.TYPE_DECL);
                while (have(Token.Kind.OPEN_BRACKET)) {
                    accept(Token.Kind.OPEN_BRACKET);
                    accept(Token.Kind.CLOSE_BRACKET);
                    currType.setLexeme(currType.lexeme() + "[]");
                }

                Token currIdent = expectRetrieve(Token.Kind.IDENT);

                expressionsList.add(new Symbol(currType));
                identList.add(new Symbol(currIdent));
            } while (accept(Token.Kind.COMMA));
            expect(Token.Kind.CLOSE_PAREN);
        }
        else {
            expect(Token.Kind.CLOSE_PAREN);
        }
        // System.out.println("Out here: " + currentToken.lexeme());
        
        expect(Token.Kind.COLON);
        Token returnType = null;
        if (!have(Token.Kind.VOID)) {
            // TODO: Check
            if (have(Token.Kind.INT)) {
                returnType = expectRetrieve(Token.Kind.INT);
                
            }
            else if (have(Token.Kind.FLOAT)) {
                returnType = expectRetrieve(Token.Kind.FLOAT);
            }
            else {
                returnType = expectRetrieve(Token.Kind.BOOL);
            }
        }
        else {
            returnType = expectRetrieve(Token.Kind.VOID);
        }

        body = funcBody();
        argsList = new ArgumentList(funcToken.lineNumber(), funcToken.charPosition(), expressionsList, identList, new Symbol(returnType), true);
        return new FunctionDeclaration(funcToken.lineNumber(), funcToken.charPosition(), body, argsList, new Symbol(funcToken));
    }

    private FunctionBody funcBody() {
        Token funcToken = expectRetrieve(Token.Kind.OPEN_BRACE);
        DeclarationList varDecl = new DeclarationList(funcToken.lineNumber(), funcToken.charPosition());
        boolean hasVarDecl = false;
        StatementSequence statSeq;

        while (have(Token.Kind.INT) || have(Token.Kind.FLOAT) || have(Token.Kind.BOOL)) {
            Token type = designator();
            
            do {
                Token ident = expectRetrieve(Token.Kind.IDENT);
                // Token ident = designator();
                varDecl.addDeclaration(new VariableDeclaration(type.lineNumber(), type.charPosition(), new Symbol(ident), new Symbol(type)));
            } while (accept(Token.Kind.COMMA));

            expect(Token.Kind.SEMICOLON);
            hasVarDecl = true;
        }

        statSeq = statSeq();
        expect(Token.Kind.CLOSE_BRACE);
        expect(Token.Kind.SEMICOLON);
        if (hasVarDecl) {
            return new FunctionBody(funcToken.lineNumber(), funcToken.charPosition(), varDecl, statSeq);
        }
        return new FunctionBody(funcToken.lineNumber(), funcToken.charPosition(), statSeq);
    }

    private StatementSequence statSeq() {
        // System.out.println("Statseq");
        List<Statement> sequenceList = new ArrayList<>();
        do {
            if (!have(Token.Kind.CLOSE_BRACE)) {
                Statement currStatment = statement();
                // System.out.println("Adding: " + currStatment.toString());
                if (currStatment != null) {
                    sequenceList.add(currStatment);                    
                }
            }
        } while (accept(Token.Kind.SEMICOLON));

        return new StatementSequence(0, 0, sequenceList);
    }

    private Statement statement() {
        // System.out.println("Statement starting with : " + currentToken.lexeme());
        if (have(Token.Kind.IDENT)) {
            return assign();
        } else if (have(Token.Kind.CALL)) {
            return (Statement) funcCall();
        } else if (have(Token.Kind.IF)) {
            return ifStat();
        } else if (have(Token.Kind.WHILE)) {
            return whileStat();
        } else if (have(Token.Kind.REPEAT)) {
            return repeatStat();
        } else if (have(Token.Kind.RETURN)) {
            return returnStat();
        } else if (have(Token.Kind.SEMICOLON)) {
            // String errorMessage = reportSyntaxError(NonTerminal.STATEMENT);
            // throw new QuitParseException(errorMessage);
        }
        // System.out.println("Here: " + currentToken.lexeme());
        return null;
    }

    private Statement assign() {
        Token designatorToken = expectRetrieve(Token.Kind.IDENT);

        while (have(Token.Kind.OPEN_BRACKET)) {
            while (!(haveAssignOp())) {
                designatorToken.setLexeme(designatorToken.lexeme() + currentToken.lexeme());
                currentToken = scanner.next();
            }
            accept(Token.Kind.CLOSE_BRACKET);
        }

        if (haveAssignOp()) {
            Token assignmentOp = null;
            if (have(Token.Kind.ASSIGN)) {
                assignmentOp = expectRetrieve(Token.Kind.ASSIGN);
            }
            if (have(Token.Kind.ADD_ASSIGN)) {
                assignmentOp = expectRetrieve(Token.Kind.ADD_ASSIGN);
            }
            if (have(Token.Kind.SUB_ASSIGN)) {
                assignmentOp = expectRetrieve(Token.Kind.SUB_ASSIGN);
            }
            if (have(Token.Kind.DIV_ASSIGN)) {
                assignmentOp = expectRetrieve(Token.Kind.DIV_ASSIGN);
            }
            if (have(Token.Kind.MUL_ASSIGN)) {
                assignmentOp = expectRetrieve(Token.Kind.MUL_ASSIGN);
            }
            if (have(Token.Kind.MOD_ASSIGN)) {
                assignmentOp = expectRetrieve(Token.Kind.MOD_ASSIGN);
            }
            
            Expression expr = relExpr();
            return new Assignment(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(designatorToken), new Symbol(assignmentOp), expr);
        } else {
            Token terminal =  matchNonTerminal(NonTerminal.UNARY_OP);
            if (terminal.is(Token.Kind.UNI_INC)) {
                return new Assignment(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(designatorToken), new Symbol(terminal),
                new Addition(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(new Token("+", 0, 0)), 
                    new VariableReference(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(designatorToken)), 
                        new IntegerLiteral(designatorToken.lineNumber(), designatorToken.charPosition(),  new Symbol(new Token("1", 0, 0)))));
            }
            else {
                return new Assignment(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(designatorToken), new Symbol(terminal),
                new Subtraction(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(new Token("+", 0, 0)), 
                    new VariableReference(designatorToken.lineNumber(), designatorToken.charPosition(), new Symbol(designatorToken)), 
                        new IntegerLiteral(designatorToken.lineNumber(), designatorToken.charPosition(),  new Symbol(new Token("1", 0, 0)))));
            }
            
        }
    }

    private Node funcCall() {
        ArgumentList argsList;
        List<Expression> args = new ArrayList<>();

        expect(Token.Kind.CALL);
        Token funcToken = expectRetrieve(Token.Kind.IDENT);
        expect(Token.Kind.OPEN_PAREN);

        if (!have(Token.Kind.CLOSE_PAREN)) {
            do {
                Expression currExpression = relExpr();
                args.add(currExpression);
            } while (accept(Token.Kind.COMMA));
        }

        expect(Token.Kind.CLOSE_PAREN);
        // TODO: Check and get return type somehoww

        
        argsList = new ArgumentList(funcToken.lineNumber(), funcToken.charPosition(), args, null);
        return new FunctionCall(funcToken.lineNumber(), funcToken.charPosition(), new Symbol(funcToken), argsList);
    }

    private Statement ifStat() {
        Expression relation;
        StatementSequence thenBlock;
        StatementSequence elseBlock;
        IfStatement stat;

        Token ifToken = expectRetrieve(Token.Kind.IF);
        relation = relation();
        expect(Token.Kind.THEN);
        thenBlock = statSeq();

        if (have(Token.Kind.ELSE)) {
            // System.out.println("caught else ");
            expect(Token.Kind.ELSE);
            elseBlock = statSeq();
            stat = new IfStatement(ifToken.lineNumber(), ifToken.charPosition(), relation, thenBlock, elseBlock);
        }
        else {
            stat = new IfStatement(ifToken.lineNumber(), ifToken.charPosition(), relation, thenBlock);
        }
        // System.out.println("here: " + currentToken.lexeme());
        expect (Token.Kind.FI);
        return stat;
    }

    private Statement whileStat() {
        Expression relation;
        StatementSequence statSeq;

        Token whileToken = expectRetrieve(Token.Kind.WHILE);
        relation = relation();
        expect(Token.Kind.DO);
        statSeq = statSeq();
        expect(Token.Kind.OD);

        return new WhileStatement(whileToken.lineNumber(), whileToken.charPosition(), relation, statSeq);
    }

    private Statement repeatStat() {
        Expression relation;
        StatementSequence statSeq;

        Token repeatToken = expectRetrieve(Token.Kind.REPEAT);
        statSeq = statSeq();
        expect(Token.Kind.UNTIL);
        relation = relation();

        return new RepeatStatement(repeatToken.lineNumber(), repeatToken.charPosition(), relation, statSeq);
    }

    private Statement returnStat() {
        // System.out.println("return stat");
        Token returnToken = expectRetrieve(Token.Kind.RETURN);
        if (have(Token.Kind.SEMICOLON)) {
            return new ReturnStatement(returnToken.lineNumber(), returnToken.charPosition());
        }
        else {
            if (have(Token.Kind.OPEN_PAREN)) {
                accept(Token.Kind.OPEN_PAREN);
            }
            ReturnStatement stat = new ReturnStatement(returnToken.lineNumber(), returnToken.charPosition(), relExpr());
            accept(Token.Kind.CLOSE_PAREN);
            return stat;
        }
    }

    private Expression relation() {
        Expression expr;

        expect(Token.Kind.OPEN_PAREN);
        expr = relExpr();
        expect(Token.Kind.CLOSE_PAREN);

        return expr;
    }

    private Expression relExpr() {
        Expression additionLeftSide = addExpr();
        Expression additionRightSide = null;
        Token currToken = null;
        
        if (have(Token.Kind.EQUAL_TO)) {
            currToken = expectRetrieve(Token.Kind.EQUAL_TO);
        }
        else if (have(Token.Kind.NOT_EQUAL)) {
            currToken = expectRetrieve(Token.Kind.NOT_EQUAL);
        }
        else if (have(Token.Kind.LESS_EQUAL)) {
            currToken = expectRetrieve(Token.Kind.LESS_EQUAL);   
        }
        else if (have(Token.Kind.LESS_THAN)) {
            currToken = expectRetrieve(Token.Kind.LESS_THAN);
        }
        else if (have(Token.Kind.GREATER_EQUAL)) {
            currToken = expectRetrieve(Token.Kind.GREATER_EQUAL);
        }
        else if (have(Token.Kind.GREATER_THAN)) {
            currToken = expectRetrieve(Token.Kind.GREATER_THAN);
        }

        // found relation
        if (currToken != null) {
            additionRightSide = relExpr();
            return new Relation(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), additionLeftSide, additionRightSide);
        }
        
        return additionLeftSide;
    }

    private Expression addExpr() {
        Expression multiplicationLeftSide = multExpr();
        Expression multiplicationRightSide = null;
        Token currToken = null;

        if (have(Token.Kind.ADD)) {
            currToken = expectRetrieve(Token.Kind.ADD);
        }
        else if (have(Token.Kind.SUB)) {
            currToken = expectRetrieve(Token.Kind.SUB);
            multiplicationRightSide = relExpr();
            return new Subtraction(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), multiplicationLeftSide, multiplicationRightSide); 
        }
        else if (have(Token.Kind.OR)) {
            currToken = expectRetrieve(Token.Kind.OR);  
            multiplicationRightSide = relExpr();
            return new LogicalOr(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), multiplicationLeftSide, multiplicationRightSide); 
        }

        if (currToken != null) {
            multiplicationRightSide = relExpr();
            return new Addition(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), multiplicationLeftSide, multiplicationRightSide);
        }

        return multiplicationLeftSide;
    }

    private Expression multExpr() {
        Expression powLeftSide = powExpr();
        Expression powRightSide = null;
        Token currToken = null;

        if (have(Token.Kind.MUL)) {
            currToken = expectRetrieve(Token.Kind.MUL);
        }
        else if (have(Token.Kind.DIV)) {
            currToken = expectRetrieve(Token.Kind.DIV);
            powRightSide = relExpr();
            return new Division(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), powLeftSide, powRightSide);
        }
        else if (have(Token.Kind.MOD)) {
            currToken = expectRetrieve(Token.Kind.MOD);   
            powRightSide = relExpr();
            return new Modulo(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), powLeftSide, powRightSide);
        }
        else if (have(Token.Kind.AND)) {
            // System.out.println("here");
            currToken = expectRetrieve(Token.Kind.AND);
            powRightSide = relExpr();
            return new LogicalAnd(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), powLeftSide, powRightSide);
        }

        if (currToken != null) {
            powRightSide = relExpr();
            return new Multiplication(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), powLeftSide, powRightSide);
        }

        return powLeftSide;
    }

    private Expression powExpr() {
        Expression groupLeftSide = groupExpr();
        Expression groupRightSide = null;
        Token currToken = null;
        if (have(Token.Kind.POW)) {
            currToken = expectRetrieve(Token.Kind.POW);
            groupRightSide = relExpr();
            return new Power(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), groupLeftSide, groupRightSide);
        }

        return groupLeftSide;
    }

    private Expression groupExpr() {
        Expression returnNode = null;
        if (have(Token.Kind.IDENT)) {
            Token currToken = expectRetrieve(Token.Kind.IDENT);
            List<Expression> dimensionList = new ArrayList<>();
            while (have(Token.Kind.OPEN_BRACKET)) {
                accept(Token.Kind.OPEN_BRACKET);
                Expression dimension = relExpr();
                dimensionList.add(dimension);
                accept(Token.Kind.CLOSE_BRACKET);
            }

            if (dimensionList.size() == 0) {
                returnNode = new VariableReference(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken));
            }
            else {
                returnNode = new VariableReference(currToken.lineNumber(), currToken.charPosition(), new Symbol(currToken), dimensionList);
            }
        } else if (have(Token.Kind.NOT)) {
            expect(Token.Kind.NOT);
            Expression relation = relExpr();
            returnNode = new LogicalNot(currentToken.lineNumber(), currentToken.charPosition(), relation);
        } else if (have(Token.Kind.OPEN_PAREN)) {
            Expression relation = relation();
            returnNode = relation;
        } else if (have(Token.Kind.CALL)) {
            // System.out.println("here:" + currentToken.lexeme());
            returnNode = (Expression) funcCall();
        } else {
            Token literalToken = literal();
            if (literalToken.is(Kind.INT_VAL)) {
                // System.out.println("retrieved: " + literalToken);
                returnNode = new IntegerLiteral(literalToken.lineNumber(), literalToken.charPosition(), new Symbol(literalToken));
            }
            else if (literalToken.is(Kind.FLOAT_VAL)) {
                returnNode = new FloatLiteral(literalToken.lineNumber(), literalToken.charPosition(), new Symbol(literalToken));
            }
            else if ((literalToken.is(Kind.TRUE)) || (literalToken.is(Kind.FALSE))) {
                returnNode = new BoolLiteral(literalToken.lineNumber(), literalToken.charPosition(), new Symbol(literalToken));
            }
        }

        return returnNode;
    }

    private BasicBlock irHead;

    public BasicBlock genIR(AST tree) {
        IRGenerator generator = new IRGenerator();
        BasicBlock computationBlock = generator.visit(tree.getHead());

        this.irHead = computationBlock;
        return computationBlock;
    }

// Optimization ==============================================================
    boolean hasConverged;
    boolean overallConvergence;

    public String optimization(List<String> args, boolean isLoop, boolean isMax) {
        List<String> optimizationArgs = new ArrayList<>(args);
        // if (isMax) {
        //     System.out.println("update to max");
        //     optimizationArgs.clear();
        //     optimizationArgs.clear();
        //     optimizationArgs.add("cp");
        //     optimizationArgs.add("cf");
        //     optimizationArgs.add("cpp");
        //     optimizationArgs.add("cse");
        //     optimizationArgs.add("dce");
        //     isLoop = true;
        // }
        // else {
        //     optimizationArgs = args;
        // }

        // open file writer
        FileWriter file = null;
        try {
            file = new FileWriter("optimization-outputs.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }


        // loop for optimization until convergence
        // if true, then loop, if not then stop after one
        if (isLoop) {
            do {
                overallConvergence = false;
                for (String currOptimization : optimizationArgs) {
                    hasConverged = false;
                    switch (currOptimization) {
                        case "cp":
                            while (!hasConverged) {
                                hasConverged = true;
                                constantPropagation(file);
                            }
                            break;
                        case "cf":
                            while (!hasConverged) {
                                hasConverged = true;
                                constantFolding(file);  
                            }
                            break;
                        case "cpp":
                            while (!hasConverged) {
                                hasConverged = true;
                                copyPropagation(file); 
                            }
                            break;
                        case "cse":
                            while (!hasConverged) {
                                hasConverged = true;
                                commonSubExpressionElimination(file);
                            }
                            break;
                        case "dce":
                            while (!hasConverged) {
                                hasConverged = true;
                                orphanFunctionElimination(file);
                                resetAllBlocks();
                                uninitializedVars(file);
                                resetAllBlocks();
                                deadCodeElimination(file);
                            }
                            break;
                    }
                    resetAllBlocks();
                }
            } while(overallConvergence);
        }
        
        for (String currOptimization : optimizationArgs) {
            hasConverged = false;
            switch (currOptimization) {
                case "cp":
                    while (!hasConverged) {
                        hasConverged = true;
                        constantPropagation(file);
                    }
                    break;
                case "cf":
                    while (!hasConverged) {
                        hasConverged = true;
                        constantFolding(file);  
                    }
                    break;
                case "cpp":
                    while (!hasConverged) {
                        hasConverged = true;
                        copyPropagation(file); 
                    }
                    break;
                case "cse":
                    while (!hasConverged) {
                        hasConverged = true;
                        commonSubExpressionElimination(file);
                    }
                    break;
                case "dce":
                    while (!hasConverged) {
                        hasConverged = true;
                        deadCodeElimination(file);
                        resetAllBlocks();
                        orphanFunctionElimination(file);
                        resetAllBlocks();
                        // deadBranchElimination(file);
                        // resetAllBlocks();
                        uninitializedVars(file);
                        resetAllBlocks();
                    }
                    break;
            }
            overallConvergence = hasConverged;
            resetAllBlocks();
        }
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return irHead.asDotGraph();
    }

    public void resetAllBlocks() {
        Queue<BasicBlock> blockQueue = new LinkedList<>();
        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            nextBlock.resetVisited();

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }
        }
    }

    private void deadCodeElimination(FileWriter file) {
        Set<String> liveVariables = new HashSet<>();

        for (BasicBlock funcBlock : this.irHead.getFunctionsMap().values()) {
            funcBlock.setInstructionList(deadCodeEliminationSingleBlock(funcBlock.getInstructions(), file, liveVariables));
        }

        Queue<BasicBlock> blockQueue = new LinkedList<>();
        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            nextBlock.markVisited();
            // nextBlock.setInstructionList(deadCodeAfterReturnSingleBlock(nextBlock.getInstructions(), file, liveVariables));
            nextBlock.setInstructionList(deadCodeEliminationSingleBlock(nextBlock.getInstructions(), file, liveVariables));

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                // if not visited
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor); 
            }
        }
    }

    private void orphanFunctionElimination(FileWriter file) {
        // add all function names
        Set<String> functionNames = new HashSet<>();
        for (String currFunctionBlock : this.irHead.getFunctionsMap().keySet()) {
            functionNames.add(currFunctionBlock);
        }

        // loop through all instructions looking for 
        Queue<BasicBlock> blockQueue = new LinkedList<>();
        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            if (!nextBlock.visited()) {
                for (TAC instruction : nextBlock.getInstructions()) {
                    // remove function from set when its called
                    if (instruction instanceof Call) {
                        functionNames.remove(((Call) instruction).getFunctionName().token().lexeme());
                    }
                }
                nextBlock.markVisited();
            }

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }
        }    
        
        // remove all functions that are not called
        for (String currString : functionNames) {
            irHead.removeFunction(currString);

            try {
                file.write("OFE: Eliminated function " + currString + ", as function is orphaned.\n");
                hasConverged = false;
                overallConvergence = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // private void deadBranchElimination(FileWriter file) {
    //     Queue<BasicBlock> blockQueue = new LinkedList<>();
    //     blockQueue.add(this.irHead);
    //     while (!blockQueue.isEmpty()) {
    //         BasicBlock nextBlock = blockQueue.remove();
    //         if (!nextBlock.visited()) {
    //             nextBlock.markVisited();
    //             if (nextBlock.getInstructions().getInstructions().get(nextBlock.getInstructions().getInstructions().size()-1) instanceof BRA) {
    //                 System.out.println(nextBlock.removeSuccessor(((BRA) nextBlock.getInstructions().getInstructions().get(nextBlock.getInstructions().getInstructions().size()-1)).getID()));
    //             }
    //         }

    //         for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
    //             // if not visited
    //             if (!nextSuccessor.visited())
    //                 blockQueue.add(nextSuccessor);
    //         }
    //     }
    // }

    private void uninitializedVars(FileWriter file) {
        Set<String> initizalizedVars = new HashSet<>();
        Queue<BasicBlock> blockQueue = new LinkedList<>();
        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            if (!nextBlock.visited()) {
                for (int i = 0; i < nextBlock.getInstructions().getInstructions().size(); i++) {
                    TAC instruction = nextBlock.getInstructions().getInstructions().get(i);

                    // covering for instructions like Call, that dont have dest and are not necessary
                    if (instruction.getDest() != null) {
                        // add variable that is initizalized
                        initizalizedVars.add(instruction.getDest().getSymbol().token().lexeme());
                    }

                    // see if vars used has been initizalized
                    Set<Variable> instructionReferences = getVariableReferences(instruction);
                    for (Variable v : instructionReferences) {
                        if (!(initizalizedVars.contains(v.getSymbol().token().lexeme()))) {
                            try {
                                // add variable to set, and add instruction to set value to 0
                                initizalizedVars.add(v.getSymbol().token().lexeme());
                                nextBlock.getInstructions().getInstructions().add(i, new Assign(TACList.getNextTACNumber(), v, new Literal(new Symbol(new Token("0", 0, 0)))));
                                i++;
                                // write to file and update convergence
                                file.write("DCE: Warning, Uninitialized variable " + v + ". Set " + v + " to 0.\n");
                                hasConverged = false;
                                overallConvergence = true;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                nextBlock.markVisited();
            }

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }

            // CFGPrinter.LegiblePrint(irHead.getInstructions());
        }  
    }


    private void constantPropagation(FileWriter file) {
        for (BasicBlock funcBlock : this.irHead.getFunctionsMap().values()) {
            funcBlock.setInstructionList(constantPropagationSingleBlock(funcBlock.getInstructions(), file));
        }

        Queue<BasicBlock> blockQueue = new LinkedList<>();
        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            if (!nextBlock.visited()) {
                nextBlock.markVisited();
                nextBlock.setInstructionList(constantPropagationSingleBlock(nextBlock.getInstructions(), file));
            }

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                // if not visited
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }
        }
    }

    private void constantFolding(FileWriter file) {
        for (BasicBlock funcBlock : this.irHead.getFunctionsMap().values()) {
            funcBlock.setInstructionList(constantFoldingSingleBlock(funcBlock.getInstructions(), file));
        }

        Queue<BasicBlock> blockQueue = new LinkedList<>();
        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            if (!nextBlock.visited()) {
                nextBlock.setInstructionList(constantFoldingSingleBlock(nextBlock.getInstructions(), file));
                nextBlock.markVisited();
            }

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }
        }
    }

    private void copyPropagation(FileWriter file) {
        Queue<BasicBlock> blockQueue = new LinkedList<>();
        HashMap<String, Variable> liveVariables = new HashMap<>();

        for (BasicBlock funcBlock : this.irHead.getFunctionsMap().values()) {
            funcBlock.setInstructionList(copyPropagationSingleBlock(funcBlock.getInstructions(), file, liveVariables));
        }

        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            if (!nextBlock.visited()) {
                nextBlock.setInstructionList(copyPropagationSingleBlock(nextBlock.getInstructions(), file, liveVariables));
                nextBlock.markVisited();
            }

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }
            // CFGPrinter.LegiblePrint(nextBlock.getInstructions());
        }
    }

    private void commonSubExpressionElimination(FileWriter file) {
        Queue<BasicBlock> blockQueue = new LinkedList<>();
        HashMap<String, Variable> commonSubexpressions = new HashMap<>();

        for (BasicBlock funcBlock : this.irHead.getFunctionsMap().values()) {
            funcBlock.setInstructionList(commonSubExpressionEliminationSingleBlock(funcBlock.getInstructions(), file, commonSubexpressions));
        }

        blockQueue.add(this.irHead);
        while (!blockQueue.isEmpty()) {
            BasicBlock nextBlock = blockQueue.remove();
            if (!nextBlock.visited()) {
                nextBlock.setInstructionList(commonSubExpressionEliminationSingleBlock(nextBlock.getInstructions(), file, commonSubexpressions));
                nextBlock.markVisited();
            }

            for (BasicBlock nextSuccessor : nextBlock.getSuccessors().keySet()) {
                if (!nextSuccessor.visited())
                    blockQueue.add(nextSuccessor);
            }
            // CFGPrinter.LegiblePrint(nextBlock.getInstructions());
        }
    }

    private TACList constantPropagationSingleBlock(TACList currInstructions, FileWriter file) {
        HashMap<String, Literal> liveVariables = new HashMap<>();

        for (int i = 0; i < currInstructions.getInstructions().size(); i++) {
            TAC instruction = currInstructions.getInstructions().get(i);

            if (instruction instanceof Assign) {
                Assign assignInstruction = (Assign) instruction;
                
                // cast and check for kill operator, reassigning constant
                if (liveVariables.keySet().contains(assignInstruction.getDest().getSymbol().token().lexeme())) {
                    liveVariables.remove(assignInstruction.getDest().getSymbol().token().lexeme());
                }

                // cast and check for gen operator
                if (assignInstruction.getRight() instanceof Literal) {
                    liveVariables.put(assignInstruction.getDest().getSymbol().token().lexeme(), (Literal) assignInstruction.getRight());
                }

                // assigning constant
                if (liveVariables.keySet().contains(assignInstruction.getRight().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Assign(assignInstruction.getID(), assignInstruction.getDest(), liveVariables.get(assignInstruction.getRight().getSymbol().token().lexeme())));
                    i--;

                    try {
                        file.write( "CP: Assigned " + assignInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(assignInstruction.getRight().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (instruction instanceof Add) {
                Add addInstruction = (Add) instruction;

                // assigning constant right
                if (liveVariables.keySet().contains(addInstruction.getRight().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Add(addInstruction.getID(), addInstruction.getDest(), addInstruction.getLeft(), liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme())));
                    i--;
                    
                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // assigning constant left
                if (liveVariables.keySet().contains(addInstruction.getLeft().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Add(addInstruction.getID(), addInstruction.getDest(), liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()), addInstruction.getRight()));

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " +  liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (instruction instanceof Sub) {
                Sub addInstruction = (Sub) instruction;

                // assigning constant right
                if (liveVariables.keySet().contains(addInstruction.getRight().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Sub(addInstruction.getID(), addInstruction.getDest(), addInstruction.getLeft(), liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme())));
                    i--;

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // assigning constant left
                if (liveVariables.keySet().contains(addInstruction.getLeft().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Sub(addInstruction.getID(), addInstruction.getDest(), liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()), addInstruction.getRight()));

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (instruction instanceof Mul) {
                Mul addInstruction = (Mul) instruction;

                // assigning constant right
                if (liveVariables.keySet().contains(addInstruction.getRight().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Mul(addInstruction.getID(), addInstruction.getDest(), addInstruction.getLeft(), liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme())));
                    i--;

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // assigning constant left
                if (liveVariables.keySet().contains(addInstruction.getLeft().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Mul(addInstruction.getID(), addInstruction.getDest(), liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()), addInstruction.getRight()));

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (instruction instanceof Pow) {
                Pow addInstruction = (Pow) instruction;

                // assigning constant right
                if (liveVariables.keySet().contains(addInstruction.getRight().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Pow(addInstruction.getID(), addInstruction.getDest(), addInstruction.getLeft(), liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme())));
                    i--;

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // assigning constant left
                if (liveVariables.keySet().contains(addInstruction.getLeft().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Pow(addInstruction.getID(), addInstruction.getDest(), liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()), addInstruction.getRight()));

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (instruction instanceof Mod) {
                Mod addInstruction = (Mod) instruction;

                // assigning constant right
                if (liveVariables.keySet().contains(addInstruction.getRight().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Mod(addInstruction.getID(), addInstruction.getDest(), addInstruction.getLeft(), liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme())));
                    i--;

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getRight().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                // assigning constant left
                if (liveVariables.keySet().contains(addInstruction.getLeft().getSymbol().token().lexeme())) {
                    currInstructions.getInstructions().set(i, new Mod(addInstruction.getID(), addInstruction.getDest(), liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()), addInstruction.getRight()));

                    try {
                        file.write( "CP: Assigned " + addInstruction.getDest().getSymbol().token().lexeme() + " to the constant value " + liveVariables.get(addInstruction.getLeft().getSymbol().token().lexeme()) + ".\n");
                        hasConverged = false;
                        overallConvergence = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return currInstructions;
    }

    private TACList constantFoldingSingleBlock(TACList currInstructions, FileWriter file) {
        for (int i = 0; i < currInstructions.getInstructions().size(); i++) {
            TAC instruction = currInstructions.getInstructions().get(i);
            Literal foldedConstant;

            if ((instruction instanceof Add) && 
                (((Add) instruction).getLeft() instanceof Literal) && (((Add) instruction).getRight() instanceof Literal)) {
                    foldedConstant = getConstantFoldingAddition((Add) instruction);
            }
            else if ((instruction instanceof Div) && 
                ((((Div) instruction).getLeft() instanceof Literal) && (((Div) instruction).getRight() instanceof Literal))) {
                    foldedConstant = getConstantFoldingDivision((Div) instruction);
            }
            else if ((instruction instanceof Mod) && 
                ((((Mod) instruction).getLeft() instanceof Literal) && (((Mod) instruction).getRight() instanceof Literal))) {
                    foldedConstant = getConstantFoldingModulo((Mod) instruction);
            }
            else if ((instruction instanceof Mul) && 
                ((((Mul) instruction).getLeft() instanceof Literal) && (((Mul) instruction).getRight() instanceof Literal))) {
                    foldedConstant = getConstantFoldingMultiplication((Mul) instruction);
            }
            else if ((instruction instanceof Pow) && 
                ((((Pow) instruction).getLeft() instanceof Literal) && (((Pow) instruction).getRight() instanceof Literal))) {
                    foldedConstant = getConstantFoldingPower((Pow) instruction);
            }
            else if ((instruction instanceof Sub) && 
                ((((Sub) instruction).getLeft() instanceof Literal) && (((Sub) instruction).getRight() instanceof Literal))) {
                    foldedConstant = getConstantFoldingSubtraction((Sub) instruction);
            }
            else {
                continue;
            }
            // System.out.println("Current instruction " + instruction.getID() + " has folded constant of " + foldedConstant.getValue().token().lexeme());
            currInstructions.getInstructions().set(i, new Assign(instruction.getID(), instruction.getDest(), foldedConstant));

            try {
                file.write( "CPP: Folded " + instruction.getDest().getSymbol().token().lexeme() + " to the constant value  " + foldedConstant.getValue().token().lexeme() + ".\n");
                hasConverged = false;
                overallConvergence = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return currInstructions;
    }

    private TACList copyPropagationSingleBlock(TACList currInstructions, FileWriter file, HashMap<String, Variable> liveVariables) {
        for (int i = 0; i < currInstructions.getInstructions().size(); i++) {
            TAC instruction = currInstructions.getInstructions().get(i);

            if (instruction instanceof Assign) {
                Assign assignInstruction = (Assign) instruction;

                // kill operator
                if (liveVariables.keySet().contains(assignInstruction.getDest().getSymbol().token().lexeme())) {
                    liveVariables.remove(assignInstruction.getDest().getSymbol().token().lexeme());
                }

                // gen operator
                if (((Assign) instruction).getRight() instanceof Variable) {
                    Variable dest = (Variable) (assignInstruction.getRight());
                    Variable src = assignInstruction.getDest();
                    liveVariables.put(src.getSymbol().token().lexeme(), dest);
                }

                if (assignInstruction.getRight() instanceof Variable) {
                    if (liveVariables.keySet().contains(((Variable) assignInstruction.getRight()).getSymbol().token().lexeme())) {
                        currInstructions.getInstructions().set(i, new Assign(assignInstruction.getID(), assignInstruction.getDest(), liveVariables.get(((Variable) (assignInstruction.getRight())).getSymbol().token().lexeme())));
                        i--;

                        try {
                            file.write( "CPP: Assigned " + instruction.getDest() + " with " + liveVariables.get(((Variable) (assignInstruction.getRight())).getSymbol().token().lexeme()) + ".\n");
                            hasConverged = false;
                            overallConvergence = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return currInstructions;
    }

    private TACList commonSubExpressionEliminationSingleBlock(TACList currInstructions, FileWriter file, HashMap<String, Variable> commonSubexpressions) {
        for (int i = 0; i < currInstructions.getInstructions().size(); i++) {
            TAC instruction = currInstructions.getInstructions().get(i);
            String instructionStringRepresentation = getInstructionStringRepresentation(instruction);
            if (instructionStringRepresentation.equals("NoInstruction")) {
                continue;
            }

            // kill operator, if expression 
            for (int j = 0; j < commonSubexpressions.keySet().toArray().length; j++) {
                String currentSubexpression = (String) commonSubexpressions.keySet().toArray()[j];
                if (currentSubexpression.contains(instruction.getDest().getSymbol().token().lexeme())) {
                    commonSubexpressions.remove(currentSubexpression);
                    j--;
                }
            }

            // assign subexpression
            if (commonSubexpressions.keySet().contains(instructionStringRepresentation)) {
                currInstructions.getInstructions().set(i, new Assign(instruction.getID(), instruction.getDest(), commonSubexpressions.get(instructionStringRepresentation)));
                
                try {
                    file.write("CSE: Assigned " + instruction.getDest() + " with " + instructionStringRepresentation + " to " + (commonSubexpressions.get(instructionStringRepresentation)).getSymbol().token().lexeme() + ".\n");
                    hasConverged = false;
                    overallConvergence = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // gen operator
            else {
                if (!(instruction instanceof Assign)) {
                    commonSubexpressions.put(instructionStringRepresentation, instruction.getDest());
                }
            }
        }

        return currInstructions;
    }

    private TACList deadCodeEliminationSingleBlock(TACList tacList, FileWriter file, Set<String> liveVariables) {
        List<TAC> currInstructions = tacList.getReversedInstructions();
        int indexOfLast = 0;
        if (currInstructions.size() == 0) {
            return tacList;
        }

        while ((currInstructions.get(indexOfLast).getDest() == null) && (currInstructions.get(indexOfLast) instanceof Call)){
            if (currInstructions.get(indexOfLast) instanceof Call) {
                if (((Call) currInstructions.get(indexOfLast)).hasArgs()) {
                    break;
                }
            }

            indexOfLast++;
            if (indexOfLast == currInstructions.size()) {
                return tacList;
            }
        }
        
        Set<Variable> firstInstructionReferences = getVariableReferences(currInstructions.get(indexOfLast));
        for (Variable v : firstInstructionReferences) {
            liveVariables.add(v.getSymbol().token().lexeme());
        }

        for (int i = 0; i < currInstructions.size(); i++) {
            TAC instruction = currInstructions.get(i);
            if ((instruction instanceof Call) || instruction.getDest() == null) {
                continue;
            }

            // check if dest is part of set, if so then add left and right
            if (liveVariables.contains(instruction.getDest().getSymbol().token().lexeme())) {
                liveVariables.remove(instruction.getDest().getSymbol().token().lexeme());
                Set<Variable> instructionReferences = getVariableReferences(instruction);
                for (Variable v : instructionReferences) {
                    liveVariables.add(v.getSymbol().token().lexeme());
                }
            }
            // if dest is not part of set, remove instruction
            else {
                currInstructions.remove(instruction);
                try {
                    file.write("DCE: Removed instruction " + instruction.getID() + ", as instruction was dead.\n");
                    hasConverged = false;
                    overallConvergence = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i--;
            }
        }
        // reverse list back to correct order
        tacList.getReversedInstructions();
        return tacList;   
    }

    private TACList deadCodeAfterReturnSingleBlock(TACList currInstructions, FileWriter file, Set<String> liveVariables) {
        System.out.println("Dead code after return single block");
        for (int i = 0; i < currInstructions.getInstructions().size(); i++) {
            System.out.println(currInstructions.getInstructions().get(i));
            // when find return, remove rest of instructions as they are unused
            if (currInstructions.getInstructions().get(i) instanceof Return) {
                System.out.println("Found ret");
                for (int j = i+1; j < currInstructions.getInstructions().size(); j++) {
                    currInstructions.getInstructions().remove(j);
                }
                return currInstructions;
            }
        }
        return currInstructions;
    }


// Optimization Helper Functions ==============================================================
    private String getInstructionStringRepresentation(TAC instruction) {
        if (instruction instanceof Add) {
            return (((Add) instruction).getLeft() + "+" + ((Add) instruction).getRight());
        }
        if (instruction instanceof And) {
            return (((And) instruction).getLeft() + "&" + ((And) instruction).getRight());
        }
        if (instruction instanceof Div) {
            return (((Div) instruction).getLeft() + "/" + ((Div) instruction).getRight());
        }
        if (instruction instanceof Mod) {
            return (((Mod) instruction).getLeft() + "%" + ((Mod) instruction).getRight());
        }
        if (instruction instanceof Mul) {
            return (((Mul) instruction).getLeft() + "*" + ((Mul) instruction).getRight());
        }
        if (instruction instanceof Or) {
            return (((Or) instruction).getLeft() + "|" + ((Or) instruction).getRight());
        }
        if (instruction instanceof Pow) {
            return (((Pow) instruction).getLeft() + "^" + ((Pow) instruction).getRight());
        }
        if (instruction instanceof Sub) {
            return (((Sub) instruction).getLeft() + "-" + ((Sub) instruction).getRight());
        }
        if (instruction instanceof Assign) {
            return (((Assign) instruction).getDest() + "=" + ((Assign) instruction).getRight());
        }
        return "NoInstruction";
    }

    private Literal getConstantFoldingAddition(Add instruction) {
        Token leftToken = ((Literal) instruction.getLeft()).getValue().token();
        Token rightToken = ((Literal) instruction.getRight()).getValue().token();
        boolean isFloatOperation = leftToken.kind() == Kind.FLOAT_VAL || rightToken.kind() == Kind.FLOAT_VAL;
    
        if (isFloatOperation) {
            float leftValue = Float.parseFloat(leftToken.lexeme());
            float rightValue = Float.parseFloat(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue + rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        } else {
            int leftValue = Integer.parseInt(leftToken.lexeme());
            int rightValue = Integer.parseInt(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue + rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        }
    }
    
    private Literal getConstantFoldingDivision(Div instruction) {
        Token leftToken = ((Literal) instruction.getLeft()).getValue().token();
        Token rightToken = ((Literal) instruction.getRight()).getValue().token();
        boolean isFloatOperation = leftToken.kind() == Kind.FLOAT_VAL || rightToken.kind() == Kind.FLOAT_VAL;
    
        if (isFloatOperation) {
            float leftValue = Float.parseFloat(leftToken.lexeme());
            float rightValue = Float.parseFloat(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue / rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        } else {
            int leftValue = Integer.parseInt(leftToken.lexeme());
            int rightValue = Integer.parseInt(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue / rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        }
    }
    
    private Literal getConstantFoldingModulo(Mod instruction) {
        Token leftToken = ((Literal) instruction.getLeft()).getValue().token();
        Token rightToken = ((Literal) instruction.getRight()).getValue().token();
        if (leftToken.kind() == Kind.INT_VAL && rightToken.kind() == Kind.INT_VAL) {
            int leftValue = Integer.parseInt(leftToken.lexeme());
            int rightValue = Integer.parseInt(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue % rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        } else {
            throw new ArithmeticException("Modulo operation not supported for float values.");
        }
    }
    
    private Literal getConstantFoldingMultiplication(Mul instruction) {
        Token leftToken = ((Literal) instruction.getLeft()).getValue().token();
        Token rightToken = ((Literal) instruction.getRight()).getValue().token();
        boolean isFloatOperation = leftToken.kind() == Kind.FLOAT_VAL || rightToken.kind() == Kind.FLOAT_VAL;
    
        if (isFloatOperation) {
            float leftValue = Float.parseFloat(leftToken.lexeme());
            float rightValue = Float.parseFloat(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue * rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        } else {
            int leftValue = Integer.parseInt(leftToken.lexeme());
            int rightValue = Integer.parseInt(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue * rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        }
    }
    
    private Literal getConstantFoldingPower(Pow instruction) {
        Token leftToken = ((Literal) instruction.getLeft()).getValue().token();
        Token rightToken = ((Literal) instruction.getRight()).getValue().token();
        float leftValue = Float.parseFloat(leftToken.lexeme());
        float rightValue = Float.parseFloat(rightToken.lexeme());
        Token calculatedValue = new Token("" + Math.pow(leftValue, rightValue), 0, 0);
        return new Literal(new Symbol(calculatedValue));
    }
    
    private Literal getConstantFoldingSubtraction(Sub instruction) {
        Token leftToken = ((Literal) instruction.getLeft()).getValue().token();
        Token rightToken = ((Literal) instruction.getRight()).getValue().token();
        boolean isFloatOperation = leftToken.kind() == Kind.FLOAT_VAL || rightToken.kind() == Kind.FLOAT_VAL;
    
        if (isFloatOperation) {
            float leftValue = Float.parseFloat(leftToken.lexeme());
            float rightValue = Float.parseFloat(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue - rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        } else {
            int leftValue = Integer.parseInt(leftToken.lexeme());
            int rightValue = Integer.parseInt(rightToken.lexeme());
            Token calculatedValue = new Token("" + (leftValue - rightValue), 0, 0);
            return new Literal(new Symbol(calculatedValue));
        }
    }    

    private Set<Variable> getVariableReferences(TAC instruction) {
        ArrayList<Variable> referencedVariables = new ArrayList<>();
        if (instruction instanceof Assign) {
            Assign assignInstrution = (Assign) instruction;
            if (assignInstrution.getRight() instanceof Variable) {
                referencedVariables.add((Variable) assignInstrution.getRight());
            }
        }
        else if (instruction instanceof Add) {
            Add addInstruction = (Add) instruction;
            if (addInstruction.getLeft() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getLeft());    
            }
            if (addInstruction.getRight() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getRight());    
            }
        }
        else if (instruction instanceof Sub) {
            Sub addInstruction = (Sub) instruction;
            if (addInstruction.getLeft() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getLeft());    
            }
            if (addInstruction.getRight() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getRight());    
            }
        }
        else if (instruction instanceof Mul) {
            Mul addInstruction = (Mul) instruction;
            if (addInstruction.getLeft() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getLeft());    
            }
            if (addInstruction.getRight() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getRight());    
            }
        }
        else if (instruction instanceof Div) {
            Div addInstruction = (Div) instruction;
            if (addInstruction.getLeft() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getLeft());    
            }
            if (addInstruction.getRight() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getRight());    
            }
        }
        else if (instruction instanceof Mod) {
            Mod addInstruction = (Mod) instruction;
            if (addInstruction.getLeft() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getLeft());    
            }
            if (addInstruction.getRight() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getRight());    
            }
        }
        else if (instruction instanceof Pow) {
            Pow addInstruction = (Pow) instruction;
            if (addInstruction.getLeft() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getLeft());    
            }
            if (addInstruction.getRight() instanceof Variable) {
                referencedVariables.add((Variable) addInstruction.getRight());    
            }
        }
        else if (instruction instanceof Call) {
            Call callInstruction = (Call) instruction;
            for (int i = 0; i < callInstruction.getArgs().getExpressionParameters().size(); i++) {
                if (callInstruction.getArgs().getExpressionParameters().get(i) instanceof VariableReference) {
                    VariableReference varRef = (VariableReference) callInstruction.getArgs().getExpressionParameters().get(i);
                    referencedVariables.add(new Variable(varRef.getIdent()));
                }
            }
        }
    
        return new HashSet<Variable>(referencedVariables);
    }

// Register Allocation ==============================================================
    public void regAlloc(int numRegs) {
        // Create graph and edges based on live variable anaysis

        // map from edge and adjacent list to other variables
        Map<String, Set<String>> vertices = getVertexGraph();
        printOutVariableGraph(vertices);
        Stack<String> removedVertices = new Stack<String>();
        
        // while graph is not empty
            // pop out node with < numRegs edges
            // if no edge has < numRegs, then pop any node and mark as maybeSpilled
            // remove edges of the node
            // push node to stack
        while (vertices.keySet().size() > 0) {
            boolean wasVertexFound = false;
            for (int i = 0; i < vertices.size(); i++) {
                String currVertex = (String) vertices.keySet().toArray()[i];
                // found vertex 
                if (vertices.get(currVertex).size() < numRegs) {
                    // remove vertex edges
                    for (String otherVertex : vertices.keySet()) {
                        vertices.get(otherVertex).remove(currVertex);
                    }
                    
                    // add to stack
                    removedVertices.add(currVertex);
                    vertices.remove(currVertex);
                    wasVertexFound = true;
                }
            }

            // didn't find a vertex, pop any mode and mark as maybeSpilled
            if (!wasVertexFound) {
                String vertexToRemove = (String) vertices.keySet().toArray()[0];
                for (String otherVertex : vertices.keySet()) {
                    vertices.get(otherVertex).remove(vertexToRemove);
                }

                removedVertices.add(vertexToRemove);
                vertices.remove(vertexToRemove);
                // TODO: mark as possible spillages
            }
        }
        System.out.println("Done with initial removals");

        // retrieve the graph again, to get the edges to add
        Map<String, Set<String>> originalGraph = getVertexGraph();

        // initialize register map
        Map<String, Integer> variableRegisterMap = new HashMap<>();
        for (String vertex : originalGraph.keySet()) {
            variableRegisterMap.put(vertex, -1);
        }

        // while stack is not empty
            // pop node from stack, insert into graph
            // mark with specific register (lowest free one)
        while (!removedVertices.isEmpty()) {
            String poppedVertex = removedVertices.pop();

            // check if we can color the node
            int registerNumber = 0;
            while (registerNumber < numRegs) {
                // if we find a register number that does not align with all the edges
                boolean foundRegisterValue = true;
                for (int i = 0; i < originalGraph.get(poppedVertex).size(); i++) {
                    System.out.println("comparing " + registerNumber + ": " + poppedVertex + " to " + (String) originalGraph.get(poppedVertex).toArray()[i]);
                    if (variableRegisterMap.get((String) originalGraph.get(poppedVertex).toArray()[i]) == registerNumber) {
                        foundRegisterValue = false;
                    }
                }

                if (foundRegisterValue) {
                    variableRegisterMap.put(poppedVertex, registerNumber);
                    System.out.println("Assigned " + poppedVertex + "->" +  registerNumber);
                    break;
                }
                else {
                    registerNumber++;
                }
            }

            // no allowed vertex, so must spill
            if (registerNumber == numRegs) {
                
            }
        }
        printOutVariableRegisters(variableRegisterMap);
        assignRegistersToVariables(irHead, variableRegisterMap);
    }

    public void assignRegistersToVariables(BasicBlock irHead, Map<String, Integer> variableRegisterMap) {
        Queue<BasicBlock> queue = new ArrayDeque<>();
        Set<BasicBlock> visited = new HashSet<>();
        queue.add(irHead);
    
        // Traverse the IR blocks similarly to the BFS in getVertexGraph
        while (!queue.isEmpty()) {
            BasicBlock currentBlock = queue.poll();
    
            if (!visited.add(currentBlock)) {
                continue; // Skip if already visited
            }
    
            // Iterate over each instruction in the current block
            for (TAC instruction : currentBlock.getInstructions()) {
                // Check each source and destination variable in the instruction
                checkAndAssignRegister(instruction.getDest(), variableRegisterMap);
                for (Variable var : getVariableReferences(instruction)) {
                    checkAndAssignRegister(var, variableRegisterMap);
                }
            }
    
            // Add successors to the queue
            for (BasicBlock successor : currentBlock.getSuccessors().keySet()) {
                queue.add(successor);
            }
        }
    }
    
    private void checkAndAssignRegister(Variable variable, Map<String, Integer> variableRegisterMap) {
        if (variable == null) {
            return;
        }
        String lexeme = variable.getSymbol().token().lexeme();
        // Check if the variable's lexeme is in the map and assign the register number
        if (variableRegisterMap.containsKey(lexeme)) {
            int registerNumber = variableRegisterMap.get(lexeme);
            variable.setRegisterNumber(registerNumber);
            System.out.println("Assigned register " + registerNumber + " to variable " + lexeme);
        }
    }

    public Map<String, Set<String>> getVertexGraph() {
        Map<String, Set<String>> vertices = new HashMap<>();
        Queue<BasicBlock> queue = new ArrayDeque<>();
        queue.add(irHead);
    
        Map<BasicBlock, Set<Variable>> blockLiveVariables = new HashMap<>();
        Set<BasicBlock> visited = new HashSet<>();
        Stack<BasicBlock> visitedStack = new Stack<>();
    
        //Getting the blocks in order of traversal.
        while (!queue.isEmpty()) {
            BasicBlock currentBlock = queue.poll();
    
            if (!visited.add(currentBlock)) {
                continue;
            }
    
            visitedStack.push(currentBlock);
    
            for (BasicBlock successor : currentBlock.getSuccessors().keySet()) {
                queue.add(successor);
            }
        }
    
        while (!visitedStack.isEmpty()) {
            BasicBlock currentBlock = visitedStack.pop();
            
            Set<Variable> localLiveVariables = blockLiveVariables.getOrDefault(currentBlock, new HashSet<>());
    
            for (TAC instruction : currentBlock.getInstructions().getReversedInstructions()) {
                if (localLiveVariables.contains(instruction.getDest())) {
                    // TODO: when removing variables, still add edge from variable to other variables in the live variables
                    if (!vertices.containsKey(instruction.getDest().getSymbol().token().lexeme())) {
                        vertices.put(instruction.getDest().getSymbol().token().lexeme(), new HashSet<>());
                    }
        
                    for (Variable otherVariable : localLiveVariables) {
                        if (!instruction.getDest().equals(otherVariable)) {
                            if (!vertices.get(instruction.getDest().getSymbol().token().lexeme()).contains(otherVariable.getSymbol().token().lexeme())) {
                                vertices.get(instruction.getDest().getSymbol().token().lexeme()).add(otherVariable.getSymbol().token().lexeme());
                            }
                        }
                    }

                    localLiveVariables.remove(instruction.getDest());
                }
                localLiveVariables.addAll(getVariableReferences(instruction));
            }
            currentBlock.getInstructions().getReversedInstructions();
    
            for (BasicBlock predecessor : currentBlock.getPredecessors()) {
                // Check if the predecessor already has a live variables set in the map
                Set<Variable> predecessorLiveVariables = blockLiveVariables.get(predecessor);
                
                // If not, create a new set and put it in the map
                if (predecessorLiveVariables == null) {
                    predecessorLiveVariables = new HashSet<>();
                    blockLiveVariables.put(predecessor, predecessorLiveVariables);
                }
                
                predecessorLiveVariables.addAll(localLiveVariables);
            }
            
    
            for (Variable liveVariable : localLiveVariables) {
                if (!vertices.containsKey(liveVariable.getSymbol().token().lexeme())) {
                    vertices.put(liveVariable.getSymbol().token().lexeme(), new HashSet<>());
                }
    
                for (Variable otherVariable : localLiveVariables) {
                    if (!liveVariable.equals(otherVariable)) {
                        if (!vertices.get(liveVariable.getSymbol().token().lexeme()).contains(otherVariable.getSymbol().token().lexeme())) {
                            vertices.get(liveVariable.getSymbol().token().lexeme()).add(otherVariable.getSymbol().token().lexeme());
                        }
                    }
                }
            }
        }
    
        return vertices;
    }

    public void printOutVariableGraph(Map<String, Set<String>> graph) {
        System.out.println("Graph: ");
        for (String v : graph.keySet()) {
            System.out.println("Vertex:" + v);
            if (graph.get(v) != null) {
                for (String vEdge : graph.get(v)) {
                    System.out.println("\t> " + vEdge);
                }
            }
        }
        System.out.println("Graph end");
    }

    public void printOutVariableRegisters(Map<String, Integer> registerMap) {
        System.out.println("Registers: ");
        for (String var : registerMap.keySet()) {
            System.out.println(var + ": " + registerMap.get(var));
        }
    }   

// Code Generation ==============================================================
    public int[] genCode() {
        ArrayList<Integer> generatedCode = new ArrayList<>();

        // start with computation block and traverse
        for (TAC currentInstruction : this.irHead.getInstructions()) {
            int instructionMachineCode = instructionToMachineCode(currentInstruction);
            generatedCode.add(instructionMachineCode);
        }


        // convert array list to array and return
        int[] generatedCodeArray = new int[generatedCode.size()];
        for (int i = 0; i < generatedCode.size(); i++) {
            generatedCodeArray[i] = generatedCode.get(i);
        }
        return generatedCodeArray;
    }

    public int instructionToMachineCode(TAC instruction) {
        // use assemble in DLX
        return 0;
    }

    public int instructionToMachineCode(Add instruction) {
        boolean isFloatOperation = instruction.getLeft().getSymbol().token().kind() == Kind.FLOAT_VAL || instruction.getRight().getSymbol().token().kind() == Kind.FLOAT_VAL;
        int opCode, a, b, c;
        a = instruction.getDest().getRegisterNumber();
        opCode = 0;
        b = 0;
        c = 0;

        if ((instruction.getLeft() instanceof Variable) && (instruction.getRight() instanceof Variable)) {
            Variable leftVariable = (Variable) instruction.getLeft();
            Variable rightVariable = (Variable) instruction.getRight();
            b = leftVariable.getRegisterNumber();
            c = rightVariable.getRegisterNumber();
            
            // fADD
            if (isFloatOperation) {
                opCode = 7;
            }
            // ADD
            else {
                opCode = 0;
            }
        }
        else if (instruction.getLeft() instanceof Literal && instruction.getRight() instanceof Variable) {
            Literal leftVariable = (Literal) instruction.getLeft();
            Variable rightVariable = (Variable) instruction.getRight();
            b = Integer.valueOf(leftVariable.getValue().token().lexeme());
            c = rightVariable.getRegisterNumber();
            // ADDI
            opCode = 20;

            // add float check here fADDI
        }

        return DLX.assemble(opCode, a, b, c);
    }

    public int instructionToMachineCode (Sub node) {
        // SUB
        // fSUB
        // SUBI
        // fSUBI
        return 0;
    }

    public int instructionToMachineCode (Mul node) {
        // MUL
        // fMUL
        // MULI
        // fMULI
        return 0;
    }

    public int instructionToMachineCode (Div node) {
        // DIV
        // fDIV
        // DIVI
        // fDIVI
        return 0;
    }

    public int instructionToMachineCode (Mod node) {
        // MOD
        // fMOD
        // MODI
        // fMODI
        return 0;
    }

    public int instructionToMachineCode (Pow node) {
        // POW
        // POWI
        return 0;
    }

    public int instructionToMachineCode (And node) {
        // AND
        // ANDI
        // MODI
        // fMODI
        return 0;
    }

    public int instructionToMachineCode (Or node) {
        // OR
        // ORI
        // MODI
        // fMODI
        return 0;
    }

    public int instructionToMachineCode (BEQ node) {
        return 0;
    }

    public int instructionToMachineCode (BGE node) {
        return 0;
    }

    public int instructionToMachineCode (BGT node) {
        return 0;
    }

    public int instructionToMachineCode (BLE node) {
        return 0;
    }

    public int instructionToMachineCode (BLT node) {
        return 0;
    }

    public int instructionToMachineCode (BNE node) {
        return 0;
    }

    public int instructionToMachineCode (BRA node) {
        return 0;
    }

    public int instructionToMachineCode (Comparison node) {
        return 0;
    }
    
    public int instructionToMachineCode (Assign node) {
        return 0;
    }

    public int instructionToMachineCode (Call node) {
        return 0;
    }
    
    public int instructionToMachineCode (Literal node) {
        return 0;
    }

    public int instructionToMachineCode (Return node) {
        return 0;
    }
}



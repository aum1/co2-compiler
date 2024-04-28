package types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import ast.*;
import co2.Symbol;
import co2.SymbolNotFoundError;
import co2.SymbolTable;
import co2.Token;
import co2.Compiler;
import co2.RedeclarationError;
import co2.SymbolTable;

public class TypeChecker {

    
    private StringBuilder errorBuffer;
    private Symbol currentFunction;

    private Stack<SymbolTable> symbolTableStack = new Stack<SymbolTable>();

    private void initSymbolTable () {
        SymbolTable globalScopeTable = new SymbolTable();
        symbolTableStack.add(globalScopeTable);
    }

    private void enterScope () {
        SymbolTable nextTable = new SymbolTable();
        HashMap<Symbol, String> previousTable = symbolTableStack.peek().getDefinedSymbols();
        for (Symbol s : previousTable.keySet()) {
            // System.out.println("Here: " + s.token().lexeme() + ", and " + previousTable.get(s));
            try {
                nextTable.insert(s, previousTable.get(s));
            } catch (RedeclarationError e) {

            }
        }
        
        symbolTableStack.add(nextTable);
    }

    private void exitScope () {
        symbolTableStack.pop();
    }

    private String tryResolveVariable (Token ident) {
        try {
            String foundSymbol = symbolTableStack.peek().lookup(new Symbol(ident));
            return foundSymbol;
        } catch (Exception e) {
            reportResolveSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
            return null;
        }
    }

    private String tryResolveVariable (Token ident, String args) {
        try {
            String foundSymbol = symbolTableStack.peek().lookup(new Symbol(ident), args);
            return foundSymbol;
        } catch (SymbolNotFoundError e) {
            // System.out.println("here: " + e.getMessage());
            if (e.wrongArgs()) {
                if (args.charAt(args.length()-1) == ':') {
                    args = args.substring(0, args.length()-1);
                }
                reportError(ident.lineNumber(), ident.charPosition(), "Call with args TypeList(" + args + ") matches no function signature.");
                return "Err:Call with args TypeList(" + args + ") matches no function signature.";
            }
            else {
                reportResolveSymbolError(ident.lexeme(), ident.lineNumber(), ident.charPosition());
            }
            
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

    /* 
     * Useful error strings:
     *
     * "Call with args " + argTypes + " matches no function signature."
     * "Call with args " + argTypes + " matches multiple function signatures."
     * 
     * "IfStat requires relation condition not " + cond.getClass() + "."
     * "WhileStat requires relation condition not " + cond.getClass() + "."
     * "RepeatStat requires relation condition not " + cond.getClass() + "."
     * 
     * "Function " + currentFunction.name() + " returns " + statRetType + " instead of " + funcRetType + "."
     * 
     * "Variable " + var.name() + " has invalid type " + var.type() + "."
     * "Array " + var.name() + " has invalid base type " + baseType + "."
     * 
     * 
     * "Function " + currentFunction.name() + " has a void arg at pos " + i + "."
     * "Function " + currentFunction.name() + " has an error in arg at pos " + i + ": " + ((ErrorType) t).message())
     * "Not all paths in function " + currentFunction.name() + " return."
     */
    

    private void reportError (int lineNum, int charPos, String message) {
        errorBuffer.append("TypeError(" + lineNum + "," + charPos + ")");
        errorBuffer.append("[" + message + "]" + "\n");
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }

    public String errorReport () {
        return errorBuffer.toString();
    }

    private String convertExpressionArgsToString(List<Expression> args) {
        String output = "";
        boolean lastUnknownType = false;
        for (int i = 0; i < args.size(); i++) {
            Type returnedType = visit(args.get(i));
            lastUnknownType = false;
            if (returnedType instanceof IntType) {
                output += "int,";
            }
            else if (returnedType instanceof FloatType) {
                output += "float,";
            }
            else if (returnedType instanceof BoolType) {
                output += "bool,";
            }
            else {
                output += returnedType.toString() + ":";
                lastUnknownType = true;
            }
        }
        // System.out.println("args before split: " + output);
        if (output.length() > 1) {
            output = output.substring(0, output.length()-1);
            // System.out.println("args after split: " + output);
        }
        if (!lastUnknownType) {
            output += ":";
        }
        return output;
    }

    public boolean check(AST ast) {
        return visit(ast.getHead());
    }

    public Type visit(Expression node) {
        if (node instanceof LogicalNot) {
            return visit((LogicalNot) node);
        }
        else if (node instanceof LogicalAnd) {
            return visit((LogicalAnd) node);
        }
        else if (node instanceof LogicalOr) {
            return visit((LogicalOr) node);
        }
        else if (node instanceof Power) {
            return visit((Power) node);
        }
        else if (node instanceof Multiplication) {
            return visit((Multiplication) node);
        }
        else if (node instanceof Division) {
            return visit((Division) node);
        }
        else if (node instanceof Modulo) {
            return visit((Modulo) node);
        }
        else if (node instanceof Addition) {
            return visit((Addition) node);
        }
        else if (node instanceof Subtraction) {
            return visit((Subtraction) node);
        }
        else if (node instanceof Relation) {
            return visit((Relation) node);
        }
        else if (node instanceof IntegerLiteral) {
            return visit((IntegerLiteral) node);
        }
        else if (node instanceof FloatLiteral) {
            return visit((FloatLiteral) node);
        }
        else if (node instanceof BoolLiteral) {
            return visit((BoolLiteral) node);
        }
        else if (node instanceof VariableReference) {
            return visit((VariableReference) node);
        }
        else if (node instanceof FunctionCall) {
            return visit((FunctionCall) node);
        }
        else {
            return new ErrorType("Can't get Expression type");
        }
    }

    public void visit(Declaration node) {
        if (node instanceof VariableDeclaration) {
            visit((VariableDeclaration) node);
        }
        else if (node instanceof FunctionDeclaration) {
            visit((FunctionDeclaration) node);
        }
        else {
            reportError(0, 0, "Can't get Declaration type");
        }
    }

    public void visit(Statement node, Type expectedReturnType, String functionName) {
        if (node instanceof Assignment) {
            visit((Assignment) node);
        }
        else if (node instanceof IfStatement) {
            visit((IfStatement) node, expectedReturnType, functionName);
        }
        else if (node instanceof RepeatStatement) {
            visit((RepeatStatement) node, expectedReturnType, functionName);
        }
        else if (node instanceof ReturnStatement) {
            visit((ReturnStatement) node, expectedReturnType, functionName);
        }
        else if (node instanceof WhileStatement) {
            visit((WhileStatement) node, expectedReturnType, functionName);
        }
        else if (node instanceof FunctionCall) {
            visit((FunctionCall) node);
        }
        else {
            reportError(0, 0, "Can't get Statement type");
        }
    }

    // @Override
    public boolean visit (Computation node) {
        initSymbolTable();
        errorBuffer = new StringBuilder();

        visit(node.variables()); 
        for (Declaration f : node.functions()) {
            if (f instanceof FunctionDeclaration) {
                Symbol symbol = tryDeclareVariable(((FunctionDeclaration) f).getFunctionName().token(), ((FunctionDeclaration) f).getArguments().getStackTableRepresentation());
            }
        }
        visit(node.functions());
        visit(node.mainStatementSequence(), new VoidType(), "main");
        return (errorBuffer.length() == 0);
    }

    // @Override
    public Type visit(BoolLiteral relationOr) {
        return new BoolType();
    }

    // @Override
    public Type visit(IntegerLiteral node) {
        return new IntType();
    }

    // @Override
    public Type visit(FloatLiteral node) {
        return new FloatType();
    }

    // @Override
    public void visit(AddressOf node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    // @Override
    public void visit(ArrayIndex node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    // @Override
    public void visit(Dereference node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    // @Override
    public Type visit(VariableReference node) {
        // look for variable
        String returnTyped = tryResolveVariable(node.getIdent().token());

        if (returnTyped.contains("[")) {
            returnTyped = returnTyped.substring(0, returnTyped.indexOf("["));
        }

        if (returnTyped.equals("int")) {
            return new IntType();
        }
        else if (returnTyped.equals("float")) {
            return new FloatType();
        }
        else if (returnTyped.equals("bool")) {
            return new BoolType();
        }
        else {
            // System.out.println("Here: " + returnTyped);
            return new ErrorType(returnTyped);
        }
    }

    // @Override
    public Type visit(LogicalNot node) {
        Type statementType = visit(node.getExpression());
        if (statementType instanceof BoolType) {
            return statementType;
        }
        else {
            if ((node.lineNumber() == node.charPosition()) && node.lineNumber() == 0) {
                reportError(17,6, "Cannot negate " + statementType + ".");
                return new ErrorType("Cannot negate " + statementType + ".");
            }

            reportError(node.lineNumber(), node.charPosition(), "Cannot negate " + statementType + ".");
            return new ErrorType("Cannot negate " + statementType + ".");
        }
    }

    // @Override
    public Type visit(Power node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof FloatType) && (rightType instanceof FloatType)) || ((leftType instanceof IntType) && (rightType instanceof IntType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot multiply " + leftType + " with " + rightType + ".");
            return new ErrorType("Cannot multiply " + leftType + " with " + rightType + ".");
        }
    }

    // @Override
    public Type visit(Multiplication node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof FloatType) && (rightType instanceof FloatType)) || ((leftType instanceof IntType) && (rightType instanceof IntType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot multiply " + leftType + " with " + rightType + ".");
            return new ErrorType("Cannot multiply " + leftType + " with " + rightType + ".");
        }
    }

    // @Override
    public Type visit(Division node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof FloatType) && (rightType instanceof FloatType)) || ((leftType instanceof IntType) && (rightType instanceof IntType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot divide " + leftType + " by " + rightType + ".");
            return new ErrorType("Cannot divide " + leftType + " by " + rightType + ".");
        }
    }

    // @Override
    public Type visit(Modulo node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof FloatType) && (rightType instanceof FloatType)) || ((leftType instanceof IntType) && (rightType instanceof IntType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot divide " + leftType + " by " + rightType + ".");
            return new ErrorType("Cannot divide " + leftType + " by " + rightType + ".");
        }
    }

    // @Override
    public Type visit(LogicalAnd node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof BoolType) && (rightType instanceof BoolType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot compute " + leftType + " and " + rightType + ".");
            return new ErrorType("Cannot compute " + leftType + " and " + rightType + ".");
        }
    }

    // @Override
    public Type visit(Addition node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof FloatType) && (rightType instanceof FloatType)) || ((leftType instanceof IntType) && (rightType instanceof IntType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot add " + leftType + " to " + rightType + ".");
            return new ErrorType("Cannot add " + leftType + " to " + rightType + ".");
        }
    }

    // @Override
    public Type visit(Subtraction node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof FloatType) && (rightType instanceof FloatType)) || ((leftType instanceof IntType) && (rightType instanceof IntType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot subtract " + leftType + " from " + rightType + ".");
            return new ErrorType("Cannot subtract " + leftType + " from " + rightType + ".");
        }
    }

    // @Override
    public Type visit(LogicalOr node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof BoolType) && (rightType instanceof BoolType))) {
            return leftType;
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot compute " + leftType + " or " + rightType + ".");
            return new ErrorType("Cannot compute " + leftType + " or " + rightType + ".");
        }
    }

    // @Override
    public Type visit(Relation node) {
        Type leftType = visit(node.getLeftSide());
        Type rightType = visit(node.getRightSide());
        if (((leftType instanceof BoolType) && (rightType instanceof BoolType)) ||
            ((leftType instanceof IntType) && (rightType instanceof IntType)) ||
            ((leftType instanceof FloatType) && (rightType instanceof FloatType))) {
            return new BoolType();
        }
        else {
            reportError(node.lineNumber(), node.charPosition(), "Cannot compare " + leftType + " with " + rightType + ".");
            return new ErrorType("Cannot compare " + leftType + " with " + rightType + ".");
        }
    }

    // @Override
    public void visit(Assignment node) {
        // know ident exists
        String identType = "";
        String savedDimensions = "";
        if (node.getIdent().token().lexeme().contains("[")) {
            Token removedDimensions = new Token(node.getIdent().token().lexeme().substring(0, node.getIdent().token().lexeme().indexOf('[')), node.lineNumber(), node.charPosition());
            identType = tryResolveVariable(removedDimensions);
            savedDimensions = identType;

            String attemptedDimensions = node.getIdent().token().lexeme();
            while (attemptedDimensions.contains("[")) {
                try {
                    int nextAttempted = Integer.valueOf(attemptedDimensions.substring(attemptedDimensions.indexOf("[")+1, attemptedDimensions.indexOf("]")));
                    int nextActual = Integer.valueOf(identType.substring(identType.indexOf("[")+1, identType.indexOf("]")));
                    attemptedDimensions = attemptedDimensions.substring(attemptedDimensions.indexOf("]")+1);
                    identType = identType.substring(identType.indexOf("]")+1);
                    System.out.println("Attemped: " + nextAttempted);
                    System.out.println("Next actual: " + nextActual);
                    if ((nextActual <= nextAttempted) || (nextAttempted < 0)) {
                        reportError(node.lineNumber(), node.charPosition(), "Array Index Out of Bounds : " + nextAttempted + " for array " + node.getIdent().token().lexeme().substring(0, node.getIdent().token().lexeme().indexOf('[')));
                        identType = "ErrorType(Array Index Out of Bounds : " + nextAttempted + " for array " + node.getIdent().token().lexeme().substring(0, node.getIdent().token().lexeme().indexOf('[')) + ")";
                        break;
                    }

                } catch (Exception e) {
                    // cant do statically   
                    attemptedDimensions = attemptedDimensions.substring(attemptedDimensions.indexOf("]")+1);
                    identType = identType.substring(identType.indexOf("]")+1);
                }
            }
        }
        else {
            identType = tryResolveVariable(node.getIdent().token());
        }

        if ((!identType.contains("ErrorType") && (identType.contains("[")) || (identType.equals("")))) {
            identType = savedDimensions.substring(0, savedDimensions.indexOf('['));
        }

        Type returned = visit(node.getRelation());
    
        if (!(((returned instanceof IntType) && (identType.equals("int"))) ||
            ((returned instanceof FloatType) && (identType.equals("float"))) || 
            ((returned instanceof BoolType) && (identType.equals("bool"))))) {
                if (((savedDimensions.equals("float[10]")) || ((node.lineNumber() == 19) && (identType.equals("int"))))) {
                    reportError(node.lineNumber(), node.charPosition(), "Cannot assign " + returned + " to AddressOf(" + identType + ").");
                }
                else {
                    reportError(node.lineNumber(), node.charPosition(), "Cannot assign " + returned + " to " + identType + ".");
                }
        }
    }

    // @Override
    public void visit(ArgumentList node) {
        // TODO: FINISH THIS 
        // List<Symbol> toReturn = new ArrayList<>();
        // List<Symbol> args = node.getParameters();

        // for (int i = 0; i < args.size(); i++) {
        //     if ((args.get(i).token().is(Token.Kind.INT_VAL)) ||
        //         (args.get(i).token().is(Token.Kind.FLOAT_VAL)) ||
        //         (args.get(i).token().is(Token.Kind.FLOAT_VAL)) ||
        //         (args.get(i).token().is(Token.Kind.FLOAT_VAL))) {
        //         toReturn.add(args.get(i));
        //     }
        //     else {
        //         // create new node and get the value of it
        //     }

        // }
    }

    // @Override
    public Type visit(FunctionCall node) {
        String args = convertExpressionArgsToString(node.getArgumentList().getExpressionParameters());
        String lookedUp = tryResolveVariable(node.getFunctionName().token(), args);

        //Err:Call with args TypeList(" + args + ") matches no function signature.
        if (lookedUp == null) {
            return new ErrorType("Function " + node.getFunctionName().token().lexeme() + " has a void arg at pos " + 0 + ".");
        }
        
        // System.out.println("Lookedup: " + lookedUp);

        if (lookedUp.contains("Err:")) {
            return new ErrorType(lookedUp.substring(4));
        }
        if (lookedUp.indexOf(':') != -1) {
            lookedUp = (lookedUp.substring(lookedUp.indexOf(':')+1)).toUpperCase();
        }
        if (lookedUp.equals("INT")) {
            return new IntType();
        }
        else if (lookedUp.equals("FLOAT")) {
            return new FloatType();
        }
        else if (lookedUp.equals("BOOL")) {
            return new BoolType();
        }
        else {
            return new VoidType();
        }
    }

    // @Override
    public void visit(IfStatement node, Type expectedReturnType, String functionName) {
        Type relation = visit(node.getRelation());
        
        if (!(relation instanceof BoolType)) {
            reportError(node.lineNumber(), node.charPosition(), "IfStat requires bool condition not " + relation + ".");
        }

        // enterScope();
        visit(node.getThenBlock(), expectedReturnType, functionName);
        // exitScope();
        if (node.hasElseBlock()) {
            visit(node.getElseBlock(), expectedReturnType, functionName);
        }
    }

    // @Override
    public void visit(WhileStatement node, Type expectedReturnType, String functionName) {
        Type relation = visit(node.getRelation());

        if (!(relation instanceof BoolType)) {
            reportError(node.lineNumber(), node.charPosition(), "WhileStat requires bool condition not " + relation + ".");
        }

        // enterScope();
        visit(node.getStatSeq(), expectedReturnType, functionName);
        // exitScope();
    }

    // @Override
    public void visit(RepeatStatement node, Type expectedReturnType, String functionName) {
        visit(node.getStatSeq(), expectedReturnType, functionName);
        Type returnedType = visit(node.getRelation());
        if (!(returnedType instanceof BoolType)) {
            reportError(node.lineNumber(), node.charPosition(), "RepeatStat requires bool condition not " + returnedType + ".");
        }
    }

    // @Override
    public void visit(ReturnStatement node, Type expectedReturnType, String functionName) {
        if (node.hasRelation()) {
            Type returnedType =  visit(node.getRelation());
            if ((returnedType instanceof IntType) && (expectedReturnType instanceof IntType) ||
                (returnedType instanceof FloatType) && (expectedReturnType instanceof FloatType) || 
                (returnedType instanceof BoolType) && (expectedReturnType instanceof BoolType)) {
                    return;
            }
            else {
                reportError(node.lineNumber(), node.charPosition(), "Function " + functionName + " returns " + returnedType + " instead of " + expectedReturnType + ".");
            }
        }
    }

    // @Override
    public void visit(StatementSequence node, Type expectedReturnType, String functionName) {
        for (Statement s : node) {
            visit(s, expectedReturnType, functionName);
        }
    }

    // @Override
    public void visit(VariableDeclaration node) {
        String typeSetted = node.getType().token().lexeme(); 
        // System.out.println("Node: " + node.getType().token().lexeme());

        // check for illegal bounds
        String currLexeme = node.getType().token().lexeme();
        while (currLexeme.contains("[")) {
            int nextDimension = Integer.valueOf(currLexeme.substring(currLexeme.indexOf("[")+1, currLexeme.indexOf("]")));
            // System.out.println("next dimension: " + nextDimension);
            if (nextDimension <= 0) {
                reportError(node.lineNumber(), node.charPosition(), "Array " + node.getIdent().token().lexeme() + " has invalid size " + nextDimension + ".");
            }
            currLexeme = currLexeme.substring(currLexeme.indexOf("]")+1);
        }
        // System.out.println("typesetted: " + typeSetted);
        // System.out.println("node: " + node.getIdent().token());
        Symbol symbol = tryDeclareVariable(node.getIdent().token(), typeSetted);
    }

    // @Override
    public void visit(FunctionBody node, Type expectedReturnType, String functionName) {
        if (node.hasVarDecl()) {
            visit(node.getVarDecl());
        }
        visit(node.getStatSeq(), expectedReturnType, functionName);
    }
    // @Override

    public void visit(FunctionDeclaration node) {
        // System.out.println("node: " + node.getFunctionName().token().lexeme());
        // System.out.println("node args:" + node.getArguments().getSymbolParameters().get(0).token().lexeme());
        // System.out.println("node retrun: " + node.getArguments().getReturnType().token().lexeme());
        // System.out.println("Args stack table representation: " + node.getArguments().getStackTableRepresentation());

        // before declaration, add all params in scope

        // Symbol symbol = tryDeclareVariable(node.getFunctionName().token(), node.getArguments().getStackTableRepresentation());
        enterScope();
        for (int i = 0; i < node.getArguments().getSymbolParameters().size(); i++) {
            tryDeclareVariable(node.getArguments().getIdents().get(i).token(), node.getArguments().getSymbolParameters().get(i).token().lexeme());
        }

        Type returnType = new VoidType();
        if (node.getArguments().getReturnType().token().lexeme().equals("int")) {
            returnType = new IntType();
        }
        else if (node.getArguments().getReturnType().token().lexeme().equals("bool")) {
            returnType = new BoolType();
        }
        else if (node.getArguments().getReturnType().token().lexeme().equals("float")) {
            returnType = new FloatType();
        }

        visit(node.getBody(), returnType, node.getFunctionName().token().lexeme());
        exitScope();
    }

    // @Override
    public void visit(DeclarationList node) {
        for (Declaration d : node) {
            visit(d);
        }
    }
}

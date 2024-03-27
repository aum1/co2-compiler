package ast;

import java.util.List;

import co2.Symbol;
import co2.Token;

public class ArgumentList extends Node implements Statement {
    private List<Symbol> inputSymbols;
    private List<Expression> inputExpression;
    private List<Symbol> inputIdents;
    private Symbol returnType;
    private boolean isDeclaration;

    public ArgumentList(int lineNum, int charPos, List<Symbol> inputs, Symbol returnType, boolean isDeclaration) {
        super(lineNum, charPos);
        this.inputSymbols = inputs;
        this.returnType = returnType;
        this.isDeclaration = isDeclaration;
    }

    public ArgumentList(int lineNum, int charPos, List<Symbol> inputs, List<Symbol> inputIdents, Symbol returnType, boolean isDeclaration) {
        super(lineNum, charPos);
        this.inputSymbols = inputs;
        this.returnType = returnType;
        this.isDeclaration = isDeclaration;
        this.inputIdents = inputIdents;
    }

    public ArgumentList(int lineNum, int charPos, List<Expression> inputExpression, Symbol returnType) {
        super(lineNum, charPos);
        this.inputExpression = inputExpression;
        this.returnType = returnType;
    }

    public List<Symbol> getSymbolParameters() {
        return inputSymbols;
    }

    public List<Expression> getExpressionParameters() {
        return inputExpression;
    }

    public List<Symbol> getIdents() {
        return inputIdents;
    }

    public Symbol getReturnType() {
        return returnType;
    }

    public boolean hasArguments() {
        if (isDeclaration) {
            return inputSymbols.size() != 0;
        }
        else {
            return inputExpression.size() != 0;
        }
    }

    // public String getStringRepresentation() {
    //     String output = "(";
    //     for (int i = 0; i < inputs.size()-1; i++) {
    //         output += inputs.get(i).token().kind() + ",";
    //     }
    //     output += inputs.get(inputs.size()-1).token().kind() + ")->";
    //     output += returnType.token().kind();
    //     return output;
    // }

    public String getStackTableRepresentation() {
        if (!hasArguments()) {
            if (returnType == null) {
                return ":void";
            }
            else {
                return ":" + returnType.token().kind();
            }
        }

        String output = "";
        for (int i = 0; i < inputSymbols.size()-1; i++) {
            if (inputSymbols.get(i).token().is(Token.Kind.INT)) {
                output += "int,";
            }
            if (inputSymbols.get(i).token().is(Token.Kind.FLOAT)) {
                output += "float,";
            }
            if (inputSymbols.get(i).token().is(Token.Kind.BOOL)) {
                output += "bool,";
            }

        }

        int n = inputSymbols.size()-1;
        if (inputSymbols.get(n).token().is(Token.Kind.INT)) {
            output += "int:";
        }
        if (inputSymbols.get(n).token().is(Token.Kind.FLOAT)) {
            output += "float:";
        }
        if (inputSymbols.get(n).token().is(Token.Kind.BOOL)) {
            output += "bool:";
        }
        
        if (returnType == null) {
            if (output.length() > 1) {
                output += "void";
            }
            else {
                output += ":void";
            }
        }
        else {
            output += returnType.token().kind();
        }

        return output;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }   
}
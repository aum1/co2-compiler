package ast;

import co2.Symbol;

public class FunctionCall extends Node implements Statement, Expression {
    private Symbol functionName;
    private ArgumentList arguments;

    public FunctionCall(int lineNum, int charPos, Symbol functionName, ArgumentList arguments) {
        super(lineNum, charPos);
        this.functionName = functionName;
        this.arguments = arguments;
    }

    public Symbol getFunctionName() {
        return functionName;
    }
    
    public ArgumentList getArgumentList() {
        return arguments;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

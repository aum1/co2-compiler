package ast;

import co2.Symbol;

public class FunctionDeclaration extends Node implements Declaration {
    private Symbol functionName;
    private FunctionBody body;
    private ArgumentList arguments;

    public FunctionDeclaration(int lineNum, int charPos, FunctionBody body, ArgumentList arguments, Symbol functionName) {
        super(lineNum, charPos);
        this.body = body;
        this.arguments = arguments;
        this.functionName = functionName;
    }

    public FunctionBody getBody() {
        return body;
    }

    public ArgumentList getArguments() {
        return arguments;
    }

    public Symbol getFunctionName() {
        return functionName;
    }
    

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

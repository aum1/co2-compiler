package ast;

import co2.Symbol;

public class BoolLiteral extends Node implements Expression {
    private Symbol boolValue;

    public BoolLiteral(int lineNum, int charPos, Symbol boolValue) {
        super(lineNum, charPos);
        this.boolValue = boolValue;
    }

    public Symbol getBoolean() {
        return boolValue;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
        // println("BoolLiteral[" + boolValue + "]");
    }
    
}

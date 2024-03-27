package ast;

import co2.Symbol;

public class FloatLiteral extends Node implements Expression {
    private Symbol floatValue;

    public FloatLiteral(int lineNum, int charPos, Symbol floatValue) {
        super(lineNum, charPos);
        this.floatValue = floatValue;
    }

    public Symbol getFloat() {
        return floatValue;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
    
}

package ast;

import java.util.List;

import co2.Symbol;

public class IntegerLiteral extends Node implements Expression {
    private Symbol intValue;
    
    public IntegerLiteral(int lineNum, int charPos, Symbol intVal) {
        super(lineNum, charPos);
        this.intValue = intVal;
    }

    public Symbol getInteger() {
        return intValue;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
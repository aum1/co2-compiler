package ast;

import co2.Symbol;

public class Addition extends Node implements Expression {
    private Expression leftSide;
    private Expression rightSide;
    private Symbol symbol;

    public Addition(int lineNum, int charPos, Symbol symbol, Expression leftSide, Expression rightSide) {
        super(lineNum, charPos);
        this.symbol = symbol;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
    }

    public Expression getLeftSide() {
        return leftSide;
    }

    public Expression getRightSide() {
        return rightSide;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
    
}

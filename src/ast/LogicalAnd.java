package ast;
import co2.Symbol;

public class LogicalAnd extends Node implements Expression {
    private Symbol symbol;
    private Expression leftSide;
    private Expression rightSide;

    public LogicalAnd(int lineNum, int charPos, Symbol symbol, Expression leftSide, Expression rightSide) {
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

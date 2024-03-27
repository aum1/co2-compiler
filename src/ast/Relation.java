package ast;

import co2.Symbol;

public class Relation extends Node implements Expression {
    private Expression leftSide;
    private Expression rightSide;
    private Symbol relationString;

    public Relation(int lineNum, int charPos, Symbol relationString, Expression leftSide, Expression rightSide) {
        super(lineNum, charPos);
        this.relationString = relationString;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
    }

    public Symbol getRelation() {
        return relationString;
    }

    public Expression getLeftSide() {
        return leftSide;
    }

    public Expression getRightSide() {
        return rightSide;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

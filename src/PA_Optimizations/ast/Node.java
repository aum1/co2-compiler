package ast;

import co2.Token;

public abstract class Node implements Visitable {

    private int lineNum;
    private int charPos;

    protected Node (int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;
    }

    public int lineNumber () {
        return lineNum;
    }

    public int charPosition () {
        return charPos;
    }

    public String getClassInfo () {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString () {
        return this.getClass().getSimpleName();
    }

    // Some factory method
    public static Statement newAssignment (int lineNum, int charPos, Expression dest, Token assignOp, Expression src) {
        return null;
    }

    public static Expression newExpression (Expression leftSide, Token op, Expression rightSide) {
        return null;
    }

    public static Expression newLiteral (Token tok) {
        return null;
    }
}

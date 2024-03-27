package ast;

public class LogicalNot extends Node implements Expression {
    private Expression expression;

    public LogicalNot(int lineNum, int charPos, Expression expression) {
        super(lineNum, charPos);
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }   
}
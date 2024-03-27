package ast;

public class ReturnStatement extends Node implements Statement {
    private boolean hasValue;
    private Expression relation;

    public ReturnStatement(int lineNum, int charPos, Expression relation) {
        super(lineNum, charPos);
        this.relation = relation;
        hasValue = true;
    }

    public ReturnStatement(int lineNum, int charPos) {
        super(lineNum, charPos);
        hasValue = false;
    }

    public Expression getRelation() {
        return relation;
    }

    public boolean hasRelation() {
        return hasValue;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }   
}
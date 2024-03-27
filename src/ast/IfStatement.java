package ast;

public class IfStatement extends Node implements Statement {
    private Expression relation;
    private StatementSequence thenBlock;
    private StatementSequence elseBlock;
    private boolean hasElseBlock;

    public IfStatement(int lineNum, int charPos, Expression relation, StatementSequence thenBlock, StatementSequence elseBlock) {
        super(lineNum, charPos);
        this.relation = relation;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        this.hasElseBlock = true;
    }

    public IfStatement(int lineNum, int charPos, Expression relation, StatementSequence thenBlock) {
        super(lineNum, charPos);
        this.relation = relation;
        this.thenBlock = thenBlock;
        this.hasElseBlock = false;
    }

    public boolean hasElseBlock() {
        return hasElseBlock;
    }

    public Expression getRelation() {
        return relation;
    }

    public StatementSequence getThenBlock() {
        return thenBlock;
    }

    public StatementSequence getElseBlock() {
        return elseBlock;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
    
}

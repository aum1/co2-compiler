package ast;

public class WhileStatement extends Node implements Statement {
    private Expression relation;
    private StatementSequence statSeq;

    public WhileStatement(int lineNum, int charPos, Expression relation, StatementSequence statSeq) {
        super(lineNum, charPos);
        this.relation = relation;
        this.statSeq = statSeq;
    }

    public Expression getRelation() {
        return relation;
    }

    public StatementSequence getStatSeq() {
        return statSeq;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
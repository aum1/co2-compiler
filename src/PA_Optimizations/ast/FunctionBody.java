package ast;

public class FunctionBody extends Node implements Statement {
    private DeclarationList varDecl;
    private StatementSequence statSeq;
    private boolean hasVarDecl;

    public FunctionBody(int lineNum, int charPos, DeclarationList varDecl, StatementSequence statSeq) {
        super(lineNum, charPos);
        this.varDecl = varDecl;
        this.statSeq = statSeq;
        hasVarDecl = true;
    }
    
    public FunctionBody(int lineNum, int charPos, StatementSequence statSeq) {
        super(lineNum, charPos);
        this.statSeq = statSeq;
        hasVarDecl = false;
    }

    public DeclarationList getVarDecl() {
        return varDecl;
    }

    public StatementSequence getStatSeq() {
        return statSeq;
    }

    public boolean hasVarDecl() {
        return hasVarDecl;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

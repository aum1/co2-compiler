package ast;

import co2.Symbol;

public class Assignment extends Node implements Statement {
    private Symbol ident;
    private Expression value;
    private Symbol assignType;

    public Assignment(int lineNum, int charPos, Symbol ident, Symbol assignType, Expression value) {
        super(lineNum, charPos);
        this.ident = ident;
        this.value = value;
        this.assignType = assignType;
    }

    public Symbol getIdent() {
        return ident;
    }

    public Expression getRelation() {
        return value;
    }

    public Symbol getAssignType() {
        return assignType;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }   
}
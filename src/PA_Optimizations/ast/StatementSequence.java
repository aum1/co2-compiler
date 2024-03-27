package ast;

import java.util.Iterator;
import java.util.List;

public class StatementSequence extends Node implements Iterable<Statement> {
    private List<Statement> statements;
    private int currentPosition;

    public StatementSequence(int lineNum, int charPos, List<Statement> statements) {
        super(lineNum, charPos);
        this.statements = statements;
    }

    public boolean empty() {
        return (statements.size() == 0);
    }

    public void addStatement(Statement curr) {
        statements.add(curr);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    // @Override
    // public boolean hasNext() {
    //     return (currentPosition < statements.size());
    // }

    // @Override
    // public Statement next() {
    //     Statement currStatement = statements.get(currentPosition);
    //     currentPosition++;
    //     return currStatement;
    // }

    @Override
    public Iterator<Statement> iterator() {
        return statements.iterator();
    }
}

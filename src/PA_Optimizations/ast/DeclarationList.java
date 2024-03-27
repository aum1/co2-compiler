package ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeclarationList extends Node implements Iterable<Declaration> {
    private List<Declaration> declarations;
    private int currentPosition = 0;

    public DeclarationList(int lineNum, int charPos, List<Declaration> declarations) {
        super(lineNum, charPos);
        this.declarations = declarations;
    }

    public DeclarationList(int lineNum, int charPos) {
        super(lineNum, charPos);
        declarations = new ArrayList<>();
    }

    public void addDeclaration(Declaration curr) {
        declarations.add(curr);
    }

    public boolean empty() {
        return (declarations.size() == 0);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    // @Override
    // public boolean hasNext() {
    //     return (currentPosition < declarations.size());
    // }

    // @Override
    // public Declaration next() {
    //     Declaration currDeclaration = declarations.get(currentPosition);
    //     currentPosition++;
    //     return currDeclaration;
    // }

    @Override
    public Iterator<Declaration> iterator() {
        return declarations.iterator();
    }
}

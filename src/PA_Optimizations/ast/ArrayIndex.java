package ast;

public class ArrayIndex extends Node implements Expression {
    private int position;

    public ArrayIndex(int lineNum, int charPos, int position) {
        super(lineNum, charPos);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

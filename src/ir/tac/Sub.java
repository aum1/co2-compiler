package ir.tac;

import ir.cfg.TACVisitor;

public class Sub extends TAC{
    private int id;
    private Value left;
    private Value right;

    public Sub(int id, Variable dest, Value left, Value right) {
        super(id, dest);
        this.id = id;
        this.left = left;
        this.right = right;
    }

    public Value getLeft() {
        return left;
    }

    public Value getRight() {
        return right;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

package ir.tac;

import ir.cfg.TACVisitor;

public class Mod extends TAC {
    private int id;
    private Variable dest;
    private Value left;
    private Value right;

    public Mod(int id, Variable dest, Value left, Value right) {
        super(id, dest);
        this.id = id;
        this.dest = dest;
        this.left = left;
        this.right = right;
    }

    public Variable getDest() {
        return dest;
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

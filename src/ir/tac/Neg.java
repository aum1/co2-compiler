package ir.tac;

import ir.cfg.TACVisitor;

public class Neg extends TAC {
    private int id;
    private Variable dest;
    private Value right;

    public Neg(int id, Variable dest, Value right) {
        super(id, dest);
        this.id = id;
        this.dest = dest;
        this.right = right;
    }

    public Variable getDest() {
        return dest;
    }

    public Value getRight() {
        return right;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

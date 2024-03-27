package ir.tac;

import ir.cfg.TACVisitor;

public class AddA extends TAC {
    private int id;
    private Variable dest;
    private Value left;
    private Value right;

    public AddA(int id, Variable dest, Value left, Value right) {
        super(id, dest);
        this.id = id;
        this.dest = dest;
        this.left = left;
        this.right = right;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

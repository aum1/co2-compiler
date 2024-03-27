package ir.tac;

import ir.cfg.TACVisitor;

public class BNE extends TAC {
    private int id;
    private Variable left;
    private int right;

    public BNE(int id, Variable left, int right) {
        super(id);
        this.id = id;
        this.left = left;
        this.right = right;
    }

    public Variable getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

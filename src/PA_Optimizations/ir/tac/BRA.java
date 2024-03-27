package ir.tac;

import ir.cfg.TACVisitor;

public class BRA extends TAC {
    private int id;
    private int right;

    public BRA(int id,int right) {
        super(id);
        this.id = id;
        this.right = right;
    }

    public int getRight() {
        return right;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

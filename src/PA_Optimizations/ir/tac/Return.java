package ir.tac;

import ir.cfg.TACVisitor;

public class Return extends TAC {
    private int id;
    // private Expression dest;

    public Return(int id) {
        super(id);
        this.id = id;
    }

    // public BasicBlock getDest() {
    //     return dest;
    // }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

package ir.tac;

import ir.cfg.TACVisitor;

public class Return extends TAC {
    private int id;
    private boolean hasReturnValue;
    private Value returnValue;
    // private Expression dest;

    public Return(int id) {
        super(id);
        this.id = id;
        this.hasReturnValue = false;
    }

    public Return(int id, Value returnValue) {
        super(id);
        this.id = id;
        this.hasReturnValue = true;
        this.returnValue = returnValue;
    }

    public boolean hasReturnValue() {
        return hasReturnValue;
    }

    public Value getReturnValue() {
        return returnValue;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

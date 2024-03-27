package ir.tac;

import co2.Symbol;
import ir.cfg.BasicBlock;
import ir.cfg.TACVisitor;

public class Call extends TAC {
    private int id;
    private BasicBlock dest;
    private boolean isPredefined;
    private Symbol functionName;

    public Call(int id, BasicBlock dest, Symbol functionName) {
        super(id);
        this.id = id;
        this.dest = dest;
        this.functionName = functionName;
        this.isPredefined = false;
    }

    public Call(int id, Symbol functionName) {
        super(id);
        this.id = id;
        this.functionName = functionName;
        this.isPredefined = true;
    }

    public Symbol getFunctionName() {
        return functionName;
    }

    public boolean isPredefined() {
        return this.isPredefined;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

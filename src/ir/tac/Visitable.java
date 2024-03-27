package ir.tac;

import ir.cfg.TACVisitor;

public interface Visitable {
    
    public void accept(TACVisitor visitor);
}   
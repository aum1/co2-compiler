package ir.cfg;

public interface Visitable {
    
    public void accept (CFGVisitor visitor);
}
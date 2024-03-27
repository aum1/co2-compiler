package ir.tac;

import ir.cfg.TACVisitor;

public class Assign extends TAC {
    
    private Variable dest; // lhs
    private Value right; // operand_2

    public Assign(int id, Variable dest, Value right) {
        super(id, dest);
        this.dest = dest;
        this.right = right;
    }
    
    public Assign(int id, Variable dest) {
        super(id, dest);
        this.dest = dest;
    }

    public Variable getDest() {
        return dest;
    }
    
    public Value getRight() {
        return right;
    }

    @Override
    public void accept(TACVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}
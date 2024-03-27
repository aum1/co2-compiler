package ir.tac;

public abstract class TAC implements Visitable {
    
    private int id; // instruction id
    private Variable dest;

    private boolean eliminated; // if this instruction is not needed by any optimization, 
                                // note: do not physically remove instructions

    protected TAC(int id) {
        this.id = id;
        this.eliminated = false;
    }

    protected TAC(int id, Variable dest) {
        this.id = id;
        this.dest = dest;
        this.eliminated = false;

        // saving code position will be helpful in debugging
    }

    public int getID() {
        return id;
    }

    public Variable getDest() {
        return dest;
    }
}
package ir.tac;

import ir.cfg.BasicBlock;
import ir.cfg.TACVisitor;

public class BRA extends TAC {
    private int id;
    private int right;
    private BasicBlock trueBlockBranch;

    public BRA(int id,int right) {
        super(id);
        this.id = id;
        this.right = right;
    }

    public BRA(int id, BasicBlock right) {
        super(id);
        this.id = id;
        this.right = right.getID();
        this.trueBlockBranch = right;
    }

    public int getRight() {
        return right;
    }

    public void setTrueBasicBlock(BasicBlock trueBlockBranch) {
        this.trueBlockBranch = trueBlockBranch;
    }

    public BasicBlock getTrueBasicBlock() {
        return trueBlockBranch;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

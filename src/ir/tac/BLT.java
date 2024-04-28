package ir.tac;

import ir.cfg.BasicBlock;
import ir.cfg.TACVisitor;

public class BLT extends TAC {
    private int id;
    private Variable left;
    private int right;
    private BasicBlock trueBlockBranch;
    private BasicBlock falseBlockBranch;

    public BLT(int id, Variable left, int right) {
        super(id);
        this.id = id;
        this.left = left;
        this.right = right;
    }

    public BLT(int id, Variable left, BasicBlock right) {
        super(id);
        this.id = id;
        this.left = left;
        this.right = right.getID();
        this.trueBlockBranch = right;
    }

    public BLT(int id, Variable left, BasicBlock trueBlock, BasicBlock falseBlock) {
        super(id);
        this.id = id;
        this.left = left;
        this.right = trueBlock.getID();
        this.trueBlockBranch = trueBlock;
        this.falseBlockBranch = falseBlock;
    }

    public Variable getLeft() {
        return left;
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

    public void setFalseBasicBlock(BasicBlock trueBlockBranch) {
        this.trueBlockBranch = trueBlockBranch;
    }

    public BasicBlock getFalseBasicBlock() {
        return falseBlockBranch;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

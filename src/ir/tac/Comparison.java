package ir.tac;

import co2.Symbol;
import ir.cfg.TACVisitor;

public class Comparison extends TAC {
    private int id;
    private Variable dest;
    private Symbol comparisonOperator;
    private Value left;
    private Value right;

    public Comparison(int id, Variable dest, Value left, Value right, Symbol comparisonOperator) {
        super(id, dest);
        this.id = id;
        this.dest = dest;
        this.left = left;
        this.right = right;
        this.comparisonOperator = comparisonOperator;
    }

    public Variable getDest() {
        return dest;
    }

    public Value getLeft() {
        return left;
    }

    public Value getRight() {
        return right;
    }

    public Symbol getComparisonOperator() {
        return comparisonOperator;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

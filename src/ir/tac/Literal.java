package ir.tac;

import co2.Symbol;
import co2.Token.Kind;
import ir.cfg.TACVisitor;


public class Literal implements Value {
    private Symbol val;

    public Literal(Symbol symbol) {
        this.val = symbol;
    }

    public Symbol getValue() {
        return val;
    }

    public String toString() {
        return val.token().lexeme();
    }

    @Override
    public void accept(TACVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public int getMachineCodeRepresentation() {
        return Integer.valueOf(val.token().lexeme());
    }

    public float getMachineCodeFloatRepresentation() {
        return Float.valueOf(val.token().lexeme());
    }

    @Override
    public boolean isFloat() {
        return (val.token().kind() == Kind.FLOAT_VAL);
    }

    @Override
    public boolean isBool() {
        return (val.token().kind() == Kind.BOOL);
    }

    @Override
    public boolean isInt() {
        return (val.token().kind() == Kind.INT_VAL);
    }

    @Override
    public Symbol getSymbol() {
        return val;
    }
}
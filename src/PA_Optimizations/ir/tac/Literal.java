package ir.tac;

import co2.Symbol;
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
    public Symbol getSymbol() {
        return val;
    }
}
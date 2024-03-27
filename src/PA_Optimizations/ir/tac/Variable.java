package ir.tac;

import co2.Symbol;
import ir.cfg.TACVisitor;

public class Variable implements Value {

    private Symbol sym;

    public Variable(Symbol sym) {
        this.sym = sym;
    }

    public Symbol getSymbol() {
        return sym;
    }

    public String toString() {
        return sym.token().lexeme();
    }

    @Override
    public void accept(TACVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }}
package ir.tac;

import co2.Symbol;

public interface Value extends Visitable {
    public String toString();
    public Symbol getSymbol();
}
package ir.tac;

import co2.Symbol;

public interface Value extends Visitable {
    public String toString();
    public Symbol getSymbol();
    public boolean isFloat();
    public boolean isInt();
    public boolean isBool();
    public int getMachineCodeRepresentation();
    public float getMachineCodeFloatRepresentation();
}
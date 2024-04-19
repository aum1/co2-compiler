package ir.tac;

import co2.Symbol;
import co2.Token.Kind;
import ir.cfg.TACVisitor;

public class Variable implements Value {

    private Symbol sym;
    private int registerNumber;
    private boolean isBool;
    private boolean isFloat;
    private boolean isInt;

    public Variable(Symbol sym) {
        this.sym = sym;
    }

    public Symbol getSymbol() {
        return sym;
    }

    public String toString() {
        return sym.token().lexeme();
    }

    public int getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(int registerNumber) {
        this.registerNumber = registerNumber;
    }

    @Override
    public int getMachineCodeRepresentation() {
        return registerNumber;
    }

    public void setIsInt(boolean isInt) {
        this.isInt = isInt;
    }

    public void setIsFloat(boolean isInt) {
        this.isFloat = isInt;
    }
    
    public void setIsBool(boolean isInt) {
        this.isBool = isInt;
    }

    @Override
    public boolean isFloat() {
        return isFloat;
    }

    @Override
    public boolean isBool() {
        return isBool;
    }

    @Override
    public boolean isInt() {
        return isInt;
    }

    @Override
    public void accept(TACVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }}
package ast;

import co2.Symbol;

public class Dereference extends Node implements Expression {
    private Symbol symbol;
    private String address;

    public Dereference(int lineNum, int charPos, Symbol symbol, String address) {
        super(lineNum, charPos);
        this.symbol = symbol;
        this.address = address;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

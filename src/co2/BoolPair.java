package co2;

public class BoolPair implements Pair {
    private Symbol first;
    private boolean second;
    public BoolPair(Symbol first, boolean second) {
        this.first = first;
        this.second = second;
    }

    public Symbol symbol() {
        return first;
    }

    public boolean value() {
        return second;
    }

    public void setValue(boolean newVal) {
        this.second = newVal;
    }
}

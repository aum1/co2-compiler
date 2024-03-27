package co2;

public class IntPair implements Pair{
    private Symbol first;
    private int second;
    public IntPair(Symbol first, int second) {
        this.first = first;
        this.second = second;
    }

    public Symbol symbol() {
        return first;
    }

    public int value() {
        return second;
    }

    public void setValue(int newVal) {
        this.second = newVal;
    }
}

package co2;

public class IntArrayPair implements Pair {
    private Symbol first;
    private int[] second;
    public IntArrayPair(Symbol first, int[] second) {
        this.first = first;
        this.second = second;
    }

    public Symbol symbol() {
        return first;
    }

    public int[] value() {
        return second;
    }

    public int value(int index) {
        return this.second[index];
    }

    public void setValue(int index, int value) {
        this.second[index] = value;
    }
}

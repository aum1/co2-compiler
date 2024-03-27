package co2;

public class FloatPair implements Pair {
    private Symbol first;
    private float second;
    public FloatPair(Symbol first, float second) {
        this.first = first;
        this.second = second;
    }

    public Symbol symbol() {
        return first;
    }

    public float value() {
        return second;
    }

    public void setValue(float newVal) {
        this.second = newVal;
    }
}

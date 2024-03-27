package co2;

public class Symbol {

    private Token token;

    // TODO: Add other parameters like type

    public Symbol (Token token) {
        this.token = token;
    }
    public Token token () {
        return token;
    }
}

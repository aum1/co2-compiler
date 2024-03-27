package co2;

public class SymbolNotFoundError extends Error{
    private static final long serialVersionUID = 1L;
    private final String name;
    private boolean hasWrongArgs;

    public SymbolNotFoundError (String name) {
        super("Symbol " + name + " not found.");
        this.name = name;
    }
    public SymbolNotFoundError (String name, boolean hasWrongArgs) {
        super("Symbol " + name + " not found.");
        this.hasWrongArgs = hasWrongArgs;
        this.name = name;
    }

    public boolean wrongArgs() {
        return hasWrongArgs;
    }

    public String name () {
        return name;
    }
}

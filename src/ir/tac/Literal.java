package ir.tac;

import co2.Symbol;
import co2.Token.Kind;
import ir.cfg.TACVisitor;


public class Literal implements Value {
    private Symbol val;

    public Literal(Symbol symbol) {
        this.val = symbol;
    }

    public Symbol getValue() {
        return val;
    }

    public String toString() {
        return val.token().lexeme();
    }

    @Override
    public void accept(TACVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
public int getMachineCodeRepresentation() {
    String lexeme = val.token().lexeme();
    try {
        // First, try to parse the lexeme as an integer directly.
        return Integer.parseInt(lexeme);
    } catch (NumberFormatException e1) {
        try {
            // If the lexeme is not a valid integer, try parsing it as a float and cast to int.
            float floatValue = Float.parseFloat(lexeme);
            return (int) floatValue;
        } catch (NumberFormatException e2) {
            // Handle the case where the lexeme might be a boolean.
            if (lexeme.equalsIgnoreCase("true")) {
                return 1;  // Commonly, true is represented as 1.
            } else if (lexeme.equalsIgnoreCase("false")) {
                return 0;  // Commonly, false is represented as 0.
            }
            // If none of the above, throw an exception or handle the case where the lexeme is neither int, float, nor boolean.
            throw new UnsupportedOperationException("Unsupported type or invalid format for machine code representation: " + lexeme);
        }
    }
}

    @Override
    public float getMachineCodeFloatRepresentation() {
        return Float.valueOf(val.token().lexeme());
    }

    @Override
    public boolean isFloat() {
        return (val.token().kind() == Kind.FLOAT_VAL);
    }

    @Override
    public boolean isBool() {
        return (val.token().kind() == Kind.TRUE || val.token().kind() == Kind.FALSE);
    }

    @Override
    public boolean isInt() {
        return (val.token().kind() == Kind.INT_VAL);
    }

    @Override
    public Symbol getSymbol() {
        return val;
    }
}
package ast;

import co2.Symbol;
import java.util.List;

public class VariableReference extends Node implements Expression {
    private Symbol ident;
    private List<Expression> dimensions;
    private boolean hasDimensions;
    private boolean isInt;
    private boolean isFloat;
    private boolean isBool;

    public VariableReference(int lineNum, int charPos, Symbol ident) {
        super(lineNum, charPos);
        this.ident = ident;
        hasDimensions = false;
    }

    public VariableReference(int lineNum, int charPos, Symbol ident, List<Expression> dimensions) {
        super(lineNum, charPos);
        this.ident = ident;
        this.dimensions = dimensions;
        hasDimensions = true;
    }

    public Symbol getIdent() {
        return ident;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
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

    public boolean isInt() {
        return isInt;
    }

    public boolean isBool() {
        return isBool;
    }

    public boolean isFloat() {
        return isFloat;
    }
}

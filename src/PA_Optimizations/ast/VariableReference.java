package ast;

import co2.Symbol;
import java.util.List;

public class VariableReference extends Node implements Expression {
    private Symbol ident;
    private List<Expression> dimensions;
    private boolean hasDimensions;

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
    
    
    
}

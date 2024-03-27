package ast;

import co2.Symbol;
import java.util.List;

public class VariableDeclaration extends Node implements Declaration {
    private Symbol ident;
    private Symbol type;
    private List<Expression> dimensions;

    public VariableDeclaration(int lineNum, int charPos, Symbol ident, Symbol type) {
        super(lineNum, charPos);
        this.ident = ident;
        this.type = type;
    }

    public VariableDeclaration(int lineNum, int charPos, Symbol ident, Symbol type, List<Expression> dimensions) {
        super(lineNum, charPos);
        this.ident = ident;
        this.type = type;
        this.dimensions = dimensions;
    }

    public Symbol getIdent() {
        return ident;
    }

    public Symbol getType() {
        return type;
    }
    
    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}

package ir.tac;

import java.util.List;

import ast.ArgumentList;
import co2.Symbol;
import ir.cfg.BasicBlock;
import ir.cfg.TACVisitor;

public class Call extends TAC {
    private int id;
    private BasicBlock destinationBlock;
    private boolean isPredefined;
    private Symbol functionName;
    private ArgumentList args;
    private List<Symbol> expressionList;
    private boolean hasArgs;
    private Variable returnVariable;

    public Call(int id, BasicBlock dest, Symbol functionName) {
        super(id);
        this.id = id;
        this.destinationBlock = dest;
        this.functionName = functionName;
        this.isPredefined = false;
        this.hasArgs = false;
    }

    public Call(int id, Symbol functionName, ArgumentList args, Variable dest) {
        super(id);
        this.id = id;
        this.functionName = functionName;
        this.isPredefined = true;
        this.args = args;
        this.hasArgs = true;
        this.returnVariable = dest;
    }

    public Call(int id, BasicBlock dest, Symbol functionName, ArgumentList args) {
        super(id);
        this.id = id;
        this.destinationBlock = dest;
        this.functionName = functionName;
        this.isPredefined = false;
        this.args = args;
        this.hasArgs = true;
    }

    public Call(int id, Symbol functionName) {
        super(id);
        this.id = id;
        this.functionName = functionName;
        this.isPredefined = true;
        this.hasArgs = false;
    }

    public Call(int id, Symbol functionName, ArgumentList args) {
        super(id);
        this.id = id;
        this.functionName = functionName;
        this.isPredefined = true;
        this.args = args;
        this.hasArgs = true;
    }

    public Symbol getFunctionName() {
        return functionName;
    }

    public Variable getReturnVariable() {
        return returnVariable;
    }

    public void setReturnVariable(Variable v) {
        this.returnVariable = v;
    }

    public boolean isPredefined() {
        String currFunctionName = functionName.token().lexeme();
        return (currFunctionName.equals("printInt") || currFunctionName.equals("printFloat") || currFunctionName.equals("printBool")
            || currFunctionName.equals("readInt") || currFunctionName.equals("readFloat") || currFunctionName.equals("readBool") || currFunctionName.equals("println"));
    }

    public BasicBlock getDestinationBlock() {
        return destinationBlock;
    }

    public boolean hasArgs() {
        return this.hasArgs;
    }

    public ArgumentList getArgs() {
        return this.args;
    }

    @Override
    public void accept(TACVisitor visitor) {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }
}

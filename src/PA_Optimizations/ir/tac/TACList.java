package ir.tac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import co2.Symbol;

public class TACList implements Iterable<TAC>{
    private List<TAC> instructions;
    private Variable latestVariable;
    private static int nextTACNumber = 0;

    public TACList(List<TAC> instructions) {
        this.instructions = instructions;
    }

    public TACList(List<TAC> instructions, Variable latestVariable) {
        this.instructions = instructions;
        this.latestVariable = latestVariable;
    }

    public TACList() {
        this.instructions = new ArrayList<>();
    }

    public void addInstruction(TAC newInstruction) {
        this.instructions.add(newInstruction);
    }

    public List<TAC> getInstructions() {
        return this.instructions;
    }

    public List<TAC> getReversedInstructions() {
        List<TAC> reversed = this.instructions;
        Collections.reverse(reversed);
        return reversed;
    }

    public Variable getLatestVariable() {
        return this.latestVariable;
    }

    public void setLatestVariable(Variable newVariable) {
        this.latestVariable = newVariable;
    }

    @Override
    public Iterator<TAC> iterator() {
        return this.instructions.iterator();
    }

    public static int getNextTACNumber() {
        int toReturn = nextTACNumber;
        nextTACNumber++;
        return toReturn;
    }
}

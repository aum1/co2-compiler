package ir.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import ir.tac.TAC;
import ir.tac.TACList;

import java.util.List;
import java.util.Map;

public class BasicBlock extends Block implements Iterable<TAC> {
    private int num; // block number;
    private TACList instructions;
    private List<BasicBlock> predecessors;
    // private List<BasicBlock> successors;
    private Map<BasicBlock, String> successors;
    private HashMap<String, BasicBlock> functionsMap;
    private ArrayList<Integer> machineInstructions;
    private int machineInstructionsStartingPosition;

    private static int nextBlockNumber = 0;
    private static int nextTemporaryNumber = 0;

    public BasicBlock(int num, TACList instructions, List<BasicBlock> predecessors, Map<BasicBlock, String> successors) {
        this.num = num;
        this.instructions = instructions;
        this.predecessors = predecessors;
        this.successors = successors;
        machineInstructions = new ArrayList<>();
        machineInstructionsStartingPosition = 0;
        this.visited = false;

        if (this.predecessors == null) {
            this.predecessors = new ArrayList<>();
        }
        if (this.successors == null) {
            this.successors = new HashMap<>();
        }
    }

    public BasicBlock(int num, TACList instructions, List<BasicBlock> predecessors, Map<BasicBlock, String> successors, HashMap<String, BasicBlock> functionsMap) {
        this.num = num;
        this.instructions = instructions;
        this.predecessors = predecessors;
        this.successors = successors;
        this.functionsMap = functionsMap;
        machineInstructions = new ArrayList<>();
        machineInstructionsStartingPosition = 0;
        this.visited = false;

        if (this.predecessors == null) {
            this.predecessors = new ArrayList<>();
        }
        if (this.successors == null) {
            this.successors = new HashMap<>();
        }
    }

    public int getID() {
        return num;
    }

    public void addInstruction(TAC newInstruction) {
        this.instructions.addInstruction(newInstruction);
    }

    public void addMachineInstruction(Integer newInstruction) {
        this.machineInstructions.add(newInstruction);
    }

    public ArrayList<Integer> getMachineInstructions() {
        return this.machineInstructions;
    }

    public void setMachineInstructions(ArrayList<Integer> newMachineInstructions) {
        this.machineInstructions = newMachineInstructions;
    }

    public void removeMachineInstruction(int index) {
        this.machineInstructions.remove(index);
    }

    public void setMachineInstructionsStartingPosition(int newPos) {
        this.machineInstructionsStartingPosition = newPos;
    }

    public int getMachineInstructionsStartingPosition() {
        return machineInstructionsStartingPosition;
    }

    public void setInstructionList(TACList newInstructionList) {
        this.instructions = newInstructionList;
    }

    public void getMachineInstructionsLength() {
        this.machineInstructions.size();
    }

    public TACList getInstructions() {
        return this.instructions;
    }

    public String asDotGraph() {
        CFGPrinter printer = new CFGPrinter();
        return printer.visit(this);
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public void addPredecessor(BasicBlock newBlock) {
        this.predecessors.add(newBlock);
    }

    public boolean removeSuccessor(int idToRemove) {
        for (BasicBlock b : this.getSuccessors().keySet()) {
            if (b.getID() == idToRemove) {
                getSuccessors().remove(b);
                return true;
            }
        }
        return false;
    }

    public boolean removePredecessor(int idToRemove) {
        for (BasicBlock b : this.getPredecessors()) {
            if (b.getID() == idToRemove) {
                getPredecessors().remove(b);
                return true;
            }
        }
        return false;
    }
    

    public Map<BasicBlock, String> getSuccessors() {
        return successors;
    }

    public void addSuccessor(BasicBlock newBlock) {
        this.successors.put(newBlock, "");
    }

    public void addSuccessor(BasicBlock newBlock, String message) {
        this.successors.put(newBlock, message);
    }

    public HashMap<String, BasicBlock> getFunctionsMap() {
        return functionsMap;
    }

    // used for head block only
    public void removeFunction(String functionName) {
        functionsMap.remove(functionName);
    }    

    @Override
    public Iterator<TAC> iterator() {
        return instructions.iterator();
    }

    @Override
    public void accept(CFGVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public void resetVisited() {
        this.visited = false;
    }

    public static int getNextBlockNumber() {
        int toReturn = nextBlockNumber;
        nextBlockNumber++;
        return toReturn;
    }

    public static int getNextTempNumber() {
        int toReturn = nextTemporaryNumber;
        nextTemporaryNumber++;
        return toReturn;   
    }
}
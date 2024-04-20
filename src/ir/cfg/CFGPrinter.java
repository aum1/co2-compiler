package ir.cfg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ast.Computation;
import ast.FunctionCall;
import ir.tac.Add;
import ir.tac.Sub;
import ir.tac.TAC;
import ir.tac.TACList;
import ir.tac.Neg;
import ir.tac.BEQ;
import ir.tac.BGE;
import ir.tac.BGT;
import ir.tac.BLE;
import ir.tac.BLT;
import ir.tac.BNE;
import ir.tac.BRA;
import ir.tac.Call;
import ir.tac.Assign;
import ir.tac.Comparison;
import ir.tac.Literal;
import ir.tac.Value;
import ir.tac.Variable;
import ir.tac.And;
import ir.tac.Div;
import ir.tac.Mod;
import ir.tac.Mul;
import ir.tac.Or;
import ir.tac.Pow;
import ir.tac.Return;

// To print basic block in Dot language
public class CFGPrinter implements CFGVisitor {
    Set<String> transitionStrings = new HashSet<String>();
    Set<Integer> blocksVisited = new HashSet<Integer>();

    @Override
    public String visit(BasicBlock node) {

        // Preamble and computation block
        String toReturn = "digraph G { \n node[shape=record]; \n "; 
        toReturn += "subgraph cluster_" + node.getID() + " { \n";

        // add instructions in dotgraph format
        List<TAC> instructionList = node.getInstructions().getInstructions();
        for (int i = 0; i < instructionList.size(); i++) {
            String currNodeTransition = "\"" + instructionList.get(i).getID() + ": " + visit(instructionList.get(i)) + "\" -> ";
            if (!(transitionStrings.contains(currNodeTransition))) {
                toReturn += currNodeTransition;
                transitionStrings.add(currNodeTransition);
            }
        }

        // remove last arrow
        toReturn = toReturn.substring(0, toReturn.length()-4);
        toReturn += "; \n label = \"bb" + node.getID() + "\";\n } \n";

        // create transition from start block to first subgraph
        String currTransition = "start -> \"" + instructionList.get(0).getID() + ": " + visit(instructionList.get(0)) + "\"\n";
        transitionStrings.add(currTransition);
        toReturn += currTransition;

        // for all functions, get the subgraph
        for (String functionName : node.getFunctionsMap().keySet()) {
            String currSuccessorString = getSubGraph(node.getFunctionsMap().get(functionName));
            toReturn += currSuccessorString;
        }

        // for all successors, get the subgraph if it exists
        if ((node.getSuccessors() != null)) {
            for (BasicBlock currNodeSuccessor : node.getSuccessors().keySet()) {
                String currSuccessorString = getSubGraph(currNodeSuccessor);
                toReturn += currSuccessorString;

                // add arrow from end of parent node to beginning of successor node
                if (currNodeSuccessor.getInstructions().getInstructions().size() > 0) {
                    currTransition = "\"" + instructionList.get(instructionList.size()-1).getID() + ": " + visit(instructionList.get(instructionList.size()-1)) + "\" -> \""+ currNodeSuccessor.getInstructions().getInstructions().get(0).getID() + ": " + visit(currNodeSuccessor.getInstructions().getInstructions().get(0)) + "\" [label=\"" + node.getSuccessors().get(currNodeSuccessor) + "\"]" +"\n";
                    toReturn += currTransition;
                    transitionStrings.add(currTransition);
                }
                
            }
        }
        
        toReturn += "}";
        return toReturn;
    }

    public String getSubGraph(BasicBlock node) {
        String toReturn = "";
        toReturn += "subgraph cluster_" + node.getID() + " { \n";

        if (blocksVisited.contains(node.getID())) {
            return "";
        }
        else {
            blocksVisited.add(node.getID());
        }

        // add instructions in dotgraph format
        List<TAC> instructionList = node.getInstructions().getInstructions();
        for (int i = 0; i < instructionList.size(); i++) {
            String currNodeTransition = "\"" + instructionList.get(i).getID() + ": " + visit(instructionList.get(i)) + "\" -> ";
            if (!(transitionStrings.contains(currNodeTransition))) {
                toReturn += currNodeTransition;
                transitionStrings.add(currNodeTransition);
            }
        }

        // remove last arrow
        if (toReturn.contains("->")) {
            toReturn = toReturn.substring(0, toReturn.length()-4);
            toReturn += "; \n label = \"bb" + node.getID() + "\";\n } \n";
        }
        else {
            // remove last semicolon if block is empty
            toReturn += "\n label = \"bb" + node.getID() + "\";\n } \n";
        }

        if (node.getSuccessors() != null) {
            for (BasicBlock currNodeSuccessor : node.getSuccessors().keySet()) {
                if (currNodeSuccessor.getInstructions().getInstructions().size() == 0) {
                    continue;
                }

                if (!(currNodeSuccessor.getID() == node.getID())) {
                    if (!(currNodeSuccessor.getSuccessors().containsKey(node))) {
                        String currSuccessorString = getSubGraph(currNodeSuccessor);
                        toReturn += currSuccessorString;
                    }
                }
                 
                // add arrow from end of parent node to beginning of successor node
                if (instructionList.size() > 0) {
                    String currTransition = "\"" + instructionList.get(instructionList.size()-1).getID() + ": " + visit(instructionList.get(instructionList.size()-1)) + "\" -> \""+ currNodeSuccessor.getInstructions().getInstructions().get(0).getID() + ": " + visit(currNodeSuccessor.getInstructions().getInstructions().get(0)) + "\" [label=\"" + node.getSuccessors().get(currNodeSuccessor) + "\"]" +"\n";
                    if (!(transitionStrings.contains(currTransition))) {
                        toReturn += currTransition;
                        transitionStrings.add(currTransition);
                    }
                    
                }
                
            }
        }

        return toReturn;
    }

    @Override
    public String visit(Add node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " + " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Sub node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " - " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Mul node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " * " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Div node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " / " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Mod node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " % " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Pow node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " ^ " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(And node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " && " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Or node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " || " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Neg node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = !" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Comparison node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getLeft() + " " + node.getComparisonOperator().token().lexeme() + " " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(BEQ node) {
        String toReturn = "beq " + node.getLeft() + "? bb" + node.getTrueBasicBlock().getID() + " : bb" + node.getFalseBasicBlock().getID();
        return toReturn;
    }

    @Override
    public String visit(BGE node) {
        String toReturn = "bge " + node.getLeft() + " bb" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(BGT node) {
        String toReturn = "bgt " + node.getLeft() + " bb" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(BLE node) {
        String toReturn = "ble " + node.getLeft() + " bb" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(BLT node) {
        String toReturn = "blt " + node.getLeft() + " bb" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(BNE node) {
        String toReturn = "bne " + node.getLeft() + " bb" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(BRA node) {
        String toReturn = "branch bb" + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Assign node) {
        String toReturn = node.getDest().getSymbol().token().lexeme() + " = " + node.getRight();
        return toReturn;
    }

    @Override
    public String visit(Return node) {
        String toReturn = "ret";
        return toReturn;
    }

    @Override
    public String visit(Call node) {
        String toReturn = "call " + node.getFunctionName().token().lexeme();
        return toReturn;
    }

    @Override
    public String visit(Literal node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    public String visit(TAC node) {
        if (node instanceof Add) {
            return visit((Add) node);
        }
        else if (node instanceof Sub) {
            return visit((Sub) node);
        }
        else if (node instanceof Mul) {
            return visit((Mul) node);
        }
        else if (node instanceof Div) {
            return visit((Div) node);
        }
        else if (node instanceof Mod) {
            return visit((Mod) node);
        }
        else if (node instanceof And) {
            return visit((And) node);
        }
        else if (node instanceof Or) {
            return visit((Or) node);
        }
        else if (node instanceof Neg) {
            return visit((Neg) node);
        }
        else if (node instanceof Comparison) {
            return visit((Comparison) node);
        }
        else if (node instanceof Assign) {
            return visit((Assign) node);
        }
        else if (node instanceof Return) {
            return visit((Return) node);
        }
        else if (node instanceof Call) {
            return visit((Call) node);
        }
        else if (node instanceof BEQ) {
            return visit((BEQ) node);
        }
        else if (node instanceof BNE) {
            return visit((BNE) node);
        }
        else if (node instanceof BGE) {
            return visit((BGE) node);
        }
        else if (node instanceof BGT) {
            return visit((BGT) node);
        }
        else if (node instanceof BLE) {
            return visit((BLE) node);
        }
        else if (node instanceof BLT) {
            return visit((BLT) node);
        }
        else if (node instanceof BRA) {
            return visit((BRA) node);
        }
        else {
            return "EMPTY";
        }
    }

    public static void LegiblePrint(BasicBlock block) {
        LegiblePrint(block.getInstructions());
    }

    public static void LegiblePrint(TACList currInstructions) {
        // TACList currInstructions = block.getInstructions();
        System.out.println("My instructions");
        for (int i = 0; i < currInstructions.getInstructions().size(); i++) {
            TAC instruction = currInstructions.getInstructions().get(i);
            String destStringPredecessor = "";
            String leftStringPredecessor = "";
            String rightStringPredecessor = "";
            if (instruction.getDest() instanceof Variable) {
                destStringPredecessor = "R";
            }
            else {
                destStringPredecessor = "$";
            }

            if (currInstructions.getInstructions().get(i) instanceof Assign) {
                if (((Assign) instruction).getRight() instanceof Variable) {
                    rightStringPredecessor = "R";
                }
                else {
                    rightStringPredecessor = "$";
                }
                System.out.println(((Assign) instruction).getID() + ":" + destStringPredecessor + ((Assign) instruction).getDest().getMachineCodeRepresentation() + "=" + rightStringPredecessor + ((Assign) instruction).getRight().getMachineCodeRepresentation());
            }
            if (currInstructions.getInstructions().get(i) instanceof Add) {
                if (((Add) instruction).getRight() instanceof Variable) {
                    rightStringPredecessor = "R";
                }
                else {
                    rightStringPredecessor = "$";
                }

                if (((Add) instruction).getLeft() instanceof Variable) {
                    leftStringPredecessor = "R";
                }
                else {
                    leftStringPredecessor = "$";
                }

                System.out.println(((Add) instruction).getID() + ":" + destStringPredecessor + ((Add) instruction).getDest().getMachineCodeRepresentation() + " = " + rightStringPredecessor + ((Add) instruction).getLeft().getMachineCodeRepresentation() + " + " + leftStringPredecessor + ((Add) instruction).getRight().getMachineCodeRepresentation());
            }
            if (currInstructions.getInstructions().get(i) instanceof Sub) {
                if (((Sub) instruction).getRight() instanceof Variable) {
                    rightStringPredecessor = "R";
                }
                else {
                    rightStringPredecessor = "$";
                }

                if (((Sub) instruction).getLeft() instanceof Variable) {
                    leftStringPredecessor = "R";
                }
                else {
                    leftStringPredecessor = "$";
                }

                System.out.println(((Sub) instruction).getID() + ":" + destStringPredecessor + ((Sub) instruction).getDest().getMachineCodeRepresentation() + " = " + rightStringPredecessor + ((Sub) instruction).getLeft().getMachineCodeRepresentation() + " - " + leftStringPredecessor + ((Sub) instruction).getRight().getMachineCodeRepresentation());
            }
            if (currInstructions.getInstructions().get(i) instanceof Mul) {
                if (((Mul) instruction).getRight() instanceof Variable) {
                    rightStringPredecessor = "R";
                }
                else {
                    rightStringPredecessor = "$";
                }

                if (((Mul) instruction).getLeft() instanceof Variable) {
                    leftStringPredecessor = "R";
                }
                else {
                    leftStringPredecessor = "$";
                }
                System.out.println(((Mul) instruction).getID() + ":" + destStringPredecessor + ((Mul) instruction).getDest().getMachineCodeRepresentation() + " = " + rightStringPredecessor + ((Mul) instruction).getLeft().getMachineCodeRepresentation() + " * " + leftStringPredecessor + ((Mul) instruction).getRight().getMachineCodeRepresentation());
            }
            if (currInstructions.getInstructions().get(i) instanceof Pow) {
                if (((Pow) instruction).getRight() instanceof Variable) {
                    rightStringPredecessor = "R";
                }
                else {
                    rightStringPredecessor = "$";
                }

                if (((Pow) instruction).getLeft() instanceof Variable) {
                    leftStringPredecessor = "R";
                }
                else {
                    leftStringPredecessor = "$";
                }
                System.out.println(((Pow) instruction).getID() + ":" + destStringPredecessor + ((Pow) instruction).getDest().getMachineCodeRepresentation() + " = " + rightStringPredecessor + ((Pow) instruction).getLeft().getMachineCodeRepresentation() + " ^ " + leftStringPredecessor + ((Pow) instruction).getRight().getMachineCodeRepresentation());
            }
            if (currInstructions.getInstructions().get(i) instanceof Mod) {
                if (((Mod) instruction).getRight() instanceof Variable) {
                    rightStringPredecessor = "R";
                }
                else {
                    rightStringPredecessor = "$";
                }

                if (((Mod) instruction).getLeft() instanceof Variable) {
                    leftStringPredecessor = "R";
                }
                else {
                    leftStringPredecessor = "$";
                }
                System.out.println(((Mod) instruction).getID() + ":" + destStringPredecessor + ((Mod) instruction).getDest().getMachineCodeRepresentation() + " = " + rightStringPredecessor + ((Mod) instruction).getLeft().getMachineCodeRepresentation() + " % " + leftStringPredecessor + ((Mod) instruction).getRight().getMachineCodeRepresentation());
            }

            // if (instruction.getDest() != null) {
            //     System.out.println("set dest: " + instruction.getDest().isBool() + " " + instruction.getDest().isFloat() + " " + instruction.getDest().isInt());
            // }
        }
    }
}
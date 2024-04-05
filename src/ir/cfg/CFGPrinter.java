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
        String toReturn = "beq " + node.getLeft() + " bb" + node.getRight();
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

            if (currInstructions.getInstructions().get(i) instanceof Assign) {
                System.out.println(((Assign) instruction).getID() + ":" + ((Assign) instruction).getDest() + "=" + ((Assign) instruction).getRight());
            }
            if (currInstructions.getInstructions().get(i) instanceof Add) {
                System.out.println(((Add) instruction).getID() + ":" + ((Add) instruction).getDest() + " = " + ((Add) instruction).getLeft() + " + " + ((Add) instruction).getRight());
            }
            if (currInstructions.getInstructions().get(i) instanceof Sub) {
                System.out.println(((Sub) instruction).getID() + ":" + ((Sub) instruction).getDest() + " = " + ((Sub) instruction).getLeft() + " - " + ((Sub) instruction).getRight());
            }
            if (currInstructions.getInstructions().get(i) instanceof Mul) {
                System.out.println(((Mul) instruction).getID() + ":" + ((Mul) instruction).getDest() + " = " + ((Mul) instruction).getLeft() + " * " + ((Mul) instruction).getRight());
            }
            if (currInstructions.getInstructions().get(i) instanceof Pow) {
                System.out.println(((Pow) instruction).getID() + ":" + ((Pow) instruction).getDest() + " = " + ((Pow) instruction).getLeft() + " ^ " + ((Pow) instruction).getRight());
            }
            if (currInstructions.getInstructions().get(i) instanceof Mod) {
                System.out.println(((Mod) instruction).getID() + ":" + ((Mod) instruction).getDest() + " = " + ((Mod) instruction).getLeft() + " % " + ((Mod) instruction).getRight());
            }
        }
    }
}
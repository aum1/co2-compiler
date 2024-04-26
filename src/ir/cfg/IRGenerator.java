package ir.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ir.tac.TAC;
import ir.tac.TACList;
import ir.tac.Value;
import ir.tac.Variable;
import types.ErrorType;
import ast.AST;
import ast.Computation;
import ast.Declaration;
import ast.DeclarationList;
import ast.IfStatement;
import ast.IntegerLiteral;
import ast.LogicalAnd;
import ast.Multiplication;
import ast.RepeatStatement;
import ast.ReturnStatement;
import ast.Statement;
import ast.StatementSequence;
import ast.Addition;
import ast.Assignment;
import ast.Subtraction;
import ast.VariableDeclaration;
import ast.VariableReference;
import ast.WhileStatement;
import ast.Division;
import ast.Expression;
import ast.FloatLiteral;
import ast.FunctionBody;
import ast.FunctionCall;
import ast.FunctionDeclaration;
import ast.Modulo;
import ast.Power;
import ast.Relation;
import ast.BoolLiteral;
import ast.LogicalAnd;
import ast.LogicalNot;
import ast.LogicalOr;
import ir.tac.Add;
import ir.tac.And;
import ir.tac.Assign;
import ir.tac.BEQ;
import ir.tac.BGE;
import ir.tac.BGT;
import ir.tac.BLE;
import ir.tac.BLT;
import ir.tac.BNE;
import ir.tac.BRA;
import ir.tac.Call;
import ir.tac.Comparison;
import ir.tac.Div;
import ir.tac.Literal;
import ir.tac.Mod;
import ir.tac.Mul;
import ir.tac.Neg;
import ir.tac.Or;
import ir.tac.Pow;
import ir.tac.Return;
import ir.tac.Sub;
import co2.Symbol;
import co2.Token;

public class IRGenerator {
    private TACList currentInstructionList;
    private boolean isFirstBlock = true;
    private BasicBlock previousBlock;
    private HashMap<String, BasicBlock> functionBlocks = new HashMap<String, BasicBlock>();

    public BasicBlock genIR(AST a) {
        BasicBlock IRHead = visit(a.getHead());
        return IRHead;
    }

    public BasicBlock visit(Computation node) {
        HashMap<String, BasicBlock> functionDeclarationBlocks = new HashMap<>();
        BasicBlock computationBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), null, null, null, functionDeclarationBlocks);
        previousBlock = computationBlock;
        
        for (Declaration d : node.functions()) {
            BasicBlock currFunction = visit(((FunctionDeclaration) d));
            functionDeclarationBlocks.put(((FunctionDeclaration) d).getFunctionName().token().lexeme(), currFunction);
        }
        
        if (isFirstBlock) {
            computationBlock.setInstructionList(currentInstructionList);
        }
        
        computationBlock.setInstructionList(new TACList());
        currentInstructionList = computationBlock.getInstructions();

        previousBlock = computationBlock;
        visit(node.mainStatementSequence());
        return computationBlock;
    }

    public BasicBlock visit(FunctionDeclaration node) {
        BasicBlock functionBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), null, null, null);
        currentInstructionList = new TACList();
        previousBlock = functionBlock;
        visit(node.getBody());
        if (functionBlock.getInstructions() == null) {
            functionBlock.setInstructionList(currentInstructionList);
        }
        
        functionBlock.addInstruction(new Return(TACList.getNextTACNumber()));
        functionBlocks.put(node.getFunctionName().token().lexeme(), functionBlock);
        return functionBlock;
    }

    public void visit(FunctionBody node) {
        if (node.hasVarDecl()) {
            visit(node.getVarDecl());
        }
        visit(node.getStatSeq());
    }

    public void visit(FunctionCall node) {
        System.out.println("Visiting FunctionCall: " + node.getFunctionName().token().lexeme());
    
        if ((node.getFunctionName().token().lexeme().equals("printInt")) || (node.getFunctionName().token().lexeme().equals("printFloat")) || (node.getFunctionName().token().lexeme().equals("printBool"))
                || (node.getFunctionName().token().lexeme().equals("readInt")) || (node.getFunctionName().token().lexeme().equals("readFloat")) || (node.getFunctionName().token().lexeme().equals("readBool")) || (node.getFunctionName().token().lexeme().equals("println"))) {
            System.out.println("Entering standard function call");
            if (node.getArgumentList().getExpressionParameters().size() > 0) {
                if (node.getArgumentList().getExpressionParameters().get(0) instanceof FunctionCall) {
                    visit(node.getArgumentList().getExpressionParameters().get(0));
                }
            }
            currentInstructionList.addInstruction(new Call(TACList.getNextTACNumber(), node.getFunctionName(), node.getArgumentList()));
            System.out.println("LatestVariable might not be updated by standard function calls");
            return;
        }
    
        System.out.println("Entering user-defined function call: " + node.getFunctionName().token().lexeme());
        BasicBlock functionBlock = functionBlocks.get(node.getFunctionName().token().lexeme());
        
        if (functionBlock == null) {
            System.err.println("Error: Function block not found for " + node.getFunctionName().token().lexeme());
        } else {
            currentInstructionList.addInstruction(new Call(TACList.getNextTACNumber(), functionBlock, node.getFunctionName(), node.getArgumentList()));
            previousBlock.addSuccessor(functionBlock);
            System.out.println("Function call added to currentInstructionList");
        }
    }
    

    public void visit(ReturnStatement node) {
        if (node.hasRelation()) {
            visit(node.getRelation());
            currentInstructionList.addInstruction(new Return(BasicBlock.getNextBlockNumber(), currentInstructionList.getLatestVariable()));
        }
        else {
            currentInstructionList.addInstruction(new Return(BasicBlock.getNextBlockNumber()));
        }
    }

    public void visit(StatementSequence node) {
        for (Statement s : node) {
            visit(s);
        }
    }

    public void visit(IfStatement node) {
        Symbol relationSymbol = visit(node.getRelation());
        previousBlock.setInstructionList(currentInstructionList);
        int elseBlockID = -1;
        
        TACList thenList = new TACList();
        currentInstructionList = thenList;
        visit(node.getThenBlock());
        isFirstBlock = false;

        // create blocks 
        BasicBlock thenBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), thenList, null, null);
        BasicBlock blockAfterIf = new BasicBlock(BasicBlock.getNextBlockNumber(), new TACList(), null, null);
        BasicBlock elseBlock = null;

        currentInstructionList.addInstruction(new BRA(TACList.getNextTACNumber(), blockAfterIf));

        // if else block, then get list of else instructions
        if (node.hasElseBlock()) {
            TACList elseList = new TACList();
            currentInstructionList = elseList;
            visit(node.getElseBlock());
            currentInstructionList.addInstruction(new BRA(TACList.getNextTACNumber(), blockAfterIf));
            isFirstBlock = false;

            elseBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), elseList, null, null);
            previousBlock.addSuccessor(elseBlock, "fall-through");
            elseBlock.addSuccessor(blockAfterIf);

            elseBlock.addPredecessor(previousBlock);
            blockAfterIf.addPredecessor(elseBlock);
            elseBlockID = elseBlock.getID();
        }
        else {
            previousBlock.addSuccessor(blockAfterIf);
            blockAfterIf.addPredecessor(previousBlock);
        }
        previousBlock.addSuccessor(thenBlock, "branch");
        thenBlock.addSuccessor(blockAfterIf);

        thenBlock.addPredecessor(previousBlock);
        blockAfterIf.addPredecessor(thenBlock);

        if (relationSymbol != null) {
            if (relationSymbol.token().lexeme().equals("==")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BEQ(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BEQ(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, blockAfterIf));
                }   
            }
            if (relationSymbol.token().lexeme().equals("!=")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BNE(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BNE(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, blockAfterIf));
                }
            }
            if (relationSymbol.token().lexeme().equals("<")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BLT(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BLT(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, blockAfterIf));
                }
            }
            if (relationSymbol.token().lexeme().equals("<=")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BLE(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BLE(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, blockAfterIf));
                }
            }
            if (relationSymbol.token().lexeme().equals(">")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BGT(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BGT(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, blockAfterIf));
                }
            }
            if (relationSymbol.token().lexeme().equals(">=")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BGE(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BGE(TACList.getNextTACNumber(), previousBlock.getInstructions().getLatestVariable(), thenBlock, blockAfterIf));
                }
            }
        }
        else if (node.getRelation() instanceof BoolLiteral) {
            if (((BoolLiteral) node.getRelation()).getBoolean().token().lexeme().equals("true")) {
                previousBlock.addInstruction(new BRA(TACList.getNextTACNumber(), thenBlock));
            }
            if (((BoolLiteral) node.getRelation()).getBoolean().token().lexeme().equals("false")) {
                if (node.hasElseBlock()) {
                    previousBlock.addInstruction(new BRA(TACList.getNextTACNumber(), elseBlock));
                }
                else {
                    previousBlock.addInstruction(new BRA(TACList.getNextTACNumber(), blockAfterIf));
                }   
            }
        }

        previousBlock = blockAfterIf;
        currentInstructionList = blockAfterIf.getInstructions();
    }

    public void visit(WhileStatement node) {
        previousBlock.setInstructionList(currentInstructionList);

        TACList relationList = new TACList();
        currentInstructionList = relationList;
        Symbol relationSymbol = visit(node.getRelation());

        // get then list of then instructions
        TACList innerStatSeq = new TACList();
        currentInstructionList = innerStatSeq;
        
        visit(node.getStatSeq());
        isFirstBlock = false;

        // create blocks 
        BasicBlock relationBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), relationList, null, null);
        BasicBlock statSeqBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), innerStatSeq, null, null);
        BasicBlock blockAfterWhile = new BasicBlock(BasicBlock.getNextBlockNumber(), new TACList(), null, null);

        // if else block, then get list of else instructions
        previousBlock.addSuccessor(relationBlock, "while condition");
        relationBlock.addSuccessor(statSeqBlock, "Branch inside while loop");
        relationBlock.addSuccessor(blockAfterWhile, "Fall through");
        // statSeqBlock.addSuccessor(blockAfterWhile);
        statSeqBlock.addSuccessor(relationBlock, "Loop condition");

        relationBlock.addPredecessor(previousBlock);
        statSeqBlock.addPredecessor(relationBlock);
        blockAfterWhile.addPredecessor(relationBlock);
        blockAfterWhile.addPredecessor(statSeqBlock);
        statSeqBlock.addPredecessor(statSeqBlock);
        relationBlock.addPredecessor(statSeqBlock);
        
        if (relationSymbol != null) {
            if (relationSymbol.token().lexeme().equals("==")) {
                relationBlock.addInstruction(new BEQ(TACList.getNextTACNumber(), relationBlock.getInstructions().getLatestVariable(), statSeqBlock, blockAfterWhile));
            }
            if (relationSymbol.token().lexeme().equals("!=")) {
                relationBlock.addInstruction(new BNE(TACList.getNextTACNumber(), relationBlock.getInstructions().getLatestVariable(), statSeqBlock, blockAfterWhile));
            }
            if (relationSymbol.token().lexeme().equals("<")) {
                relationBlock.addInstruction(new BLT(TACList.getNextTACNumber(), relationBlock.getInstructions().getLatestVariable(), statSeqBlock, blockAfterWhile));
            }
            if (relationSymbol.token().lexeme().equals("<=")) {
                relationBlock.addInstruction(new BLE(TACList.getNextTACNumber(), relationBlock.getInstructions().getLatestVariable(), statSeqBlock, blockAfterWhile));
            }
            if (relationSymbol.token().lexeme().equals(">")) {
                relationBlock.addInstruction(new BGT(TACList.getNextTACNumber(), relationBlock.getInstructions().getLatestVariable(), statSeqBlock, blockAfterWhile));
            }
            if (relationSymbol.token().lexeme().equals(">=")) {
                relationBlock.addInstruction(new BGE(TACList.getNextTACNumber(), relationBlock.getInstructions().getLatestVariable(), statSeqBlock, blockAfterWhile));
            }
        }
        else if (node.getRelation() instanceof BoolLiteral) {
            if (((BoolLiteral) node.getRelation()).getBoolean().token().lexeme().equals("true")) {
                relationBlock.addInstruction(new BRA(TACList.getNextTACNumber(), statSeqBlock));
            }
            if (((BoolLiteral) node.getRelation()).getBoolean().token().lexeme().equals("false")) {
                relationBlock.addInstruction(new BRA(TACList.getNextTACNumber(), blockAfterWhile));
            }
        }

        statSeqBlock.addInstruction(new BRA(TACList.getNextTACNumber(), relationBlock));
        
        previousBlock = blockAfterWhile;
        currentInstructionList = blockAfterWhile.getInstructions();
    }

    public void visit(RepeatStatement node) {
        visit(node.getRelation());

        previousBlock.setInstructionList(currentInstructionList);

        // get list of repeat instructions
        TACList innerStatSeq = new TACList();
        currentInstructionList = innerStatSeq;
        visit(node.getStatSeq());
        isFirstBlock = true;

        // create blocks
        BasicBlock statSeqBlock = new BasicBlock(BasicBlock.getNextBlockNumber(), innerStatSeq, null, null);
        BasicBlock blockAfterRepeat = new BasicBlock(BasicBlock.getNextBlockNumber(), new TACList(), null, null);

        previousBlock.addSuccessor(statSeqBlock);
        previousBlock.addSuccessor(blockAfterRepeat);
        statSeqBlock.addSuccessor(blockAfterRepeat);

        statSeqBlock.addPredecessor(previousBlock);
        blockAfterRepeat.addPredecessor(previousBlock);
        blockAfterRepeat.addPredecessor(statSeqBlock);

        previousBlock = blockAfterRepeat;
        currentInstructionList = blockAfterRepeat.getInstructions();
    }

    public void visit(Assignment node) {
        Variable dest = new Variable(node.getIdent());
        System.out.println("Before visit: " + (currentInstructionList.getLatestVariable() != null ? currentInstructionList.getLatestVariable().toString() : "null"));
        visit(node.getRelation());
        System.out.println("After visit: " + (currentInstructionList.getLatestVariable() != null ? currentInstructionList.getLatestVariable().toString() : "null"));
        
        dest.setIsBool(currentInstructionList.getLatestVariable().isBool());
        dest.setIsFloat(currentInstructionList.getLatestVariable().isFloat());
        dest.setIsInt(currentInstructionList.getLatestVariable().isInt());

        currentInstructionList.addInstruction(new Assign(TACList.getNextTACNumber(), dest, currentInstructionList.getLatestVariable()));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Addition node) {
        // get left tac
        // get right tac
        // add tacs to new Add node
        // return add node

        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new Add(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Subtraction node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());

        currentInstructionList.addInstruction(new Sub(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Multiplication node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());

        currentInstructionList.addInstruction(new Mul(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Division node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new Div(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Modulo node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new Mod(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Power node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new Pow(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(IntegerLiteral node) {
        Variable destination = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        Value rightValue = new Literal(node.getInteger());

        destination.setIsBool(false);
        destination.setIsFloat(false);
        destination.setIsInt(true);

        currentInstructionList.addInstruction(new Assign(TACList.getNextTACNumber(), destination, rightValue));
        currentInstructionList.setLatestVariable(destination);
    }

    public void visit(FloatLiteral node) {
        Variable destination = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        Value rightValue = new Literal(node.getFloat());

        destination.setIsBool(false);
        destination.setIsFloat(true);
        destination.setIsInt(false);

        currentInstructionList.addInstruction(new Assign(TACList.getNextTACNumber(), destination, rightValue));
        currentInstructionList.setLatestVariable(destination);
    }

    public void visit(BoolLiteral node) {
        // System.out.println("here");
        Variable destination = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        Value rightValue = new Literal(node.getBoolean());

        destination.setIsBool(true);
        destination.setIsFloat(false);
        destination.setIsInt(false);

        currentInstructionList.addInstruction(new Assign(TACList.getNextTACNumber(), destination, rightValue));
        currentInstructionList.setLatestVariable(destination);
    }

    public void visit(LogicalAnd node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new And(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(LogicalOr node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();

        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new Or(TACList.getNextTACNumber(), dest, leftLatest, rightLatest));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(LogicalNot node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getExpression());

        currentInstructionList.addInstruction(new Neg(TACList.getNextTACNumber(), dest, currentInstructionList.getLatestVariable()));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(Relation node) {
        Variable dest = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        visit(node.getLeftSide());
        Variable leftLatest = currentInstructionList.getLatestVariable();
        visit(node.getRightSide());
        Variable rightLatest = currentInstructionList.getLatestVariable();
        
        dest.setIsBool(leftLatest.isBool());
        dest.setIsFloat(leftLatest.isFloat());
        dest.setIsInt(leftLatest.isInt());
        
        currentInstructionList.addInstruction(new Comparison(TACList.getNextTACNumber(), dest, leftLatest, rightLatest, node.getRelation()));
        currentInstructionList.setLatestVariable(dest);
    }

    public void visit(VariableReference node) {
        // Variable destination = new Variable(new Symbol(new Token("t" + BasicBlock.getNextTempNumber(), node.charPosition(), node.lineNumber())));
        Variable rightValue = new Variable(new Symbol(node.getIdent().token()));
        rightValue.setIsBool(node.isBool());
        rightValue.setIsFloat(node.isFloat());
        rightValue.setIsInt(node.isInt());

        // Assign assignmentTAC = new Assign(TACList.getNextTACNumber(), destination, rightValue);
        // currentInstructionList.addInstruction(assignmentTAC);
        currentInstructionList.setLatestVariable(rightValue);
    }

    public void visit(Statement node) {
        if (node instanceof Assignment) {
            visit((Assignment) node);
        }
        else if (node instanceof IfStatement) {
            visit((IfStatement) node);
        }
        else if (node instanceof RepeatStatement) {
            visit((RepeatStatement) node);
        }
        else if (node instanceof ReturnStatement) {
            visit((ReturnStatement) node);
        }
        else if (node instanceof WhileStatement) {
            visit((WhileStatement) node);
        }
        else if (node instanceof FunctionCall) {
            visit((FunctionCall) node);
        }   
        // else {
        //     return null;
        // }
    }

    public Symbol visit(Expression node) {
        System.out.println("Visiting expression of type: " + node.getClass().getSimpleName());
    
        if (node instanceof LogicalNot) {
            System.out.println("Entering LogicalNot");
            visit((LogicalNot) node);
        }
        else if (node instanceof LogicalAnd) {
            System.out.println("Entering LogicalAnd");
            visit((LogicalAnd) node);
        }
        else if (node instanceof LogicalOr) {
            System.out.println("Entering LogicalOr");
            visit((LogicalOr) node);
        }
        else if (node instanceof Power) {
            System.out.println("Entering Power");
            visit((Power) node);
        }
        else if (node instanceof Multiplication) {
            System.out.println("Entering Multiplication");
            visit((Multiplication) node);
        }
        else if (node instanceof Division) {
            System.out.println("Entering Division");
            visit((Division) node);
        }
        else if (node instanceof Modulo) {
            System.out.println("Entering Modulo");
            visit((Modulo) node);
        }
        else if (node instanceof Addition) {
            System.out.println("Entering Addition");
            visit((Addition) node);
        }
        else if (node instanceof Subtraction) {
            System.out.println("Entering Subtraction");
            visit((Subtraction) node);
        }
        else if (node instanceof Relation) {
            System.out.println("Entering Relation");
            visit((Relation) node);
            System.out.println("Returning symbol from Relation");
            return ((Relation) node).getRelation();
        }
        else if (node instanceof IntegerLiteral) {
            System.out.println("Entering IntegerLiteral");
            visit((IntegerLiteral) node);
        }
        else if (node instanceof FloatLiteral) {
            System.out.println("Entering FloatLiteral");
            visit((FloatLiteral) node);
        }
        else if (node instanceof BoolLiteral) {
            System.out.println("Entering BoolLiteral");
            visit((BoolLiteral) node);
        }
        else if (node instanceof VariableReference) {
            System.out.println("Entering VariableReference");
            visit((VariableReference) node);
        }
        else if (node instanceof FunctionCall) {
            System.out.println("Entering FunctionCall");
            visit((FunctionCall) node);
        }
    
        // If reached here, it means no specific type was matched or no returnable symbol was found
        System.out.println("No specific expression type matched or no symbol to return");
        return null;
    }
    

    public void visit(DeclarationList node) {
        for (Declaration d : node) {
            visit(d);
        }
    }

    public void visit(Declaration node) {
        // if (node instanceof VariableDeclaration) {
        //     visit((VariableDeclaration) node);
        // }
        if (node instanceof FunctionDeclaration) {
            visit((FunctionDeclaration) node);
        }
    }
}

package ast;

public class PrettyPrinter implements NodeVisitor {

    private int depth = 0;
    private StringBuilder sb = new StringBuilder();

    private void println (Node n, String message) {
        // System.out.println(n.getClassInfo() + message + "\n");
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }
        sb.append(indent + n.getClassInfo() + message + "\n");
    }

    @Override
    public String toString () {
        return sb.toString();
    }

    @Override
    public void visit (StatementSequence node) {
        println(node, "");
        depth++;
        for (Statement s : node) {
            s.accept(this);
        }
        depth--;
    }

    // VariableDeclaration[input:int]
    @Override
    public void visit (VariableDeclaration node) {
        println(node, "[" + node.getIdent().token().lexeme() + ":" + node.getType().token().lexeme() + "]");
    }

    @Override
    public void visit (FunctionDeclaration node) {
        // println(node, "[" + node.getFunctionName().token().lexeme() + ":" + node.getArguments().getStringRepresentation() + "]");
        println(node, "[" + node.getFunctionName().token().lexeme() + "]");
        depth++;
        node.getBody().accept(this);
        depth--;
    }

    @Override
    public void visit (DeclarationList node) {
        if (node.empty()) return;
        println(node, "");
        depth++;
        for (Declaration d : node) {
            d.accept(this);
        }
        depth--;
    }

    @Override
    public void visit (Computation node) {
        println(node, "[" + node.main().token().lexeme() + "]");
        depth++;
        node.variables().accept(this);
        node.functions().accept(this);
        node.mainStatementSequence().accept(this);
        depth--;
    }

    @Override
    public void visit(BoolLiteral node) {
        println(node, "[" + node.getBoolean().token().lexeme() + "]");
    }

    @Override
    public void visit(IntegerLiteral node) {
        println(node, "[" + node.getInteger().token().lexeme() + "]");
    }

    @Override
    public void visit(FloatLiteral node) {
        println(node, "[" + node.getFloat().token().lexeme() + "]");
    }

    @Override
    public void visit(AddressOf node) {
        println(node, "[" + node.getSymbol().token().lexeme() + ": " + node.getAddress() + "]");
    }

    @Override
    public void visit(ArrayIndex node) {
        println(node, "[" + node.getPosition() + "]");
    }

    @Override
    public void visit(Dereference node) {
        println(node, "[" + node.getSymbol().token().lexeme() + ": " + node.getAddress() + "]");
    }

    @Override
    public void visit(LogicalNot node) {
        println(node, "");
        depth++;
        node.getExpression().accept(this);
        depth--;
    }

    @Override
    public void visit(LogicalAnd node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(LogicalOr node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Relation node) {
        println(node, "[" + node.getRelation().token().lexeme() + "]");
        depth++;
        if (node.getLeftSide() != null) {
            node.getLeftSide().accept(this);
        }
        if (node.getRightSide() != null) {
            node.getRightSide().accept(this);
        }
        
        depth--;
    }

    @Override
    public void visit(Power node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Multiplication node) {
        println(node, "[" + node.getSymbol().token().lexeme() + "]");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Division node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Modulo node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Addition node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Subtraction node) {
        println(node, "");
        depth++;
        node.getLeftSide().accept(this);
        node.getRightSide().accept(this);
        depth--;
    }

    @Override
    public void visit(Assignment node) {
        println(node, "[" + node.getIdent().token().lexeme() + ", " + node.getAssignType().token().lexeme() + "]");
        depth++;
        node.getRelation().accept(this);
        depth--;
    }

    @Override
    public void visit(ArgumentList node) {
        if (node.hasArguments()) {
            // function call
            // String output = "(";
            // for (int i = 0; i < node.getSymbolParameters().size()-1; i++) {
            //     output += node.getSymbolParameters().get(i).token().lexeme()+ ",";
            // }
            // output += node.getSymbolParameters().get(node.getSymbolParameters().size()-1).token().lexeme() + ")";
            // println(node, output);

            depth++;
            for (int i = 0; i < node.getExpressionParameters().size(); i++) {
                node.getExpressionParameters().get(i).accept(this);
            }
            depth--;
        }
        else {
            println(node, "()");
        }
    }

    @Override
    public void visit(FunctionCall node) {
        println(node, "[" + node.getFunctionName().token().lexeme() + "]");
        depth++;
        node.getArgumentList().accept(this);
        depth--;
    }

    @Override
    public void visit(FunctionBody node) {
        println(node, "");
        depth++;
        node.getStatSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(IfStatement node) {
        println(node, "");
        depth++;
        node.getRelation().accept(this);
        node.getThenBlock().accept(this);
        if (node.hasElseBlock()) {
            node.getElseBlock().accept(this);
        }
        depth--;
    }

    @Override
    public void visit(WhileStatement node) {
        println(node, "");
        depth++;
        node.getRelation().accept(this);
        node.getStatSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(RepeatStatement node) {
        println(node, "");
        depth++;
        node.getRelation().accept(this);
        node.getStatSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(ReturnStatement node) {
        println(node, "");
        depth++;
        if (node.hasRelation()) {
            node.getRelation().accept(this);
        }
        depth--;
    }

    @Override
    public void visit(VariableReference node) {
        println(node, "[" + node.getIdent().token().lexeme() + "]");
    }
}

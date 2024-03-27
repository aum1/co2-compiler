package ast;

public class AST {
    private Computation head;

    public AST(Computation head) {
        this.head = head;
    }

    public Computation getHead() {
        return head;
    }

    public String printPreOrder(){
        PrettyPrinter p = new PrettyPrinter();
        p.visit(head);
        return p.toString();
    }
}

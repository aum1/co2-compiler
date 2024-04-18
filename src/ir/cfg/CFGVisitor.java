package ir.cfg;

import ir.tac.Add;
import ir.tac.Sub;
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


public interface CFGVisitor {
    public String visit (BasicBlock node);
    
    public String visit (Add node);
    public String visit (Sub node);
    public String visit (Mul node);
    public String visit (Div node);
    public String visit (Mod node);
    public String visit (Pow node);

    public String visit (And node);
    public String visit (Or node);
    public String visit (Neg node);

    public String visit (BEQ node);
    public String visit (BGE node);
    public String visit (BGT node);
    public String visit (BLE node);
    public String visit (BLT node);
    public String visit (BNE node);
    public String visit (BRA node);
    public String visit (Comparison node);

    public String visit (Assign node);
    public String visit (Call node);

    public String visit (Literal node);
    public String visit (Return node);
}

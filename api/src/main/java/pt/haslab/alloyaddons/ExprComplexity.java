package pt.haslab.alloyaddons;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.*;

public class ExprComplexity extends VisitReturn<Void> {

    @Override
    public Void visit(ExprBinary exprBinary) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprList exprList) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprCall exprCall) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprConstant exprConstant) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprITE exprITE) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprLet exprLet) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprQt exprQt) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprUnary exprUnary) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprVar exprVar) throws Err {
        return null;
    }

    @Override
    public Void visit(Sig sig) throws Err {
        return null;
    }

    @Override
    public Void visit(Sig.Field field) throws Err {
        return null;
    }
}

package pt.haslab.alloyaddons;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.*;

import java.util.Collection;

public class ExprComplexity extends VisitReturn<Void> {

    int number_of_elements = 0;
    int number_of_variebles = 0;


    public Double getComplexity() {
        return Math.pow(number_of_elements, number_of_variebles);
    }

    @Override
    public Void visit(ExprBinary exprBinary) throws Err {
        number_of_elements += 1;
        this.visitThis(exprBinary.left);
        this.visitThis(exprBinary.right);
        return null;
    }

    @Override
    public Void visit(ExprList exprList) throws Err {
        number_of_elements += exprList.args.size();
        exprList.args.forEach(this::visitThis);
        return null;
    }

    @Override
    public Void visit(ExprCall exprCall) throws Err {
        number_of_elements += 1;
        return null;
    }

    @Override
    public Void visit(ExprConstant exprConstant) throws Err {
        return null;
    }

    @Override
    public Void visit(ExprITE exprITE) throws Err {
        number_of_elements += 1;
        this.visitThis(exprITE.cond);
        this.visitThis(exprITE.left);
        this.visitThis(exprITE.right);
        return null;
    }

    @Override
    public Void visit(ExprLet exprLet) throws Err {
        number_of_elements += 1;
        number_of_variebles += 1;
        this.visitThis(exprLet.expr);
        this.visitThis(exprLet.sub);
        return null;
    }

    @Override
    public Void visit(ExprQt exprQt) throws Err {
        number_of_elements += 1;
        number_of_variebles += exprQt.decls.stream().map(x -> x.names).mapToLong(Collection::size).sum();
        this.visitThis(exprQt.sub);
        return null;
    }

    @Override
    public Void visit(ExprUnary exprUnary) throws Err {
        number_of_elements += 1;
        this.visitThis(exprUnary.sub);
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

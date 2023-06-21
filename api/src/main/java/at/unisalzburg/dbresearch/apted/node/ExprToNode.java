package at.unisalzburg.dbresearch.apted.node;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExprToNode extends VisitReturn<Node<Expr>> {

    public static Node<Expr> parse(Expr e) {
        return new ExprToNode().visitThis(e);
    }

    public static Node<Expr> parseOrDefault(Expr e) {
        if (e == null)
            return new Node<>(ExprConstant.TRUE);
        return parse(e);
    }

    @Override
    public Node<Expr> visit(ExprBinary exprBinary) throws Err {
        Node<Expr> result = new Node<>(exprBinary);

        result.addChild(visitThis(exprBinary.left));
        result.addChild(visitThis(exprBinary.right));

        return result;
    }

    @Override
    public Node<Expr> visit(ExprList exprList) throws Err {
        Node<Expr> result = new Node<>(exprList);

        exprList.args.stream().map(this::visitThis).forEach(result::addChild);

        return result;
    }

    @Override
    public Node<Expr> visit(ExprCall exprCall) throws Err {
        return new Node<>(exprCall);
    }

    @Override
    public Node<Expr> visit(ExprConstant exprConstant) throws Err {
        return new Node<>(exprConstant);
    }

    @Override
    public Node<Expr> visit(ExprITE exprITE) throws Err {
        Node<Expr> result = new Node<>(exprITE);

        result.addChild(visitThis(exprITE.left));
        result.addChild(visitThis(exprITE.right));

        return result;
    }

    @Override
    public Node<Expr> visit(ExprLet exprLet) throws Err {
        Node<Expr> result = new Node<>(exprLet);

        result.addChild(visitThis(exprLet.sub));

        return result;
    }

    @Override
    public Node<Expr> visit(ExprQt exprQt) throws Err {

        Node<Expr> bottom = visitThis(exprQt.sub);
        Expr bottomExpr = exprQt.sub;

        List<Decl> rv_decls = new ArrayList<>(exprQt.decls);
        Collections.reverse(rv_decls);

        for (Decl d : rv_decls) {
            bottomExpr = exprQt.op.make(null, null, List.of(d), bottomExpr);
            Node<Expr> next = new Node<>(bottomExpr);

            next.addChild(bottom);
            bottom = next;
        }

        return bottom;
    }

    @Override
    public Node<Expr> visit(ExprUnary exprUnary) throws Err {
        if (ExprUnary.Op.NOOP.equals(exprUnary.op))
            return visitThis(exprUnary.sub);
        Node<Expr> result = new Node<>(exprUnary);
        result.addChild(visitThis(exprUnary.sub));
        return result;
    }

    @Override
    public Node<Expr> visit(ExprVar exprVar) throws Err {
        return new Node<>(exprVar);
    }

    @Override
    public Node<Expr> visit(Sig sig) throws Err {
        return new Node<>(sig);
    }

    @Override
    public Node<Expr> visit(Sig.Field field) throws Err {
        return new Node<>(field);
    }
}

package at.unisalzburg.dbresearch.apted.node;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

public class ExprToNodeExprData extends VisitReturn<Node<ExprData>> {


    Stack<Pos> posStack = new Stack<>();

    public ExprToNodeExprData(Pos init) {
        posStack.push(init);
    }

    public static Node<ExprData> parse(Expr e) {
        return new ExprToNodeExprData(new Pos(e.pos.filename, 1, 1, Integer.MAX_VALUE, Integer.MAX_VALUE)).visitThis(e);
    }

    public static Node<ExprData> parseOrDefault(Expr e) {
        if (e == null)
            return new Node<>(new ExprData(ExprConstant.TRUE, Pos.UNKNOWN));
        return parse(e);
    }

    private Pos getValidPos(Pos previous, Pos next) {
        if (next != null && !next.equals(Pos.UNKNOWN) && previous.contains(next))
            return next;
        else
            return previous;
    }

    private Pos peekValidPos(Pos p) {
        return getValidPos(posStack.peek(), p);
    }

    private Pos pushValidPos(Pos p) {
        Pos pushed = getValidPos(posStack.peek(), p);
        posStack.push(pushed);
        return pushed;
    }

    @Override
    public Node<ExprData> visit(ExprBinary exprBinary) throws Err {
        Node<ExprData> result = new Node<>(new ExprData(exprBinary, pushValidPos(exprBinary.pos)));

        result.addChild(visitThis(exprBinary.left));
        result.addChild(visitThis(exprBinary.right));

        posStack.pop();

        return result;
    }

    @Override
    public Node<ExprData> visit(ExprList exprList) throws Err {
        Node<ExprData> result = new Node<>(new ExprData(exprList, pushValidPos(exprList.pos)));

        exprList.args.stream().map(this::visitThis).forEach(result::addChild);

        posStack.pop();

        return result;
    }

    @Override
    public Node<ExprData> visit(ExprCall exprCall) throws Err {
        return new Node<>(new ExprData(exprCall, peekValidPos(exprCall.pos)));
    }

    @Override
    public Node<ExprData> visit(ExprConstant exprConstant) throws Err {
        return new Node<>(new ExprData(exprConstant, peekValidPos(exprConstant.pos)));
    }

    @Override
    public Node<ExprData> visit(ExprITE exprITE) throws Err {
        Node<ExprData> result = new Node<>(new ExprData(exprITE, pushValidPos(exprITE.pos)));

        result.addChild(visitThis(exprITE.left));
        result.addChild(visitThis(exprITE.right));
        posStack.pop();

        return result;
    }

    @Override
    public Node<ExprData> visit(ExprLet exprLet) throws Err {
        Node<ExprData> result = new Node<>(new ExprData(exprLet, pushValidPos(exprLet.pos)));

        result.addChild(visitThis(exprLet.sub));
        posStack.pop();

        return result;
    }

    @Override
    public Node<ExprData> visit(ExprQt exprQt) throws Err {
        Pos topValidPos = pushValidPos(exprQt.pos);

        Node<ExprData> bottom = visitThis(exprQt.sub);
        Expr bottomExpr = exprQt.sub;

        List<Decl> rv_decls = new ArrayList<>(exprQt.decls);
        Collections.reverse(rv_decls);

        for (Decl d : rv_decls) {
            Pos clusterPos = Stream.concat(d.names.stream().map(Expr::pos), Stream.of(d.expr.pos(), d.span())).map(this::peekValidPos).reduce(Pos::merge).orElse(topValidPos);
            bottomExpr = exprQt.op.make(null, null, List.of(d), bottomExpr);
            Node<ExprData> next = new Node<>(new ExprData(bottomExpr, peekValidPos(clusterPos)));

            next.addChild(bottom);
            bottom = next;
        }

        posStack.pop();
        return bottom;
    }

    @Override
    public Node<ExprData> visit(ExprUnary exprUnary) throws Err {
        try {
            pushValidPos(exprUnary.pos());

            if (ExprUnary.Op.NOOP.equals(exprUnary.op))
                return visitThis(exprUnary.sub);

            Node<ExprData> result = new Node<>(new ExprData(exprUnary, posStack.peek()));
            result.addChild(visitThis(exprUnary.sub));
            return result;
        } finally {
            posStack.pop();
        }
    }

    @Override
    public Node<ExprData> visit(ExprVar exprVar) throws Err {
        return new Node<>(new ExprData(exprVar, peekValidPos(exprVar.pos())));
    }

    @Override
    public Node<ExprData> visit(Sig sig) throws Err {
        return new Node<>(new ExprData(sig, peekValidPos(sig.pos())));
    }

    @Override
    public Node<ExprData> visit(Sig.Field field) throws Err {
        return new Node<>(new ExprData(field, peekValidPos(field.pos())));
    }
}

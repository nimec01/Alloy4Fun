package at.unisalzburg.dbresearch.apted.costmodel;

import at.unisalzburg.dbresearch.apted.node.Node;
import edu.mit.csail.sdg.ast.Expr;

public class ExprShallowCostModel implements CostModel<Expr> {

    @Override
    public float del(Node<Expr> n) {
        return 1.0f;
    }

    @Override
    public float ins(Node<Expr> n) {
        return 1.0f;
    }

    @Override
    public float ren(Node<Expr> n1, Node<Expr> n2) {
        return ShallowEquals.equals(n1.getNodeData(), n2.getNodeData()) ? 0.0f : 1.0f;
    }

    @Override
    public boolean eq(Node<Expr> n1, Node<Expr> n2) {
        return ShallowEquals.equals(n1.getNodeData(), n2.getNodeData());
    }

}
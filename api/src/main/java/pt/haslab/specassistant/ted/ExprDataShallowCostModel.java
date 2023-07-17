package pt.haslab.specassistant.ted;

import at.unisalzburg.dbresearch.apted.costmodel.CostModel;
import at.unisalzburg.dbresearch.apted.node.Node;

public class ExprDataShallowCostModel implements CostModel<ExprData> {

    @Override
    public float del(Node<ExprData> n) {
        return 1.0f;
    }

    @Override
    public float ins(Node<ExprData> n) {
        return 1.0f;
    }

    @Override
    public float ren(Node<ExprData> n1, Node<ExprData> n2) {
        return ShallowEquals.equals(n1.getNodeData().expr(), n2.getNodeData().expr()) ? 0.0f : 1.0f;
    }

    @Override
    public boolean eq(Node<ExprData> n1, Node<ExprData> n2) {
        return ShallowEquals.equals(n1.getNodeData().expr(), n2.getNodeData().expr());
    }
}


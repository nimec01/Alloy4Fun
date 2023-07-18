package pt.haslab.specassistant.edittree;

import at.unisalzburg.dbresearch.apted.costmodel.CostModel;
import at.unisalzburg.dbresearch.apted.node.Node;
import pt.haslab.alloyaddons.ExprNodeEquals;

public class EditDataCostModel implements CostModel<EditData> {

    @Override
    public float del(Node<EditData> n) {
        return 1.0f;
    }

    @Override
    public float ins(Node<EditData> n) {
        return 1.0f;
    }

    @Override
    public float ren(Node<EditData> n1, Node<EditData> n2) {
        return ExprNodeEquals.equals(n1.getNodeData().expr(), n2.getNodeData().expr()) ? 0.0f : 1.0f;
    }

    @Override
    public boolean eq(Node<EditData> n1, Node<EditData> n2) {
        return ExprNodeEquals.equals(n1.getNodeData().expr(), n2.getNodeData().expr());
    }
}


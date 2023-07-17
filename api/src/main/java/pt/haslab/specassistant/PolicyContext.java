package pt.haslab.specassistant;

import org.bson.types.ObjectId;
import pt.haslab.specassistant.models.HintEdge;
import pt.haslab.specassistant.models.HintNode;

import java.util.function.Function;

public class PolicyContext implements Comparable<PolicyContext> {
    public HintNode node;

    public Double cost;
    public int distance;

    public static PolicyContext init(HintNode n) {
        PolicyContext result = new PolicyContext();
        result.node = n;
        result.cost = 0.0;
        result.distance = 0;
        return result;
    }

    public PolicyContext bestScored(PolicyContext policyContext) {
        if (this.cost < policyContext.cost) return this;
        return policyContext;
    }

    public PolicyContext scoreEdgeOrigin(HintEdge edge, Function<ObjectId, HintNode> nodeGetter) {
        PolicyContext result = new PolicyContext();

        result.node = nodeGetter.apply(edge.origin);

        double prob;
        //origin_node.leaves == 0 means that every edge of the node is an addition by an algorithm (ex: a mutation)
        //In this case, the probability of it being traversed will be 0
        if (result.node.leaves != 0)
            prob = (double) edge.count / (double) result.node.leaves;
        else
            prob = 0.0;

        edge.hopDistance = result.distance = distance + 1;

        edge.score = result.cost = edge.computeImminentCost() + prob * this.cost;

        edge.update();

        return result;
    }

    @Override
    public int compareTo(PolicyContext o) {
        return this.cost.compareTo(o.cost);
    }

    @Override
    public String toString() {
        return "{cost=%s, distance=%d}".formatted(cost, distance);
    }

    public ObjectId nodeId() {
        return node.id;
    }

    public void assignScore() {
        this.node.score = this.cost;
        this.node.persistOrUpdate();
    }
}

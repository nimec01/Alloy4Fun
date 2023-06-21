package pt.haslab.alloy4fun.data.transfer;

import org.bson.types.ObjectId;
import pt.haslab.alloy4fun.data.models.HintGraph.HintEdge;
import pt.haslab.alloy4fun.data.models.HintGraph.HintNode;
import pt.haslab.alloy4fun.services.HintService;

import java.util.function.Function;

public class ScoreTraversalContext implements Comparable<ScoreTraversalContext> {
    public HintNode node;

    public Double cost;
    public int distance;

    public static ScoreTraversalContext init(HintNode n) {
        ScoreTraversalContext result = new ScoreTraversalContext();
        result.node = n;
        result.cost = 0.0;
        result.distance = 0;
        return result;
    }

    public ScoreTraversalContext bestScored(ScoreTraversalContext scoreTraversalContext) {
        if (this.cost.compareTo(scoreTraversalContext.cost) < 0) return this;
        return scoreTraversalContext;
    }

    public ScoreTraversalContext scoreEdgeOrigin(HintEdge edge, Function<ObjectId, HintNode> nodeGetter) {
        ScoreTraversalContext result = new ScoreTraversalContext();

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
    public int compareTo(ScoreTraversalContext o) {
        return this.cost.compareTo(o.cost);
    }

    @Override
    public String toString() {
        return "{cost=%s, distance=%d}".formatted(cost, distance);
    }

    public ObjectId nodeId() {
        return node.id;
    }
}

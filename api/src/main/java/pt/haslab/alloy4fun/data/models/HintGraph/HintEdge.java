package pt.haslab.alloy4fun.data.models.HintGraph;


import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import pt.haslab.alloy4fun.util.AlloyExprDifference;
import pt.haslab.alloy4fun.util.AlloyExprDifference.IndexRangeDifference;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@MongoEntity(collection = "HintEdge")
public class HintEdge extends PanacheMongoEntity {
    public Long graph_id;

    public ObjectId origin, destination;


    public Map<String, List<IndexRangeDifference>> differenceRange;

    public int editDistance;

    public int count;

    public Double score;


    public static HintEdge createEmpty(Long graph_id, ObjectId originNodeId, ObjectId destinationNodeId) {
        HintEdge result = new HintEdge();
        result.graph_id = graph_id;
        result.origin = originNodeId;
        result.destination = destinationNodeId;

        result.count = 0;

        return result;
    }

    public HintEdge computeDifferences(CompModule world, Function<ObjectId, HintNode> nodeGetter) {
        Map<String, Expr> originParsed = nodeGetter.apply(origin).getParsedFormula(world);
        Map<String, Expr> peerParsed = nodeGetter.apply(destination).getParsedFormula(world);

        Map<String, AlloyExprDifference> diffs = Stream.of(originParsed.keySet(), peerParsed.keySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .stream()
                .collect(toMap(key -> key, key -> AlloyExprDifference.create(originParsed.getOrDefault(key, ExprConstant.TRUE), peerParsed.getOrDefault(key, ExprConstant.TRUE))));

        diffs.values().forEach(AlloyExprDifference::compute);

        differenceRange = diffs.entrySet().stream().collect(toMap(Map.Entry::getKey, x -> x.getValue().getIndexDifferences()));

        editDistance = differenceRange.values().stream().map(AlloyExprDifference::getEditDifference).reduce(0, Integer::sum);
        return this;
    }

    public HintEdge visit() {
        count++;
        return this;
    }

    public ObjectId oppositeId(ObjectId id) {
        if (origin.equals(id))
            return destination;
        return origin;
    }
}

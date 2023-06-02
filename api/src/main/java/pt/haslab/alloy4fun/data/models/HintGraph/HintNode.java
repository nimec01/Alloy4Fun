package pt.haslab.alloy4fun.data.models.HintGraph;


import edu.mit.csail.sdg.ast.Func;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import pt.haslab.alloy4fun.util.AlloyExprNormalizer;
import pt.haslab.alloy4fun.util.AlloyExprStringify;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableMap;

@MongoEntity(collection = "HintNode")
public class HintNode extends PanacheMongoEntity {

    public Long graph_id;
    public Map<String, String> formula;

    public Boolean valid;

    public Integer visits;

    public Integer leaves;


    public HintNode() {
    }

    public static HintNode createPersistent(Long graph_id, Map<String, String> formula, Boolean sat) {
        HintNode result = new HintNode();

        result.graph_id = graph_id;
        result.formula = formula;
        result.valid = sat;
        result.visits = result.leaves = 0;

        result.persist();

        return result;
    }

    public HintNode registerEntry() {
        visits++;
        return this;
    }

    public static Map<String, String> getFormulaFrom(Collection<Func> skolem, Set<String> functions) {
        return skolem.stream()
                .filter(x -> functions.contains(x.label.replace("this/", "")))
                .collect(toUnmodifiableMap(x -> x.label, x -> AlloyExprStringify.stringify( AlloyExprNormalizer.normalize(x))));
    }


}

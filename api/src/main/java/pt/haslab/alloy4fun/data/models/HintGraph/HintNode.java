package pt.haslab.alloy4fun.data.models.HintGraph;


import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import pt.haslab.alloy4fun.util.AlloyExprNormalizer;
import pt.haslab.alloy4fun.util.AlloyExprStringify;
import pt.haslab.alloy4fun.util.AlloyUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

@MongoEntity(collection = "HintNode")
public class HintNode extends PanacheMongoEntity {

    public Long graph_id;
    public Map<String, String> formula;

    public String witness;

    public Boolean valid;

    public Integer visits;

    public Integer leaves;


    public HintNode() {
    }

    public static HintNode create(Long graph_id, Map<String, String> formula, Boolean sat, String witness) {
        HintNode result = new HintNode();

        result.graph_id = graph_id;
        result.formula = formula;
        result.valid = sat;
        result.witness = witness;
        result.visits = result.leaves = 0;

        return result;
    }

    public static Map<String, Expr> getNormalizedFormulaExprFrom(Collection<Func> skolem, Set<String> functions) {
        return AlloyUtil.streamFuncsWithNames(skolem, functions)
                .collect(toUnmodifiableMap(x -> x.label, AlloyExprNormalizer::normalize));
    }

    public static Map<String, Expr> getFormulaExprFrom(Collection<Func> skolem, Set<String> functions) {
        return AlloyUtil.streamFuncsWithNames(skolem, functions).collect(toUnmodifiableMap(x -> x.label, Func::getBody));
    }

    public static Map<String, String> getNormalizedFormulaFrom(Collection<Func> funcs, Set<String> targetNames) {
        return AlloyUtil.streamFuncsWithNames(funcs, targetNames)
                .collect(toUnmodifiableMap(x -> x.label, x -> AlloyExprStringify.stringify(AlloyExprNormalizer.normalize(x))));
    }

    public Map<String, Expr> getParsedFormula(CompModule world) throws RuntimeException {
        return formula.entrySet().stream().collect(toMap(Map.Entry::getKey, x -> AlloyUtil.parseOneExprFromString(world, x.getValue())));
    }

    public HintNode visit() {
        visits++;
        return this;
    }

    public boolean compareFormula(Map<String, String> x) {
        return this.formula.equals(x);
    }

}

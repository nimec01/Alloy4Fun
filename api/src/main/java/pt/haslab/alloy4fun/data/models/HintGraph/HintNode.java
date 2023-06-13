package pt.haslab.alloy4fun.data.models.HintGraph;


import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import pt.haslab.alloy4fun.util.AlloyExprNormalizer;
import pt.haslab.alloy4fun.util.AlloyExprStringify;
import pt.haslab.alloy4fun.util.AlloyUtil;
import pt.haslab.alloy4fun.util.Catch;

import java.util.*;

import static java.util.stream.Collectors.toMap;
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

    public static HintNode create(Long graph_id, Map<String, String> formula, Boolean sat) {
        HintNode result = new HintNode();

        result.graph_id = graph_id;
        result.formula = formula;
        result.valid = sat;
        result.visits = result.leaves = 0;

        return result;
    }

    public static List<Map<String, String>> findAllMutations(final CompModule world, final Map<String, String> formula) {
        Map<String, String> unchanged = new HashMap<>();
        List<Map.Entry<String, List<String>>> changed = new ArrayList<>();

        formula.forEach((target, expr_str) -> {
            List<String> mutations = AlloyUtil.parseAndMutate(world, expr_str, 1).stream()
                    .map(AlloyExprStringify::stringifyAndDiscardTrue)
                    .filter(x -> !Objects.equals(expr_str, x))
                    .toList();
            if (mutations.isEmpty())
                unchanged.put(target, expr_str);
            else
                changed.add(Map.entry(target, mutations));
        });

        if (changed.isEmpty())
            return List.of(unchanged);

        int[] counts = new int[changed.size()];
        int[] factorization = new int[changed.size()];
        factorization[0] = 1;
        counts[0] = changed.get(0).getValue().size();
        for (int i = 1; i < changed.size(); i++) {
            counts[i] = changed.get(i).getValue().size();
            factorization[i] = factorization[i - 1] * counts[i - 1];
        }
        int arrangement_count = Arrays.stream(counts).reduce(1, (x, y) -> x * y);

        List<Map<String, String>> result = new ArrayList<>();

        for (int i = 0; i < arrangement_count; i++) {
            Map<String, String> current = new HashMap<>(unchanged);
            for (int j = 0; j < changed.size(); j++) {
                Map.Entry<String, List<String>> entry = changed.get(j);
                current.put(entry.getKey(), entry.getValue().get((i / factorization[j]) % counts[j]));
            }
            result.add(current);
        }
        return result;
    }

    public static Map<String, Expr> getNormalizedFormulaExprFrom(Collection<Func> skolem, Set<String> functions) {
        return skolem.stream().filter(x -> functions.contains(x.label.replace("this/", "")))
                .collect(toUnmodifiableMap(x -> x.label, x -> AlloyExprNormalizer.normalize(x.getBody())));
    }

    public static Map<String, String> getNormalizedFormulaFrom(Collection<Func> skolem, Set<String> functions) {
        return skolem.stream().filter(x -> functions.contains(x.label.replace("this/", "")))
                .collect(toUnmodifiableMap(x -> x.label, x -> AlloyExprStringify.stringifyAndDiscardTrue(AlloyExprNormalizer.normalize(x.getBody()))));
    }

    public Map<String, Expr> getParsedFormula(CompModule world) throws RuntimeException {
        return formula.entrySet().stream().collect(toMap(Map.Entry::getKey, x -> Catch.atRuntime(() -> world.parseOneExpressionFromString(x.getValue()))));
    }

    public HintNode visit() {
        visits++;
        return this;
    }

    public boolean compareFormula(Map<String, String> x) {
        return this.formula.equals(x);
    }

}

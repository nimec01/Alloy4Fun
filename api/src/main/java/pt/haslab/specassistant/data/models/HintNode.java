package pt.haslab.specassistant.data.models;


import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import pt.haslab.alloyaddons.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

@MongoEntity(collection = "HintNode")
public class HintNode extends PanacheMongoEntity {

    public ObjectId graph_id;
    public Map<String, String> formula;

    public String witness;

    public Boolean valid;

    public Integer visits;

    public Integer leaves;

    public Integer hopDistance;

    public Integer complexity;

    public Double score;

    public String debug;

    public HintNode() {
    }

    public static HintNode create(ObjectId graph_id, Map<String, String> formula, Boolean sat, String witness) {
        HintNode result = new HintNode();

        result.graph_id = graph_id;
        result.formula = formula;
        result.valid = sat;
        result.witness = witness;
        result.visits = result.leaves = 0;

        return result;
    }

    public Map<String, String> getFormula() {
        return formula;
    }

    public static Map<String, Expr> getNormalizedFormulaExprFrom(CompModule world, Set<String> functions) {
        return getNormalizedFormulaExprFrom(world.getAllFunc().makeConstList(), functions);
    }

    public static Map<String, Expr> getNormalizedFormulaExprFrom(Collection<Func> skolem, Set<String> functions) {
        return AlloyUtil.streamFuncsWithNames(skolem, functions)
                .collect(toUnmodifiableMap(x -> x.label, ExprNormalizer::normalize));
    }

    public static Map<String, String> formulaExprToString(Map<String, Expr> formulaExpr) {
        return formulaExpr.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> ExprStringify.stringify(x.getValue())));
    }

    public static Map<String, Expr> getFormulaExprFrom(Collection<Func> skolem, Set<String> functions) {
        return AlloyUtil.streamFuncsWithNames(skolem, functions).collect(toUnmodifiableMap(x -> x.label, Func::getBody));
    }

    public static Map<String, String> getNormalizedFormulaFrom(Collection<Func> funcs, Set<String> targetNames) {
        return AlloyUtil.streamFuncsWithNames(funcs, targetNames)
                .collect(toUnmodifiableMap(x -> x.label, x -> ExprStringify.stringify(ExprNormalizer.normalize(x))));
    }

    public Map<String, Expr> getParsedFormula(CompModule world) throws IllegalStateException {
        try {
            CompModule target_world = Optional.ofNullable(this.witness).map(Model::getWorld).orElse(world);
            return formula.entrySet().stream().collect(toMap(Map.Entry::getKey, x -> ParseUtil.parseOneExprFromString(target_world, x.getValue())));
        } catch (ErrorSyntax e) {
            throw new IllegalStateException("Syntax Error While Parsing Formula:\"" + this.getFormula().toString().replace("\n", "") + "\" " + e.pos.toString() + " " + e.getMessage(), e);
        } catch (Err e) {
            throw new IllegalStateException("Alloy Error While Parsing Formula:\"" + this.getFormula().toString().replace("\n", "") + "\" " + e.pos.toString() + " " + e.getMessage(), e);
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("IO Error While Parsing Formula:\"" + this.getFormula().toString().replace("\n", "") + "\" " + e.getMessage(), e);
        }
    }

    public HintNode visit() {
        visits++;
        return this;
    }

}

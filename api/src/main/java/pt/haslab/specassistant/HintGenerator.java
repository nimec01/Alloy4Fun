package pt.haslab.specassistant;

import at.unisalzburg.dbresearch.apted.costmodel.CostModel;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import pt.haslab.Repairer;
import pt.haslab.alloyaddons.ExprNormalizer;
import pt.haslab.alloyaddons.ExprStringify;
import pt.haslab.alloyaddons.Util;
import pt.haslab.mutation.Candidate;
import pt.haslab.mutation.mutator.Mutator;
import pt.haslab.specassistant.data.models.*;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.edittree.ASTEditDiff;
import pt.haslab.specassistant.edittree.EditData;
import pt.haslab.specassistant.edittree.ExprToEditData;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.util.Static;
import pt.haslab.specassistant.util.Text;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static pt.haslab.specassistant.util.Static.getCombinations;

@ApplicationScoped
public class HintGenerator {

    @Inject
    ModelRepository modelRepo;
    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;
    @Inject
    HintExerciseRepository exerciseRepo;

    public static <ID> List<Map<ID, Candidate>> makeCandidateMaps(Map<ID, Expr> targets, ConstList<Sig> sigs, int maxDepth) {
        Map<ID, Candidate> unchanged = new HashMap<>();
        List<Map.Entry<ID, List<Candidate>>> changed = new ArrayList<>();

        targets.forEach((target, expr) -> {
            List<Candidate> mutations = Repairer.getValidCandidates(expr, sigs, maxDepth);

            if (mutations.isEmpty()) {
                Candidate c = Candidate.empty();
                c.mutated = expr;
                unchanged.put(target, c);
            } else
                changed.add(Map.entry(target, mutations));
        });
        if (changed.isEmpty())
            return List.of(unchanged);

        return getCombinations(unchanged, changed);
    }

    private String getOriginalId(String model_id) {
        Model m = modelRepo.findById(model_id);
        if (m.isOriginal_())
            return m.id;
        return m.original;
    }

    public Optional<HintMsg> getHint(String originId, String command_label, CompModule world) {
        String original_id = getOriginalId(originId);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original_id, command_label).orElse(null);
        if (exercise == null)
            return Optional.empty();

        ObjectId graph_id = exercise.graph_id;

        Optional<HintMsg> result = hintWithMutation(graph_id, world.getAllFunc().makeConstList(), world.getAllReachableSigs(), exercise);

        if (result.isPresent())
            return result;

        return hintWithGraph(world, exercise, graph_id);
    }

    private Optional<HintMsg> hintWithGraph(CompModule world, HintExercise exercise, ObjectId graph_id) {
        Map<String, Expr> formulaExpr = HintNode.getNormalizedFormulaExprFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

        Map<String, String> formula = formulaExpr.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> ExprStringify.stringify(x.getValue())));

        Optional<HintNode> node_opt = nodeRepo.findByGraphIdAndFormula(graph_id, formula);

        if (node_opt.isPresent()) {
            HintNode origin_node = node_opt.orElseThrow();
            Optional<HintEdge> edge_opt = edgeRepo.findBestScoredByOriginNode(origin_node.id);
            if (edge_opt.isPresent()) {
                HintEdge edge = edge_opt.orElseThrow();
                node_opt = nodeRepo.findByIdOptional(edge.destination);
                if (node_opt.isPresent()) {
                    HintNode dest_node = node_opt.orElseThrow();

                    Map<String, Expr> otherFormulaExpr = dest_node.getParsedFormula(Optional.ofNullable(dest_node.witness).map(Model::getWorld).orElse(world));
                    for (String s : formula.keySet()) {
                        ASTEditDiff apted = new ASTEditDiff();

                        apted.computeEditDistance(ExprToEditData.parseOrDefault(formulaExpr.get(s)), ExprToEditData.parseOrDefault(otherFormulaExpr.get(s)));

                        Map.Entry<CostModel.OpType, EditData> change = apted.mappingFirstChange(apted.computeEditMapping());

                        if (change != null) {
                            return switch (change.getKey()) {
                                case Rename, Delete ->
                                        Optional.of(HintMsg.from(change.getValue().position(), "Try to change this declaration"));
                                case Insertion ->
                                        Optional.of(HintMsg.from(change.getValue().position(), "Try to add something to this declaration"));
                            };
                        }

                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<HintMsg> hintWithMutation(ObjectId graph_id, Collection<Func> skolem, ConstList<Sig> sigs, HintExercise exercise) {
        List<Map<String, Candidate>> candidateFormulas = makeCandidateMaps(HintNode.getFormulaExprFrom(skolem, exercise.targetFunctions), sigs, 1);

        List<Map<String, String>> mutatedFormulas = candidateFormulas.stream().map(m -> Static.mapValues(m, f -> ExprStringify.stringify(ExprNormalizer.normalize(f.mutated)))).toList();

        Optional<HintNode> e = nodeRepo.findBestByGraphIdAndFormulaIn(graph_id, mutatedFormulas);

        if (e.isPresent()) {
            HintNode n = e.orElseThrow();
            int target = mutatedFormulas.indexOf(n.formula);

            for (Candidate c : candidateFormulas.get(target).values()) {
                if (!c.mutators.isEmpty()) {
                    for (Mutator m : c.mutators) {
                        if (m.hint().isPresent())
                            return Optional.of(HintMsg.from(m.original.expr.pos(), m.hint().orElseThrow()));
                    }
                }
            }
        }

        return Optional.empty();
    }

    public Boolean testAllHints(String original_id, String command_label, CompModule world) {
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original_id, command_label).orElse(null);
        if (exercise == null)
            return false;

        ObjectId graph_id = exercise.graph_id;

        Optional<HintMsg> a = hintWithMutation(graph_id, world.getAllFunc().makeConstList(), world.getAllReachableSigs(), exercise);
        Optional<HintMsg> b = hintWithGraph(world, exercise, graph_id);

        HintGraph.registerMultipleHintAttempt(graph_id, a.isPresent(), b.isPresent());

        return a.isPresent() || b.isPresent();
    }

    public void testAllHintsOfModel(String modelId, Predicate<LocalDateTime> year_tester) {
        try {
            CompletableFuture.allOf(modelRepo.streamByOriginalAndUnSat(modelId)
                    .filter(x -> year_tester.test(Text.parseDate(x.time)))
                    .map(x -> CompletableFuture.runAsync(() -> testAllHints(x.original, x.cmd_n, Util.parseModel(x.code))))
                    .toArray(CompletableFuture[]::new)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}

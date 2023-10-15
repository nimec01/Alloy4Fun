package pt.haslab.specassistant.services;

import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import pt.haslab.Repairer;
import pt.haslab.alloyaddons.AlloyUtil;
import pt.haslab.alloyaddons.ExprNormalizer;
import pt.haslab.alloyaddons.ExprStringify;
import pt.haslab.mutation.Candidate;
import pt.haslab.mutation.mutator.Mutator;
import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintExercise;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.data.transfer.HintMsg;
import pt.haslab.specassistant.data.transfer.Transition;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.services.treeedit.ASTEditDiff;
import pt.haslab.specassistant.util.DataUtil;

import java.util.*;
import java.util.stream.Collectors;

import static pt.haslab.specassistant.data.models.HintNode.formulaExprToString;
import static pt.haslab.specassistant.util.DataUtil.getCombinations;

@ApplicationScoped
public class HintGenerator {

    @Inject
    Logger log;

    @ConfigProperty(name = "hint.mutations", defaultValue = "true")
    boolean mutationsEnabled;

    @Inject
    ModelRepository modelRepo;
    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;
    @Inject
    HintExerciseRepository exerciseRepo;

    public static <ID> List<Map<ID, Candidate>> mkAllMutatedFormula(Map<ID, Expr> formula, ConstList<Sig> sigs, int maxDepth) {
        Map<ID, Candidate> unchanged = new HashMap<>();
        List<Map.Entry<ID, List<Candidate>>> changed = new ArrayList<>();

        formula.forEach((target, expr) -> {
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

    public static HintMsg firstHint(Map<String, Expr> formulaExpr, Map<String, Expr> otherFormulaExpr) {
        for (String s : formulaExpr.keySet()) {
            ASTEditDiff diff = new ASTEditDiff().initFrom(formulaExpr.get(s), otherFormulaExpr.get(s));
            diff.computeEditDistance();
            return diff.getFirstEditOperation().getHintMessage();
        }
        return null;
    }

    public Optional<Transition> formulaTransition(ObjectId graph_id, Map<String, String> formula) {
        Optional<HintNode> node_opt = nodeRepo.findByGraphIdAndFormula(graph_id, formula);

        if (node_opt.isPresent()) {
            HintNode origin_node = node_opt.orElseThrow();
            Optional<HintEdge> edge_opt = edgeRepo.policyByOriginNode(origin_node.id);
            if (edge_opt.isPresent()) {
                HintEdge edge = edge_opt.orElseThrow();
                HintNode destination = nodeRepo.findByIdOptional(edge.destination).orElseThrow();
                return Optional.of(new Transition(edge, origin_node, destination));
            }
        }
        return Optional.empty();
    }

    public Optional<Transition> worldTransition(HintExercise exercise, CompModule world) {
        Map<String, Expr> formulaExpr = HintNode.getNormalizedFormulaExprFrom(world, exercise.targetFunctions);
        Map<String, String> formula = formulaExprToString(formulaExpr);

        return formulaTransition(exercise.graph_id, formula);
    }

    public Optional<HintMsg> hintWithGraph(HintExercise exercise, CompModule world) {
        Map<String, Expr> formulaExpr = HintNode.getNormalizedFormulaExprFrom(world, exercise.targetFunctions);
        Map<String, String> formula = formulaExprToString(formulaExpr);

        return formulaTransition(exercise.graph_id, formula).map(x -> firstHint(formulaExpr, x.to.getParsedFormula(world)));
    }


    public Optional<HintNode> mutatedNextState(HintExercise exercise, CompModule world) {
        List<Map<String, Candidate>> candidateFormulas = mkAllMutatedFormula(HintNode.getFormulaExprFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions), world.getAllReachableSigs(), 1);

        List<Map<String, String>> mutatedFormulas = candidateFormulas.stream().map(m -> DataUtil.mapValues(m, f -> ExprStringify.stringify(ExprNormalizer.normalize(f.mutated)))).toList();

        return nodeRepo.findBestByGraphIdAndFormulaIn(exercise.graph_id, mutatedFormulas);
    }

    public Optional<HintMsg> hintWithMutation(HintExercise exercise, CompModule world) {
        List<Map<String, Candidate>> candidateFormulas = mkAllMutatedFormula(HintNode.getFormulaExprFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions), world.getAllReachableSigs(), 1);

        List<Map<String, String>> mutatedFormulas = candidateFormulas.stream().map(m -> DataUtil.mapValues(m, f -> ExprStringify.stringify(ExprNormalizer.normalize(f.mutated)))).toList();

        Optional<HintNode> e = nodeRepo.findBestByGraphIdAndFormulaIn(exercise.graph_id, mutatedFormulas);

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

    public Optional<HintMsg> getHint(String originId, String command_label, CompModule world) {
        String original_id = modelRepo.getOriginalById(originId);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original_id, command_label).orElse(null);

        if (exercise == null) {
            log.debug("No exercise found for original=" + original_id + " && command_label=" + command_label);
            return Optional.empty();
        }

        Set<String> availableFuncs = AlloyUtil.streamFuncsNamesWithNames(world.getAllFunc().makeConstList(), exercise.targetFunctions).collect(Collectors.toSet());
        if (!availableFuncs.containsAll(exercise.targetFunctions)) {
            log.debug("Some of the targeted functions are not contained within provided world, missing " + new HashSet<>(exercise.targetFunctions).removeAll(availableFuncs));
            return Optional.empty();
        }

        Optional<HintMsg> result = hintWithGraph(exercise, world);

        if (result.isEmpty() && mutationsEnabled) {
            result = hintWithMutation(exercise, world);
        }

        return result;
    }
}

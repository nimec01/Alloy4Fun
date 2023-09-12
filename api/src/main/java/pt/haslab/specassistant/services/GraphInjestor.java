package pt.haslab.specassistant.services;

import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloyaddons.ParseUtil;
import pt.haslab.alloyaddons.UncheckedIOException;
import pt.haslab.specassistant.data.models.*;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.services.treeedit.ASTEditDiff;
import pt.haslab.specassistant.util.FutureUtil;
import pt.haslab.specassistant.util.Text;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@ApplicationScoped
public class GraphInjestor {

    private static final Logger LOG = Logger.getLogger(GraphInjestor.class);

    @Inject
    ModelRepository modelRepo;
    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;
    @Inject
    HintExerciseRepository exerciseRepo;


    /**
     * The abstract base BFS algorithm
     *
     * @param step        The action applied to every node according to its current context,
     *                    Returns the next context
     * @param modelGetter Function that gets thes subnodes of a model node
     * @param ctx         Current/Initial context
     * @param current     Current/Initial node
     * @param <C>         Context Class
     * @return CompletableFuture of the traversal job
     */
    private static <C> CompletableFuture<Void> walkModelTree(BiFunction<Model, C, C> step, Function<Model, Stream<Model>> modelGetter, C ctx, Model current) {
        return CompletableFuture.supplyAsync(() -> step.apply(current, ctx)).thenCompose(nextCtx -> FutureUtil.allFutures((modelGetter.apply(current).map(child -> walkModelTree(step, modelGetter, nextCtx, child)))));
    }


    public HintEdge parseDifferences(HintEdge edge, CompModule world, Function<ObjectId, HintNode> nodeGetter) {
        try {
            Map<String, Expr> peerParsed = nodeGetter.apply(edge.destination).getParsedFormula(world);
            Map<String, Expr> originParsed = nodeGetter.apply(edge.origin).getParsedFormula(world);

            edge.editDistance = ASTEditDiff.getFormulaDistanceDiff(originParsed, peerParsed);
        } catch (IllegalStateException e) {
            LOG.warn(e.getMessage());
        }
        return edge;
    }

    public CompletableFuture<Void> classifyAllEdges(CompModule world, ObjectId graph_id) {
        return FutureUtil.forEachAsync(edgeRepo.streamByGraphId(graph_id), e -> parseDifferences(e, world, nodeRepo::findById).update());
    }

    public CompletableFuture<Void> walkModelTree(Model root) {
        return walkModelTree(root, null);
    }

    public CompletableFuture<Void> walkModelTree(Model root, Predicate<LocalDateTime> dateFilter) {
        Map<String, HintExercise> cmdToExercise = exerciseRepo.streamByModelId(root.id).collect(Collectors.toUnmodifiableMap(x -> x.cmd_n, x -> x));

        if (cmdToExercise.isEmpty())
            return CompletableFuture.completedFuture(null);

        Map<ObjectId, ObjectId> initCtx = new HashMap<>();
        CompModule world = ParseUtil.parseModel(root.code);

        Set<String> modules = world.getAllReachableModules().makeConstList().stream().map(CompModule::path).collect(Collectors.toSet());

        cmdToExercise.values().forEach(exercise -> {
            Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
            initCtx.put(exercise.id, nodeRepo.incrementOrCreate(formula, false, exercise.graph_id, null).id);
        });
        Function<Model, Stream<Model>> modelGetter;
        if (dateFilter == null)
            modelGetter = model -> modelRepo.streamByDerivationOfAndOriginal(model.id, model.original);
        else {
            modelGetter = model -> modelRepo.streamByDerivationOfAndOriginal(model.id, model.original).filter(x -> dateFilter.test(Text.parseDate(x.time)));
        }
        return walkModelTree((m, ctx) -> walkModelTreeStep(cmdToExercise, modules, m, ctx), modelGetter, initCtx, root);
    }

    private Map<ObjectId, ObjectId> walkModelTreeStep(Map<String, HintExercise> cmdToExercise, Set<String> root_modules, Model current, Map<ObjectId, ObjectId> context) {
        try {
            if (current.isValidExecution() && cmdToExercise.containsKey(current.cmd_n)) {

                CompModule world = ParseUtil.parseModel(current.code);
                HintExercise exercise = cmdToExercise.get(current.cmd_n);
                ObjectId contextId = exercise.id;
                ObjectId old_node_id = context.get(contextId);

                boolean modified = world.getAllReachableModules().makeConstList().stream().map(CompModule::path).anyMatch(x -> !root_modules.contains(x));

                if (exercise.isValidCommand(world, current.cmd_i)) {
                    boolean valid = current.sat == 0;

                    Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

                    ObjectId new_node_id = nodeRepo.incrementOrCreate(formula, valid, exercise.graph_id, modified ? current.id : null).id;

                    if (!old_node_id.equals(new_node_id)) { // No laces
                        nodeRepo.incrementLeaveById(old_node_id);
                        context = new HashMap<>(context); // Make a new context based on the previous one
                        context.put(contextId, new_node_id);
                        edgeRepo.incrementOrCreate(exercise.graph_id, old_node_id, new_node_id);
                    }
                }
            }
        } catch (UncheckedIOException e) {
            LOG.warn(e);
        }
        return context;
    }

    public CompletableFuture<Void> parseModelTree(String model_id, Predicate<LocalDateTime> year_tester) {
        Model model = modelRepo.findById(model_id);

        CompModule base_world = ParseUtil.parseModel(model.code);

        CompletableFuture<Long> job;

        long start = System.nanoTime();

        if (year_tester == null) job = walkModelTree(model).thenApplyAsync(nil -> System.nanoTime() - start);
        else job = walkModelTree(model, year_tester).thenApplyAsync(nil -> System.nanoTime() - start);

        return job.thenComposeAsync(commonTime -> {
            List<CompletableFuture<Map.Entry<ObjectId, Long>>> classifications = exerciseRepo.streamByModelId(model.id).map(ex -> {
                long v = System.nanoTime();
                return classifyAllEdges(base_world, ex.graph_id).thenApply(nil -> Map.entry(ex.graph_id, System.nanoTime() - v));
            }).toList();

            return CompletableFuture.allOf(classifications.toArray(CompletableFuture[]::new))
                    .whenCompleteAsync((nil, error) -> {
                        if (error != null)
                            LOG.error(error);
                        FutureUtil.mergeFutureEntries(classifications).forEach((id, time) -> HintGraph.registerParsing(id, nodeRepo.getTotalVisitsFromGraph(id), time + commonTime));
                    });
        });
    }
}

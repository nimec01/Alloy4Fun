package pt.haslab.specassistant.services;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloyaddons.ExprComplexity;
import pt.haslab.alloyaddons.ParseUtil;
import pt.haslab.alloyaddons.UncheckedIOException;
import pt.haslab.specassistant.data.models.HintExercise;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.data.models.Model;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.services.treeedit.ASTEditDiff;
import pt.haslab.specassistant.util.FutureUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@ApplicationScoped
public class GraphIngestor {

    @Inject
    Logger log;

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
    public static <C> CompletableFuture<Void> walkModelTree(BiFunction<Model, C, C> step, Function<Model, Stream<Model>> modelGetter, C ctx, Model current) {
        return CompletableFuture.supplyAsync(() -> step.apply(current, ctx)).thenCompose(nextCtx -> FutureUtil.runEachAsync(modelGetter.apply(current), child -> walkModelTree(step, modelGetter, nextCtx, child)));
    }


    private static boolean testSpecModifications(CompModule original, CompModule target) {
        Set<String> root_modules = original.getAllReachableModules().makeConstList().stream().map(CompModule::path).collect(Collectors.toSet());
        return target.getAllReachableModules().makeConstList().stream().map(CompModule::path).anyMatch(x -> !root_modules.contains(x));
        //Missing Signature check
    }

    private Map<ObjectId, ObjectId> walkModelTreeStep(Map<String, HintExercise> cmdToExercise, Predicate<CompModule> modifiedPred, Model current, Map<ObjectId, ObjectId> context) {
        try {
            if (current.isValidExecution() && cmdToExercise.containsKey(current.cmd_n)) {

                CompModule world = ParseUtil.parseModel(current.code);
                HintExercise exercise = cmdToExercise.get(current.cmd_n);
                ObjectId contextId = exercise.id;
                ObjectId old_node_id = context.get(contextId);

                boolean modified = modifiedPred.test(world);

                if (exercise.isValidCommand(world, current.cmd_i)) {
                    boolean valid = current.sat == 0;

                    Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

                    ObjectId new_node_id = nodeRepo.incrementOrCreate(formula, valid, exercise.graph_id, modified ? current.id : null).id;

                    // nodeRepo.setDebug(current.id, new_node_id);

                    if (!old_node_id.equals(new_node_id)) { // No laces
                        nodeRepo.incrementLeaveById(old_node_id);
                        context = new HashMap<>(context); // Make a new context based on the previous one
                        context.put(contextId, new_node_id);
                        edgeRepo.incrementOrCreate(exercise.graph_id, old_node_id, new_node_id);
                    }
                }
            }
        } catch (UncheckedIOException e) {
            log.warn(e);
        } catch (Err e) {
            log.warn("Error parsing model, skipping " + current.id + " : " + e.getMessage());
        }
        return context;
    }

    public CompletableFuture<Void> walkModelTree(Model root, Predicate<Model> model_filter) {
        Map<String, HintExercise> cmdToExercise = exerciseRepo.streamByModelId(root.id).collect(Collectors.toUnmodifiableMap(x -> x.cmd_n, x -> x));

        if (cmdToExercise.isEmpty())
            return CompletableFuture.completedFuture(null);

        Map<ObjectId, ObjectId> initCtx = new HashMap<>();
        CompModule world = ParseUtil.parseModel(root.code);


        cmdToExercise.values().forEach(exercise -> {
            Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
            initCtx.put(exercise.id, nodeRepo.incrementOrCreate(formula, false, exercise.graph_id, null).id);
        });

        Function<Model, Stream<Model>> modelGetter = model -> modelRepo.streamByDerivationOfAndOriginal(model.id, model.original).filter(model_filter);

        return walkModelTree((m, ctx) -> walkModelTreeStep(cmdToExercise, w -> testSpecModifications(world, w), m, ctx), modelGetter, initCtx, root);
    }

    public CompletableFuture<Void> classifyAllEdges(CompModule world, ObjectId graph_id) {
        return FutureUtil.forEachAsync(edgeRepo.streamByGraphId(graph_id), e -> {
            HintNode destNode = nodeRepo.findById(e.destination);
            HintNode originNode = nodeRepo.findById(e.origin);
            try {
                Map<String, Expr> peerParsed = destNode.getParsedFormula(world);
                Map<String, Expr> originParsed = originNode.getParsedFormula(world);

                e.editDistance = ASTEditDiff.getFormulaDistanceDiff(originParsed, peerParsed);
                e.update();
            } catch (IllegalStateException e1) {
                log.warn("Error in edge classification, editDistance will be set to infinity: " + e1.getClass().getSimpleName() + ":" + e1.getMessage());
            }
        });
    }

    public void trimDeparturesFromValidNodes(ObjectId graph_id) {
        List<HintNode> nodes = nodeRepo.streamByGraphIdAndValid(graph_id).filter(x -> x.leaves > 0).peek(x -> x.leaves = 0).toList();
        edgeRepo.deleteByOriginIn(nodes.stream().map(x -> x.id).toList());
        nodeRepo.persistOrUpdate(nodes);
    }

    public void assignComplexityToGraphNodes(ObjectId graph_id, CompModule world) {
        FutureUtil.forEachAsync(nodeRepo.streamByGraphId(graph_id),
                n -> {
                    try {
                        n.complexity = n.getParsedFormula(world).values().stream().map(f -> {
                            ExprComplexity c = new ExprComplexity();
                            c.visitThis(f);
                            return c.getComplexity();
                        }).reduce(0.0, Double::sum);
                        n.update();
                    } catch (IllegalStateException e1) {
                        log.warn("Error in node classification, complexity will be set to infinity: " + e1.getClass().getSimpleName() + ":" + e1.getMessage());
                    }
                }
        );
    }

    public CompletableFuture<Void> parseModelTree(String model_id, Predicate<Model> model_filter) {
        Model model = modelRepo.findById(model_id);

        CompModule base_world = ParseUtil.parseModel(model.code);

        AtomicLong parsingTime = new AtomicLong(System.nanoTime());
        Map<ObjectId, Long> count = exerciseRepo.streamByModelId(model.id).map(x -> x.graph_id).collect(Collectors.toMap(x -> x, x -> nodeRepo.getTotalVisitsFromGraph(x)));

        return walkModelTree(model, model_filter)
                .thenRun(() -> parsingTime.updateAndGet(l -> System.nanoTime() - l))
                .thenCompose(nil -> FutureUtil.runEachAsync(exerciseRepo.streamByModelId(model.id),
                        ex -> {
                            long st = System.nanoTime();
                            AtomicLong local_count = new AtomicLong();
                            return classifyAllEdges(base_world, ex.graph_id)
                                    .thenRun(() -> assignComplexityToGraphNodes(ex.graph_id, base_world))
                                    .thenRun(() -> trimDeparturesFromValidNodes(ex.graph_id))
                                    .thenRun(() -> local_count.set(nodeRepo.getTotalVisitsFromGraph(ex.graph_id)))
                                    .thenRun(() -> HintGraph.registerParsing(ex.graph_id, model_id, local_count.get() - count.getOrDefault(ex.graph_id, 0L), parsingTime.get() + System.nanoTime() - st));
                        }))
                .whenComplete(FutureUtil.logTrace(log, "Finished parsing model " + model_id));
    }
}

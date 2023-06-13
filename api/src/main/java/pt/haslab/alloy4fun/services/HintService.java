package pt.haslab.alloy4fun.services;


import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.Counter;
import pt.haslab.alloy4fun.data.models.HintGraph.HintEdge;
import pt.haslab.alloy4fun.data.models.HintGraph.HintExercise;
import pt.haslab.alloy4fun.data.models.HintGraph.HintNode;
import pt.haslab.alloy4fun.data.models.Model;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.repositories.HintEdgeRepository;
import pt.haslab.alloy4fun.repositories.HintExerciseRepository;
import pt.haslab.alloy4fun.repositories.HintNodeRepository;
import pt.haslab.alloy4fun.repositories.ModelRepository;
import pt.haslab.alloy4fun.util.AlloyExprDifference;
import pt.haslab.alloy4fun.util.AlloyExprDifference.IndexRangeDifference;
import pt.haslab.alloy4fun.util.AlloyExprNormalizer;
import pt.haslab.alloy4fun.util.AlloyExprStringify;
import pt.haslab.alloy4fun.util.AlloyUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

@ApplicationScoped
public class HintService {
    private static final Logger LOG = Logger.getLogger(HintService.class);
    @Inject
    ModelRepository modelRepo;
    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;
    @Inject
    HintExerciseRepository exerciseRepo;

    public synchronized ObjectId incrementOrCreateNode(Map<String, String> formula, Boolean valid, Long graph_id) throws IOException {
        HintNode res = nodeRepo.findByGraphIdAndFormula(graph_id, formula).orElseGet(() -> HintNode.create(graph_id, formula, valid)).visit();
        res.persistOrUpdate();
        return res.id;
    }

    public synchronized void incrementOrCreateEdge(Long graph_id, ObjectId origin, ObjectId destination) {
        edgeRepo.findByOriginAndDestination(origin, destination).orElseGet(() -> HintEdge.createEmpty(graph_id, origin, destination)).visit().persistOrUpdate();
    }

    public void dropEverything() {
        Counter.deleteAll();
        nodeRepo.deleteAll();
        edgeRepo.deleteAll();
        exerciseRepo.deleteAll();
    }

    public void fullSetupFor(String model_id) throws IOException {
        Model model = modelRepo.findById(model_id);
        CompModule base_world = AlloyUtil.parseModel(model.code);
        generateExercisesFromAllCommands(base_world, model.id);
        try {
            walkModelTree(model).get();
            exerciseRepo.streamByModelId(model.id).forEach(x -> {
                try {
                    mutateAllNodes(base_world, x.graph_id).get();
                    mutateAllEdges(base_world, x.graph_id);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    //Debug only
    public void generateExercisesFromAllCommands(CompModule world, String model_id) {
        int s = world.getAllCommands().size();
        Set<String> labels = world.getAllCommands().stream().map(x -> x.label).collect(Collectors.toSet());
        labels.stream().map(l -> new HintExercise(model_id, Counter.nextGraphId(), s, l, Set.of(l))).forEach(x -> x.persistOrUpdate());
    }

    public Model getRootModel(String challenge) {
        Model m = getOriginal(challenge);
        while (m != null && !m.isRoot()) {
            if (m.isOriginal_()) m = modelRepo.findById(m.derivationOf);
            else m = modelRepo.findById(m.original);
        }
        return m;
    }

    public Model getOriginal(String challenge) {
        Model m = modelRepo.findById(challenge);
        if (!m.isOriginal_()) m = modelRepo.findById(m.original);
        return m;
    }

    private <Context> CompletableFuture<Void> walkModelTree(BiFunction<Model, Context, Context> step, Context ctx, Model current) {
        return supplyAsync(() -> step.apply(current, ctx))
                .thenCompose(updatedContext ->
                        CompletableFuture.allOf(modelRepo.streamByDerivationOfAndOriginal(current.id, current.original)
                                .map(child -> walkModelTree(step, updatedContext, child))
                                .toArray(CompletableFuture[]::new)
                        ));
    }

    public CompletableFuture<Void> walkModelTree(Model root) throws IOException {
        Map<String, HintExercise> cmdToExercise = exerciseRepo.streamByModelId(root.id).collect(toUnmodifiableMap(x -> x.cmd_n, x -> x));

        Map<ObjectId, ObjectId> initCtx = new HashMap<>();
        CompModule world = AlloyUtil.parseModel(root.code);

        cmdToExercise.values().forEach(exercise -> {
            try {
                Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
                initCtx.put(exercise.id, incrementOrCreateNode(formula, false, exercise.graph_id));
            } catch (IOException e) {
                LOG.error(e);
            }
        });

        return walkModelTree((m, ctx) -> walkModelTreeStep(cmdToExercise, m, ctx), initCtx, root);
    }

    private Map<ObjectId, ObjectId> walkModelTreeStep(Map<String, HintExercise> cmdToExercise, Model current, Map<ObjectId, ObjectId> context) {
        try {
            if (current.sat != null && current.sat >= 0 && cmdToExercise.containsKey(current.cmd_n)) {

                CompModule world = AlloyUtil.parseModel(current.code);
                HintExercise exercise = cmdToExercise.get(current.cmd_n);
                ObjectId contextId = exercise.id;
                ObjectId old_node_id = context.get(contextId);

                // If the command index is above or equal to the first "secret" index
                // (meteor currently places secrets as the last defined predicates)
                if (current.cmd_i >= world.getAllCommands().size() - exercise.secret_cmd_count) {
                    boolean valid = current.sat == 0;

                    Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

                    ObjectId new_node_id = incrementOrCreateNode(formula, valid, exercise.graph_id);

                    if (!old_node_id.equals(new_node_id)) { // No laces
                        nodeRepo.incrementLeaveById(old_node_id);
                        context = new HashMap<>(context); // Make a new context based on the previous one
                        context.put(contextId, new_node_id);
                        incrementOrCreateEdge(exercise.graph_id, old_node_id, new_node_id);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn(e);
        }
        return context;
    }

    public CompletableFuture<Void> mutateAllNodes(CompModule world, Long graph_id) {
        return CompletableFuture.allOf(nodeRepo.streamByGraphId(graph_id).map(x -> CompletableFuture.runAsync(() -> connectMutations(world, x))).toArray(CompletableFuture[]::new));
    }

    private void connectMutations(CompModule world, HintNode target) {
        Map<ObjectId, HintEdge> existingEdges = edgeRepo.streamByOriginId(target.id).collect(toMap(x -> x.destination, x -> x));

        List<HintEdge> new_edges = new ArrayList<>();

        nodeRepo.findByGraphIdAndFormulaIn(target.graph_id, HintNode.findAllMutations(world, target.formula))
                .forEach(node -> {
                    if (!existingEdges.containsKey(node.id))
                        new_edges.add(HintEdge.createEmpty(target.graph_id, target.id, node.id));
                });

        if (!new_edges.isEmpty())
            edgeRepo.persistOrUpdate(new_edges);
    }

    public CompletableFuture<Void> mutateAllEdges(CompModule world, Long graph_id) {
        return CompletableFuture.allOf(edgeRepo.streamByGraphId(graph_id).map(e -> CompletableFuture.runAsync(() -> e.computeDifferences(world, nodeRepo::findById).update())).toArray(CompletableFuture[]::new));
    }

    public void computePolicyForGraph(Long graph_id) {
        Collection<ScoreTraversalContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(ScoreTraversalContext::init).toList();

        while (!batch.isEmpty()) {
            ScoreTraversalContext targetScore = Collections.max(batch);

            Map<Boolean, List<ScoreTraversalContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(x -> x.compareTo(targetScore) >= 0));

            try {
                List<CompletableFuture<List<ScoreTraversalContext>>> actionPool = targetIds.get(true)
                        .stream()
                        .map(x -> supplyAsync(() ->
                                edgeRepo.streamByDestinationNodeIdAndAllScoreLT(x.nodeId(), x.currentScore)
                                        .map(y -> x.scoreOriginEdge(y, nodeRepo.findById(y.origin))).filter(Objects::nonNull).toList())).toList();

                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();

                List<List<ScoreTraversalContext>> result = new ArrayList<>();
                result.add(targetIds.get(false));
                for (CompletableFuture<List<ScoreTraversalContext>> l : actionPool) {
                    result.add(l.get());
                }

                batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(toMap(ScoreTraversalContext::nodeId, x -> x, ScoreTraversalContext::maxScored)).values());

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
            }
        }
    }

    public InstanceMsg getHint(String originId, String command_label, Collection<Func> skolem) {
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(originId, command_label).orElse(null);
        if (exercise == null) return InstanceMsg.hintFrom(null, "NO GRAPH AVAILABLE");

        Long graph_id = exercise.graph_id;

        Map<String, Expr> formulaExpr = HintNode.getNormalizedFormulaExprFrom(skolem, exercise.targetFunctions);

        Map<String, String> formula = formulaExpr.entrySet().stream().collect(toMap(Map.Entry::getKey, x -> AlloyExprStringify.stringifyAndDiscardTrue(x.getValue())));

        Optional<HintNode> node = nodeRepo.findByGraphIdAndFormula(graph_id, formula);

        if (node.isEmpty()) {
            return InstanceMsg.hintFrom(null, "NO MATCH");
        }

        ObjectId node_id = node.orElseThrow().id;
        Optional<HintEdge> opt = edgeRepo.findBestScoredByOriginNode(node_id);

        if (opt.isEmpty() || opt.orElseThrow().score == null) return InstanceMsg.hintFrom(null, "NODE IS ISOLATED");
        HintEdge edge = opt.orElseThrow();

        AtomicReference<String> message = new AtomicReference<>("NO DIFF");
        AtomicReference<Pos> pos = new AtomicReference<Pos>(null);

        edge.differenceRange.entrySet()
                .stream()
                .findFirst()
                .ifPresent(entry -> {
                    String label = entry.getKey();
                    IndexRangeDifference indexRangeDiff = entry.getValue().stream().findFirst().orElseThrow();
                    AlloyExprDifference diff = AlloyExprDifference.createHalfA(formulaExpr.get(label));
                    pos.set(diff.findDelimiterPosA(indexRangeDiff));
                    message.set(diff.getHintMessage(indexRangeDiff));
                });


        return InstanceMsg.hintFrom(pos.get(), message.get());

    }

    public static class ScoreTraversalContext implements Comparable<ScoreTraversalContext> {

        public HintNode node;
        public Double probability;
        public Double currentScore;
        public int maxEditDistance, aggEditDistance, distance;

        public static ScoreTraversalContext init(HintNode node) {
            ScoreTraversalContext result = new ScoreTraversalContext();
            result.node = node;
            result.probability = result.currentScore = 1.0;
            result.maxEditDistance = 0;
            result.distance = 0;
            return result;
        }

        public ObjectId nodeId() {
            return node.id;
        }


        public ScoreTraversalContext maxScored(ScoreTraversalContext scoreTraversalContext) {
            if (this.currentScore.compareTo(scoreTraversalContext.currentScore) > 0) return this;
            return scoreTraversalContext;
        }

        public ScoreTraversalContext scoreOriginEdge(HintEdge edge, HintNode origin_node) {
            try {
                ScoreTraversalContext result = new ScoreTraversalContext();

                result.node = origin_node;
                result.probability = this.probability * (edge.count / origin_node.leaves);

                result.maxEditDistance = Integer.max(edge.editDistance, this.maxEditDistance);
                result.aggEditDistance += edge.editDistance;
                result.distance = distance + 1;


                edge.score = result.currentScore = result.probability;

                edge.persistOrUpdate();

                return result;
            } catch (ArithmeticException e) {
                return null;
            }
        }

        @Override
        public int compareTo(ScoreTraversalContext o) {
            return this.currentScore.compareTo(o.currentScore);
        }

        @Override
        public String toString() {
            return "{probability=" + probability +
                    ", currentScore=" + currentScore +
                    ", distance=" + distance +
                    '}';
        }
    }
}

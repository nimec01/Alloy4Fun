package pt.haslab.alloy4fun.services;


import at.unisalzburg.dbresearch.apted.costmodel.CostModel;
import at.unisalzburg.dbresearch.apted.costmodel.ExprDataShallowCostModel;
import at.unisalzburg.dbresearch.apted.costmodel.ExprShallowCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.ExprData;
import at.unisalzburg.dbresearch.apted.node.ExprToNode;
import at.unisalzburg.dbresearch.apted.node.ExprToNodeExprData;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.logging.Log;
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
import pt.haslab.alloy4fun.data.transfer.ScoreTraversalContext;
import pt.haslab.alloy4fun.repositories.HintEdgeRepository;
import pt.haslab.alloy4fun.repositories.HintExerciseRepository;
import pt.haslab.alloy4fun.repositories.HintNodeRepository;
import pt.haslab.alloy4fun.repositories.ModelRepository;
import pt.haslab.alloy4fun.util.*;
import pt.haslab.mutation.Candidate;
import pt.haslab.mutation.mutator.Mutator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private CompModule getWorld(String model_id) {
        return AlloyUtil.parseModel(modelRepo.findById(model_id).code);
    }

    public HintEdge computeDifferences(HintEdge edge, CompModule world, Function<ObjectId, HintNode> nodeGetter) {
        try {
            HintNode originN = nodeGetter.apply(edge.origin);
            HintNode originD = nodeGetter.apply(edge.destination);
            Map<String, Expr> originParsed = originN.getParsedFormula(Optional.ofNullable(originN.witness).map(this::getWorld).orElse(world));
            Map<String, Expr> peerParsed = originD.getParsedFormula(Optional.ofNullable(originD.witness).map(this::getWorld).orElse(world));

            edge.editDistance = getFormulaMapDiff(originParsed, peerParsed).values().stream().map(APTED::computeEditDistance).reduce(0.0f, Float::sum);
        } catch (ErrorSyntax e) {
            Log.warn("SYNTAX ERROR WHILE PARSING FORMULA: " + e.getMessage());
        } catch (Err e) {
            Log.warn("ALLOY ERROR WHILE PARSING FORMULA: " + e.getMessage());
        } catch (UncheckedIOException e) {
            Log.error("IO ERROR WHILE PARSING FORMULA: " + e.getMessage());
        }
        return edge;
    }

    private static Map<String, APTED<Expr, ExprShallowCostModel>> getFormulaMapDiff(Map<String, Expr> origin, Map<String, Expr> peer) {
        return Stream.of(origin.keySet(), peer.keySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .stream()
                .collect(toMap(key -> key,
                        key -> new APTED<Expr, ExprShallowCostModel>(ExprShallowCostModel::new).init(ExprToNode.parseOrDefault(origin.get(key)), ExprToNode.parseOrDefault(peer.get(key)))));
    }

    public synchronized ObjectId incrementOrCreateNode(Map<String, String> formula, Boolean valid, Long graph_id, String witness) {
        HintNode res = nodeRepo.findByGraphIdAndFormula(graph_id, formula).orElseGet(() -> HintNode.create(graph_id, formula, valid, witness)).visit();
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

    public void fullSetupFor(String model_id) {
        Model model = modelRepo.findById(model_id);
        CompModule base_world = AlloyUtil.parseModel(model.code);
        generateExercisesFromAllCommands(base_world, model.id);
        try {
            walkModelTree(model).get();
            exerciseRepo.streamByModelId(model.id).forEach(x -> {
                try {
                    classifyAllEdges(base_world, x.graph_id)
                            .thenCompose(v -> {
                                computePolicyForGraph(x.graph_id);
                                return CompletableFuture.completedFuture(Void.TYPE);
                            }).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void computePolicyForAll(String model_id) {
        Model model = modelRepo.findById(model_id);
        CompModule base_world = AlloyUtil.parseModel(model.code);
        exerciseRepo.streamByModelId(model.id).forEach(x -> computePolicyForGraph(x.graph_id));
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
        if (m != null && !m.isOriginal_()) m = modelRepo.findById(m.original);
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

    public CompletableFuture<Void> walkModelTree(Model root) {
        Map<String, HintExercise> cmdToExercise = exerciseRepo.streamByModelId(root.id).collect(toUnmodifiableMap(x -> x.cmd_n, x -> x));

        Map<ObjectId, ObjectId> initCtx = new HashMap<>();
        CompModule world = AlloyUtil.parseModel(root.code);

        Set<String> modules = world.getAllReachableModules().makeConstList().stream().map(CompModule::path).collect(Collectors.toSet());

        cmdToExercise.values().forEach(exercise -> {
            Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
            initCtx.put(exercise.id, incrementOrCreateNode(formula, false, exercise.graph_id, null));
        });

        return walkModelTree((m, ctx) -> walkModelTreeStep(cmdToExercise, modules, m, ctx), initCtx, root);
    }

    private Map<ObjectId, ObjectId> walkModelTreeStep(Map<String, HintExercise> cmdToExercise, Set<String> root_modules, Model current, Map<ObjectId, ObjectId> context) {
        try {
            if (current.sat != null && current.sat >= 0 && cmdToExercise.containsKey(current.cmd_n)) {

                CompModule world = AlloyUtil.parseModel(current.code);
                HintExercise exercise = cmdToExercise.get(current.cmd_n);
                ObjectId contextId = exercise.id;
                ObjectId old_node_id = context.get(contextId);

                boolean modified = world.getAllReachableModules().makeConstList().stream().map(CompModule::path).anyMatch(x -> !root_modules.contains(x));

                // If the command index is above or equal to the first "secret" index
                // (meteor currently places secrets as the last defined predicates)
                if (current.cmd_i >= world.getAllCommands().size() - exercise.secret_cmd_count) {
                    boolean valid = current.sat == 0;

                    Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

                    ObjectId new_node_id = incrementOrCreateNode(formula, valid, exercise.graph_id, modified ? current.id : null);

                    if (!old_node_id.equals(new_node_id)) { // No laces
                        nodeRepo.incrementLeaveById(old_node_id);
                        context = new HashMap<>(context); // Make a new context based on the previous one
                        context.put(contextId, new_node_id);
                        incrementOrCreateEdge(exercise.graph_id, old_node_id, new_node_id);
                    }
                }
            }
        } catch (UncheckedIOException e) {
            LOG.warn(e);
        }
        return context;
    }

    public CompletableFuture<Void> classifyAllEdges(CompModule world, Long graph_id) {
        return CompletableFuture.allOf(edgeRepo.streamByGraphId(graph_id).map(e -> CompletableFuture.runAsync(() -> computeDifferences(e, world, nodeRepo::findById).update())).toArray(CompletableFuture[]::new));
    }

    public void computePolicyForGraph(Long graph_id) {
        Collection<ScoreTraversalContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(ScoreTraversalContext::init).toList();

        while (!batch.isEmpty()) {
            ScoreTraversalContext targetScore = Collections.min(batch);

            Map<Boolean, List<ScoreTraversalContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(x -> x.compareTo(targetScore) <= 0));

            try {
                List<CompletableFuture<List<ScoreTraversalContext>>> actionPool = targetIds.get(true)
                        .stream()
                        .map(x -> supplyAsync(() ->
                                edgeRepo.streamByDestinationNodeIdAndAllScoreGT(x.nodeId(), x.cost)
                                        .map(y -> x.scoreEdgeOrigin(y, nodeRepo::findById)).filter(Objects::nonNull).toList())).toList();

                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();

                List<List<ScoreTraversalContext>> result = new ArrayList<>();
                result.add(targetIds.get(false));
                for (CompletableFuture<List<ScoreTraversalContext>> l : actionPool) {
                    result.add(l.get());
                }

                batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(toMap(ScoreTraversalContext::nodeId, x -> x, ScoreTraversalContext::bestScored)).values());

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
            }
        }
    }

    public void deBloatEdgesForGraphID(Long graph_id) {
        edgeRepo.deleteByScoreNull(graph_id);
    }

    public void removeIsolatedNodesForGraphID(Long graph_id) {
        nodeRepo.streamByGraphId(graph_id).forEach(n -> {
            if (!edgeRepo.hasEndpoint(n.id))
                nodeRepo.deleteById(n.id);
        });
    }

    public Optional<InstanceMsg> getHint(String originId, String command_label, CompModule world) {
        String original_id = getOriginalId(originId);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original_id, command_label).orElse(null);
        if (exercise == null)
            return Optional.empty();

        Long graph_id = exercise.graph_id;

        Optional<InstanceMsg> result = hintWithMutation(graph_id, world.getAllFunc().makeConstList(), world.getAllReachableSigs(), exercise);

        if (result.isPresent())
            return result;

        return hintWithGraph(world, exercise, graph_id);
    }

    private String getOriginalId(String model_id) {
        Model m = modelRepo.findById(model_id);
        if (m.isOriginal_())
            return m.id;
        return m.original;
    }

    private Optional<InstanceMsg> hintWithGraph(CompModule world, HintExercise exercise, Long graph_id) {
        Map<String, Expr> formulaExpr = HintNode.getNormalizedFormulaExprFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

        Map<String, String> formula = formulaExpr.entrySet().stream().collect(toMap(Map.Entry::getKey, x -> AlloyExprStringify.stringify(x.getValue())));

        Optional<HintNode> node_opt = nodeRepo.findByGraphIdAndFormula(graph_id, formula);

        if (node_opt.isPresent()) {
            HintNode origin_node = node_opt.orElseThrow();
            Optional<HintEdge> edge_opt = edgeRepo.findBestScoredByOriginNode(origin_node.id);
            if (edge_opt.isPresent()) {
                HintEdge edge = edge_opt.orElseThrow();
                node_opt = nodeRepo.findByIdOptional(edge.destination);
                if (node_opt.isPresent()) {
                    HintNode dest_node = node_opt.orElseThrow();

                    Map<String, Expr> otherFormulaExpr = dest_node.getParsedFormula(Optional.ofNullable(dest_node.witness).map(this::getWorld).orElse(world));
                    for (String s : formula.keySet()) {
                        APTED<ExprData, ExprDataShallowCostModel> apted = new APTED<ExprData, ExprDataShallowCostModel>(ExprDataShallowCostModel::new);

                        apted.computeEditDistance(ExprToNodeExprData.parseOrDefault(formulaExpr.get(s)), ExprToNodeExprData.parseOrDefault(otherFormulaExpr.get(s)));

                        Map.Entry<CostModel.OpType, ExprData> change = apted.mappingFirstChange(apted.computeEditMapping());

                        if (change != null) {
                            return switch (change.getKey()) {
                                case Rename, Delete ->
                                        Optional.of(InstanceMsg.hintFrom(change.getValue().position(), "Try to change this declaration"));
                                case Insertion ->
                                        Optional.of(InstanceMsg.hintFrom(change.getValue().position(), "Try to add something to this declaration"));
                            };
                        }

                    }
                }
            }
        }
        return Optional.empty();
    }


    public Optional<InstanceMsg> hintWithMutation(Long graph_id, Collection<Func> skolem, ConstList<Sig> sigs, HintExercise exercise) {
        List<Map<String, Candidate>> candidateFormulas = AlloyUtil.makeCandidateMaps(HintNode.getFormulaExprFrom(skolem, exercise.targetFunctions), sigs, 1);

        List<Map<String, String>> mutatedFormulas = candidateFormulas.stream().map(m -> Static.mapValues(m, f -> AlloyExprStringify.stringify(AlloyExprNormalizer.normalize(f.mutated)))).toList();

        Optional<HintNode> e = nodeRepo.findBestByGraphIdAndFormulaIn(graph_id, mutatedFormulas);

        if (e.isPresent()) {
            HintNode n = e.orElseThrow();
            int target = mutatedFormulas.indexOf(n.formula);

            for (Candidate c : candidateFormulas.get(target).values()) {
                if (!c.mutators.isEmpty()) {
                    for (Mutator m : c.mutators) {
                        if (m.hint().isPresent())
                            return Optional.of(InstanceMsg.hintFrom(m.original.expr.pos(), m.hint().orElseThrow()));
                    }
                }
            }
        }

        return Optional.empty();
    }
}

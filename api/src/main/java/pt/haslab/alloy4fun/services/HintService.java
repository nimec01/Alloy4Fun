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
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.alloy4fun.data.models.HintGraph.HintEdge;
import pt.haslab.alloy4fun.data.models.HintGraph.HintExercise;
import pt.haslab.alloy4fun.data.models.HintGraph.HintGraph;
import pt.haslab.alloy4fun.data.models.HintGraph.HintNode;
import pt.haslab.alloy4fun.data.models.Model;
import pt.haslab.alloy4fun.data.transfer.ExerciseForm;
import pt.haslab.alloy4fun.data.transfer.InstanceMsg;
import pt.haslab.alloy4fun.data.transfer.ScoreTraversalContext;
import pt.haslab.alloy4fun.data.transfer.YearRange;
import pt.haslab.alloy4fun.repositories.HintEdgeRepository;
import pt.haslab.alloy4fun.repositories.HintExerciseRepository;
import pt.haslab.alloy4fun.repositories.HintNodeRepository;
import pt.haslab.alloy4fun.repositories.ModelRepository;
import pt.haslab.alloy4fun.util.*;
import pt.haslab.mutation.Candidate;
import pt.haslab.mutation.mutator.Mutator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static Map<String, APTED<Expr, ExprShallowCostModel>> getFormulaMapDiff(Map<String, Expr> origin, Map<String, Expr> peer) {
        return Stream.of(origin.keySet(), peer.keySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(key -> key,
                        key -> new APTED<Expr, ExprShallowCostModel>(ExprShallowCostModel::new).init(ExprToNode.parseOrDefault(origin.get(key)), ExprToNode.parseOrDefault(peer.get(key)))));
    }

    private static <Context> CompletableFuture<Void> walkModelTree(BiFunction<Model, Context, Context> step, Function<Model, Stream<Model>> modelGetter, Context ctx, Model current) {
        return CompletableFuture.supplyAsync(() -> step.apply(current, ctx)).thenCompose(updatedContext -> CompletableFuture.allOf((modelGetter.apply(current).map(child -> walkModelTree(step, modelGetter, updatedContext, child)).toArray(CompletableFuture[]::new))));
    }

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

    public synchronized ObjectId incrementOrCreateNode(Map<String, String> formula, Boolean valid, ObjectId graph_id, String witness) {
        HintNode res = nodeRepo.findByGraphIdAndFormula(graph_id, formula).orElseGet(() -> HintNode.create(graph_id, formula, valid, witness)).visit();
        res.persistOrUpdate();
        return res.id;
    }

    public synchronized void incrementOrCreateEdge(ObjectId graph_id, ObjectId origin, ObjectId destination) {
        edgeRepo.findByOriginAndDestination(origin, destination).orElseGet(() -> HintEdge.createEmpty(graph_id, origin, destination)).visit().persistOrUpdate();
    }

    public void cleanGraph(ObjectId graph_id) {
        nodeRepo.deleteByGraphId(graph_id);
        edgeRepo.deleteByGraphId(graph_id);
    }

    public void deleteGraph(ObjectId graph_id) {
        cleanGraph(graph_id);
        exerciseRepo.deleteByGraphId(graph_id);
        HintGraph.deleteById(graph_id);
    }

    public void dropEverything() {
        HintGraph.deleteAll();
        nodeRepo.deleteAll();
        edgeRepo.deleteAll();
        exerciseRepo.deleteAll();
    }

    public void parseModelHint(String model_id, YearRange year) {
        Model model = modelRepo.findById(model_id);
        CompModule base_world = AlloyUtil.parseModel(model.code);
        try {
            long commonT = System.nanoTime();
            if (year == null)
                walkModelTree(model).get();
            else {
                year.cacheDates();
                walkModelTree(model, year::testDate).get();
            }
            commonT = System.nanoTime() - commonT;

            List<CompletableFuture<Map.Entry<ObjectId, Long>>> classifications = exerciseRepo.streamByModelId(model.id).map(ex -> {
                long v = System.nanoTime();
                return classifyAllEdges(base_world, ex.graph_id).thenApply(nil -> Map.entry(ex.graph_id, System.nanoTime() - v));
            }).toList();

            CompletableFuture.allOf(classifications.toArray(CompletableFuture[]::new)).get();

            Map<ObjectId, Long> classificationTimes = new HashMap<>();
            for (CompletableFuture<Map.Entry<ObjectId, Long>> cl : classifications) {
                Map.Entry<ObjectId, Long> et = cl.get();
                classificationTimes.put(et.getKey(), et.getValue() + classificationTimes.getOrDefault(et.getKey(), commonT));
            }
            classificationTimes.forEach(HintGraph::registerParsingTime);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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

    private String getOriginalId(String model_id) {
        Model m = modelRepo.findById(model_id);
        if (m.isOriginal_())
            return m.id;
        return m.original;
    }

    public void debloatGraph(ObjectId graph_id) {
        edgeRepo.deleteByScoreNull(graph_id);
        nodeRepo.deleteByScoreNull(graph_id);
        HintGraph.setPolicySubmissionCount(graph_id, nodeRepo.getTotalVisitsFromGraph(graph_id));
    }

    public CompletableFuture<Void> walkModelTree(Model root) {
        return walkModelTree(root, null);
    }

    public CompletableFuture<Void> walkModelTree(Model root, Predicate<LocalDateTime> dateFilter) {
        Map<String, HintExercise> cmdToExercise = exerciseRepo.streamByModelId(root.id).collect(Collectors.toUnmodifiableMap(x -> x.cmd_n, x -> x));

        if (cmdToExercise.isEmpty())
            return CompletableFuture.completedFuture(null);

        Map<ObjectId, ObjectId> initCtx = new HashMap<>();
        CompModule world = AlloyUtil.parseModel(root.code);

        Set<String> modules = world.getAllReachableModules().makeConstList().stream().map(CompModule::path).collect(Collectors.toSet());

        cmdToExercise.values().forEach(exercise -> {
            Map<String, String> formula = HintNode.getNormalizedFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
            initCtx.put(exercise.id, incrementOrCreateNode(formula, false, exercise.graph_id, null));
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
            if (current.sat != null && current.sat >= 0 && cmdToExercise.containsKey(current.cmd_n)) {

                CompModule world = AlloyUtil.parseModel(current.code);
                HintExercise exercise = cmdToExercise.get(current.cmd_n);
                HintGraph.incrementParsingCount(exercise.graph_id);
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

    public CompletableFuture<Void> classifyAllEdges(CompModule world, ObjectId graph_id) {
        return CompletableFuture.allOf(edgeRepo.streamByGraphId(graph_id).map(e -> CompletableFuture.runAsync(() -> computeDifferences(e, world, nodeRepo::findById).update())).toArray(CompletableFuture[]::new));
    }

    public void computePolicyForGraph(ObjectId graph_id) {
        HintGraph.removeAllPolicyStats(graph_id);
        long t = System.nanoTime();
        Collection<ScoreTraversalContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(ScoreTraversalContext::init).toList();

        while (!batch.isEmpty()) {
            ScoreTraversalContext targetScore = Collections.min(batch);

            Map<Boolean, List<ScoreTraversalContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(x -> x.compareTo(targetScore) <= 0));

            try {
                List<CompletableFuture<List<ScoreTraversalContext>>> actionPool = targetIds.get(true)
                        .stream()
                        .peek(ScoreTraversalContext::assignScore)
                        .map(x -> CompletableFuture.supplyAsync(() ->
                                edgeRepo.streamByDestinationNodeIdAndAllScoreGT(x.nodeId(), x.cost)
                                        .map(y -> x.scoreEdgeOrigin(y, nodeRepo::findById)).filter(Objects::nonNull).toList())).toList();

                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();

                List<List<ScoreTraversalContext>> result = new ArrayList<>();
                result.add(targetIds.get(false));
                for (CompletableFuture<List<ScoreTraversalContext>> l : actionPool) {
                    result.add(l.get());
                }

                batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(Collectors.toMap(ScoreTraversalContext::nodeId, x -> x, ScoreTraversalContext::bestScored)).values());

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
            }
        }
        HintGraph.registerPolicyCalculationTime(graph_id, System.nanoTime() - t);
    }

    public Optional<InstanceMsg> getHint(String originId, String command_label, CompModule world) {
        String original_id = getOriginalId(originId);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original_id, command_label).orElse(null);
        if (exercise == null)
            return Optional.empty();

        ObjectId graph_id = exercise.graph_id;

        Optional<InstanceMsg> result = hintWithMutation(graph_id, world.getAllFunc().makeConstList(), world.getAllReachableSigs(), exercise);

        if (result.isPresent())
            return result;

        return hintWithGraph(world, exercise, graph_id);
    }

    public Boolean testAllHints(String original_id, String command_label, CompModule world) {
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original_id, command_label).orElse(null);
        if (exercise == null)
            return false;

        ObjectId graph_id = exercise.graph_id;

        Optional<InstanceMsg> a = hintWithMutation(graph_id, world.getAllFunc().makeConstList(), world.getAllReachableSigs(), exercise);
        Optional<InstanceMsg> b = hintWithGraph(world, exercise, graph_id);

        HintGraph.registerMultipleHintAttempt(graph_id, a.isPresent(), b.isPresent());

        return a.isPresent() || b.isPresent();
    }

    private Optional<InstanceMsg> hintWithGraph(CompModule world, HintExercise exercise, ObjectId graph_id) {
        Map<String, Expr> formulaExpr = HintNode.getNormalizedFormulaExprFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

        Map<String, String> formula = formulaExpr.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> AlloyExprStringify.stringify(x.getValue())));

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


    public Optional<InstanceMsg> hintWithMutation(ObjectId graph_id, Collection<Func> skolem, ConstList<Sig> sigs, HintExercise exercise) {
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

    public int generateExercisesWithGraphId(ObjectId graph_id, Collection<ExerciseForm> comandsToModelIds) {
        List<HintExercise> exs = comandsToModelIds.stream()
                .filter(x -> exerciseRepo.notExistsModelIdAndCmdN(x.modelId, x.cmd_n))
                .map(x -> new HintExercise(x.modelId, graph_id, x.secretCommandCount, x.cmd_n, x.targetFunctions))
                .toList();
        if (!exs.isEmpty())
            exerciseRepo.persistOrUpdate(exs);
        return exs.size();
    }

    public void generateExercisesWithGraphIdFromSecrets(Function<String, ObjectId> commandToGraphId, String model_id) {
        Model m = modelRepo.findByIdOptional(model_id).orElseThrow();
        CompModule world = AlloyUtil.parseModel(m.code);
        List<Pos> secretPositions = AlloyUtil.secretPos(world.path, m.code);

        Map<String, Set<String>> targets = AlloyUtil.getSecretFunctionTargetsOf(world, secretPositions);
        Integer cmdCount = targets.size();

        exerciseRepo.persistOrUpdate(targets.entrySet().stream().map(x -> new HintExercise(model_id, commandToGraphId.apply(x.getKey()), cmdCount, x.getKey(), x.getValue())));
    }


    public void testAllHintsOfModel(String modelId, YearRange yearRange) {
        yearRange.cacheDates();
        try {
            CompletableFuture.allOf(modelRepo.streamByOriginalAndUnSat(modelId)
                    .filter(x -> yearRange.testDate(Text.parseDate(x.time)))
                    .map(x -> CompletableFuture.runAsync(() -> testAllHints(x.original, x.cmd_n, AlloyUtil.parseModel(x.code))))
                    .toArray(CompletableFuture[]::new)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    public Set<ObjectId> getModelGraphs(String modelid) {
        return exerciseRepo.streamByModelId(modelid).map(x -> x.graph_id).collect(Collectors.toSet());
    }


    public void deleteExerciseByModelIDs(List<String> ids, boolean cascadeToGraphs) {
        if (!cascadeToGraphs) {
            exerciseRepo.deleteByModelIdIn(ids);
        } else {
            Set<ObjectId> graph_ids = exerciseRepo.streamByModelIdIn(ids).map(x -> x.graph_id).collect(Collectors.toSet());
            exerciseRepo.deleteByModelIdIn(ids);
            graph_ids.forEach(graph_id -> {
                if (!exerciseRepo.containsGraph(graph_id))
                    deleteGraph(graph_id);
            });
        }
    }

}

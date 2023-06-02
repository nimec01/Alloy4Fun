package pt.haslab.alloy4fun.services;


import edu.mit.csail.sdg.alloy4.Pos;
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
import pt.haslab.alloy4fun.util.AlloyUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

    public HintNode getOrCreateNode(Map<String, String> formula, Boolean valid, Long graph_id) throws IOException {
        return nodeRepo.findByGraphIdAndFormula(graph_id, formula).orElseGet(() -> HintNode.createPersistent(graph_id, formula, valid));
    }

    public HintEdge getOrCreateEdge(String origin, String destination) {
        return edgeRepo.findByOriginAndDestination(origin, destination).orElseGet(() -> HintEdge.createEmpty(origin, destination));
    }

    //Debug only
    public void generateExercisesFromAllCommands(Model model) {
        try {
            CompModule world = AlloyUtil.parseModel(model.code);

            int s = world.getAllCommands().size();

            Set<String> labels = world.getAllCommands().stream().map(x -> x.label).collect(Collectors.toSet());

            labels.stream().map(l -> new HintExercise(model.id, Counter.nextGraphId(), s, l, Set.of(l))).forEach(x -> x.persistOrUpdate());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Model getRootModel(String challenge) {
        Model m = modelRepo.findById(challenge);
        while (m != null && !m.isRoot()) {
            if (m.isOriginal_()) m = modelRepo.findById(m.derivationOf);
            else m = modelRepo.findById(m.original);
        }
        return m;
    }

    public Model getOriginal(String challenge) {
        Model m = modelRepo.findById(challenge);
        if (!m.isOriginal_())
            m = modelRepo.findById(m.original);
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

    public CompletableFuture<Void> countWalkTree(Model root) throws IOException {
        Map<String, HintExercise> cmdToExercise = exerciseRepo.streamByModelId(root.id)
                .collect(toUnmodifiableMap(x -> x.cmd_n, x -> x));

        Map<String, String> initCtx = new HashMap<>();
        CompModule world = AlloyUtil.parseModel(root.code);

        cmdToExercise.values().forEach(exercise -> {
            try {
                Map<String, String> formula = HintNode.getFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);
                initCtx.put(exercise.id.toHexString(), getOrCreateNode(formula, false, exercise.graph_id).id.toHexString());
            } catch (IOException e) {
                LOG.error(e);
            }
        });

        return walkModelTree((m, ctx) -> stepOfCountWalk(cmdToExercise, m, ctx), initCtx, root);
    }

    private Map<String, String> stepOfCountWalk(Map<String, HintExercise> cmdToExercise, Model current, Map<String, String> context) {
        try {
            if (current.sat != null && current.sat >= 0 && cmdToExercise.containsKey(current.cmd_n)) {

                CompModule world = AlloyUtil.parseModel(current.code);
                HintExercise exercise = cmdToExercise.get(current.cmd_n);
                String contextId = exercise.id.toHexString();
                String old_node_id = context.get(contextId);

                // If the command index is above or equal to the first "secret" index
                // (meteor currently places secrets as the last defined predicates)
                if (current.cmd_i >= world.getAllCommands().size() - exercise.secret_cmd_count) {
                    // sat == 0 means there were instances, sat == 1 means there were no instances
                    // If the command is a check (cmd_c is true) than instances == invalid
                    // If the command is a run (cmd_c is false) than instances == valid
                    boolean valid = current.cmd_c == (current.sat != 0);

                    Map<String, String> formula = HintNode.getFormulaFrom(world.getAllFunc().makeConstList(), exercise.targetFunctions);

                    nodeRepo.incrementLeaveById(new ObjectId(old_node_id));
                    HintNode node = getOrCreateNode(formula, valid, exercise.graph_id).registerEntry();
                    node.persistOrUpdate();
                    String new_node_id = node.id.toHexString();

                    if (!old_node_id.equals(new_node_id)) { // No laces

                        context = new HashMap<>(context); // Make a new context based on the previous one
                        context.put(contextId, new_node_id);
                        getOrCreateEdge(old_node_id, new_node_id).directedIncrement(new_node_id).persistOrUpdate();
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn(e);
        }
        return context;
    }

    public void computePolicyForGraph(Long graph_id) {
        Collection<ScoreTraversalContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(x ->  ScoreTraversalContext.init(x)).toList();

        while (!batch.isEmpty()) {
            ScoreTraversalContext targetScore = Collections.max(batch);

            Map<Boolean, List<ScoreTraversalContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(x -> x.compareTo(targetScore) >= 0));

            try {
                List<CompletableFuture<List<ScoreTraversalContext>>> actionPool = targetIds.get(true).stream()
                        .map(x -> supplyAsync(
                                () -> edgeRepo.findByNodeIdAndAllScoreLT(x.nodeId(), x.currentScore)
                                        .map(x::scoreWithEdge)
                                        .filter(Objects::nonNull)
                                        .toList()))
                        .toList();

                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();

                List<List<ScoreTraversalContext>> result = new ArrayList<>();
                result.add(targetIds.get(false));
                for (CompletableFuture<List<ScoreTraversalContext>> l : actionPool) {
                    result.add(l.get());
                }

                batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(toMap(ScoreTraversalContext::nodeId, x -> x, ScoreTraversalContext::maxScoring)).values());

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
            }
        }
    }

    public InstanceMsg getHint(String originId, String command_label, Collection<Func> skolem) {
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(originId, command_label).orElse(null);
        if (exercise == null)
            return InstanceMsg.hintFrom(Pos.UNKNOWN, "NO GRAPH AVAILABLE");

        Long graph_id = exercise.graph_id;

        Map<String, String> formula = HintNode.getFormulaFrom(skolem, exercise.targetFunctions);

        Optional<HintNode> node = nodeRepo.findByGraphIdAndFormula(graph_id, formula);

        if (node.isEmpty())
            return InstanceMsg.hintFrom(Pos.UNKNOWN, "NO MATCH");

        String node_id = node.orElseThrow().id.toHexString();
        Optional<HintEdge> opt = edgeRepo.findBestScoredByNode(node_id);

        String target = "";

        if (opt.isEmpty())
            return InstanceMsg.hintFrom(Pos.UNKNOWN, "NODE IS ISOLATED");

        HintNode res = nodeRepo.findById(new ObjectId(opt.orElseThrow().getDifferentNodeId(node_id)));

        return InstanceMsg.hintFrom(Pos.UNKNOWN, "HINT: debug:: go to " + res.formula);

    }

    public static class ScoreTraversalContext implements Comparable<ScoreTraversalContext> {

        public HintNode node;
        public Double probability;
        public Double currentScore;


        public static ScoreTraversalContext init(HintNode node) {
            ScoreTraversalContext result = new ScoreTraversalContext();
            result.node = node;
            result.probability = result.currentScore = 1.0;
            return result;
        }

        public String nodeId() {
            return node.id.toHexString();
        }


        public ScoreTraversalContext maxScoring(ScoreTraversalContext scoreTraversalContext) {
            if (this.currentScore.compareTo(scoreTraversalContext.currentScore) > 0)
                return this;
            else return scoreTraversalContext;
        }

        public ScoreTraversalContext scoreWithEdge(HintEdge edge) {



            //HintNode next = nodeRepo.findById(new ObjectId(edge.getDifferentNodeId(node.id.toHexString())));


            return null; //Todo
        }

        @Override
        public int compareTo(ScoreTraversalContext o) {
            return this.currentScore.compareTo(o.currentScore);
        }
    }
}

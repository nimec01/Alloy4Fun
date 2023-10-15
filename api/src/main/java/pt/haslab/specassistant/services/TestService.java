package pt.haslab.specassistant.services;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.Repairer;
import pt.haslab.alloy4fun.data.request.YearRange;
import pt.haslab.alloyaddons.AlloyUtil;
import pt.haslab.alloyaddons.ParseUtil;
import pt.haslab.specassistant.data.models.*;
import pt.haslab.specassistant.data.transfer.Transition;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.repositories.TestRepository;
import pt.haslab.specassistant.services.policy.Probability;
import pt.haslab.specassistant.services.policy.Reward;
import pt.haslab.specassistant.util.FutureUtil;
import pt.haslab.specassistant.util.Text;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@ApplicationScoped
public class TestService {
    @Inject
    Logger log;

    @Inject
    ModelRepository modelRepo;

    @Inject
    HintExerciseRepository exerciseRepo;

    @Inject
    HintGenerator hintGenerator;

    @Inject
    TestRepository testRepo;

    @Inject
    GraphManager graphManager;

    @Inject
    GraphInjestor graphInjestor;

    @Inject
    PolicyManager policyManager;

    // TAR TESTS *******************************************************************************************
    private static final long TarTimeoutSeconds = 60;

    public CompletableFuture<Test.Data> doTarTest(CompModule world, HintExercise exercise) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<Func> repairTargets = AlloyUtil.getFuncsWithNames(world.getAllFunc().makeConstList(), exercise.targetFunctions);
            Command command = exercise.getValidCommand(world, exercise.cmd_n).orElseThrow();
            Repairer r = Repairer.make(world, command, repairTargets, 2);

            long t = System.nanoTime();
            boolean b = r.repair().isPresent();

            return new Test.Data(b, System.nanoTime() - t);
        }).completeOnTimeout(new Test.Data(false, (double) TarTimeoutSeconds), TarTimeoutSeconds, TimeUnit.SECONDS);
    }


    public CompletableFuture<Void> testChallengeWithTAR(String modelId, Predicate<Model> model_filter) {
        log.trace("Starting TAR test for challenge " + modelId);

        final String secrets = "\n" + Text.extractSecrets(modelRepo.findById(modelId).code);
        Repairer.opts.solver = A4Options.SatSolver.SAT4J;

        Map<String, HintExercise> exercises = exerciseRepo.findByModelIdAsCmdMap(modelId);

        return FutureUtil.runEachAsync(
                modelRepo.streamByOriginalAndUnSat(modelId)
                        .filter(x -> testRepo.findByIdOptional(new Test.ID(x.id, "TAR")).map(y -> !y.data.success() && y.data.time() > 60.0).orElse(true))
                        .filter(model_filter),
                m -> {
                    CompModule w;
                    try {
                        try {
                            w = ParseUtil.parseModel(m.code + "\n" + secrets);
                        } catch (Err e) {
                            if (Text.containsSecrets(m.code))
                                w = ParseUtil.parseModel(m.code);
                            else throw e;
                        }
                        HintExercise ex = exercises.get(m.cmd_n);
                        if (ex != null && ex.isValidCommand(w, m.cmd_i)) {
                            return this.doTarTest(w, ex)
                                    .thenApply(d -> {
                                        if (d.time() > 60.0)
                                            log.warn("YIKES " + d.time());
                                        return d;
                                    })
                                    .thenAccept(d -> testRepo.updateOrCreate(new Test.ID(m.id, "TAR"), exercises.get(m.cmd_n).graph_id, d));

                        }
                    } catch (Err e) {
                        log.error("Error while parsing model " + m.id + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return CompletableFuture.completedFuture(null);
                },
                FutureUtil.errorLog(log, "Failed to test a model")
        ).whenComplete(FutureUtil.logTrace(log, "Completed TAR test on challenge " + modelId));
    }

    public CompletableFuture<Void> testAllChallengesWithTAR(Predicate<Model> model_filter) {
        return FutureUtil.forEachOrderedAsync(exerciseRepo.getAllModelIds(), x -> this.testChallengeWithTAR(x, model_filter), FutureUtil.errorLog(log, "Failed to complete a model"))
                .whenComplete(FutureUtil.logTrace(log, "Finished stressing all models with TAR"));
    }

    // SPEC TESTS ******************************************************************************************
    public Test.Data specTestMutation(CompModule world, HintExercise exercise) {
        long time = System.nanoTime();
        Optional<HintNode> node = hintGenerator.mutatedNextState(exercise, world);
        time = System.nanoTime() - time;
        return new Test.Data(node.isPresent(), time, node.map(x -> x.hopDistance).orElse(null));
    }

    public Test.Data specTest(CompModule world, HintExercise exercise) {
        long time = System.nanoTime();
        Optional<Transition> t = hintGenerator.worldTransition(exercise, world);
        time = System.nanoTime() - time;
        return new Test.Data(t.isPresent(), time, t.map(x -> x.to.hopDistance).orElse(null), t.map(x -> x.action.editDistance).orElse(null));
    }

    public void specTestFull(Model m) {
        specTestFull(m, "");
    }

    public void specTestFull(Model m, String preffix) {
        CompModule world = ParseUtil.parseModel(m.code);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(m.original, m.cmd_n).orElse(null);

        if (exercise != null && exercise.isValidCommand(world, m.cmd_i)) {
            testRepo.updateOrCreate(new Test.ID(m.id, preffix + "SPEC"), exercise.graph_id, specTest(world, exercise));
            testRepo.updateOrCreate(new Test.ID(m.id, preffix + "SPEC_MUTATION"), exercise.graph_id, specTestMutation(world, exercise));
        }
    }

    public void specTestGraphPartition(Double ratio) {
        Map<String, Map<Boolean, List<Model>>> datasets = exerciseRepo.findAll().stream()
                .map(x -> x.model_id)
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(x -> x, x -> modelRepo.streamByDerivationOf(x).collect(Collectors.partitioningBy(nil -> Math.random() <= ratio, Collectors.toList()))));

        Map<String, List<Model>> testing = datasets.entrySet()
                .stream()
                .map(e -> {
                    List<String> training = e.getValue().get(true).stream().map(x -> x.id).toList();
                    String challenge = e.getKey();

                    if (training.isEmpty())
                        throw new RuntimeException("TRAINING IS EMPTY");
                    if (e.getValue().get(false).isEmpty())
                        throw new RuntimeException("TESTING IS EMPTY");

                    graphInjestor.parseModelTree(challenge, m -> !challenge.equals(m.derivationOf) || training.contains(m.id));

                    return Map.entry(challenge, e.getValue().get(false));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Reward r : Reward.values())
            for (Probability p : Probability.values()) {
                FutureUtil.inlineRuntime(FutureUtil.forEachAsync(HintGraph.findAll().stream().map(x -> (HintGraph) x), x -> policyManager.computePolicyForGraph(x.id, 0.99, r, p))
                        .thenCompose(nil ->
                                FutureUtil.forEachAsync(
                                        testing.entrySet().stream(),
                                        e -> {
                                            List<Model> batch = e.getValue();
                                            while (!batch.isEmpty()) {
                                                batch.forEach(m -> specTestFull(m, r + "_" + p + "."));
                                                batch = modelRepo.streamByDerivationOfInAndOriginal(batch.stream().map(x -> x.id).collect(Collectors.toSet()), e.getKey()).toList();
                                            }
                                        }
                                )
                        )
                );
            }
    }

    public CompletableFuture<Void> specTestChallenge(String challenge, Predicate<Model> model_filter) {
        log.trace("Starting SpecAssistant test for challenge " + challenge);

        return FutureUtil.forEachAsync(modelRepo.streamByOriginalAndUnSat(challenge).filter(model_filter), this::specTestFull)
                .whenComplete(FutureUtil.logTrace(log, "Completed spec SpecAssistant with challenge " + challenge));
    }

    public CompletableFuture<Void> testAllChallengesWithSpec(Predicate<Model> model_filter) {
        return FutureUtil.forEachOrderedAsync(exerciseRepo.getAllModelIds(), x -> this.specTestChallenge(x, model_filter));
    }

    public void deleteAllSpecTests() {
        testRepo.deleteTestsByType("SPEC");
        testRepo.deleteTestsByType("SPEC_MUTATION");
    }

    // AUTOSETUP *******************************************************************************************

    private static ObjectId getAGraphID(Map<String, ObjectId> graphspace, String prefix, String label) {
        if (!graphspace.containsKey(label))
            graphspace.put(label, HintGraph.newGraph(prefix + "-" + label).id);
        return graphspace.get(label);
    }

    public void makeGraphAndExercisesFromCommands(String prefix, List<String> model_ids) {
        Map<String, ObjectId> graphspace = new HashMap<>();
        model_ids.forEach(id -> graphManager.generateExercisesWithGraphIdFromSecrets(l -> getAGraphID(graphspace, prefix, l), id));
    }

    public CompletableFuture<Void> autoSetupJob(List<String> model_ids, String prefix, YearRange range) {
        AtomicLong start = new AtomicLong();
        return CompletableFuture
                .runAsync(() -> start.set(System.nanoTime()))
                .thenRun(() -> log.debug("Starting setup for " + prefix + " with model ids " + model_ids))
                .thenRun(() -> graphManager.deleteExerciseByModelIDs(model_ids, true))
                .thenRun(() -> makeGraphAndExercisesFromCommands(prefix, model_ids))
                .thenRun(() -> log.trace("Scanning models " + model_ids))
                .thenCompose(nil -> FutureUtil.allFutures(model_ids.stream().map(id -> graphInjestor.parseModelTree(id, x -> range.testDate(Text.parseDate(x.time))))))
                .thenRun(() -> log.trace("Computing policies for " + prefix))
                .thenRun(() -> graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> policyManager.computePolicyForGraph(id, 0.99, Reward.COST_TED, Probability.EDGE)))
                .thenRun(() -> log.debug("Completed setup for " + prefix + " with model ids " + model_ids + " in " + 1e-9 * (System.nanoTime() - start.get()) + " seconds"))
                .whenComplete(FutureUtil.log(log));
    }


    public void fixTestGraphIds() {
        FutureUtil.inlineRuntime(
                FutureUtil.forEachAsync(
                        testRepo.findAll().stream(),
                        x -> {
                            Model m = modelRepo.findById(x.id.model_id());
                            x.setGraphId(exerciseRepo.findByModelIdAndCmdN(m.original, m.cmd_n).orElseThrow().getGraph_id()).persistOrUpdate();
                        })
        );
    }

    public void computePoliciesForAll(Double discount, Reward eval, Probability prob) {
        FutureUtil.forEachAsync(HintGraph.findAll().stream().map(x -> (HintGraph) x), x -> policyManager.computePolicyForGraph(x.id, discount, eval, prob))
                .whenComplete(FutureUtil.logTrace(log, "Finished computing policies"));
    }

}
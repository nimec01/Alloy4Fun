package pt.haslab.specassistant.services;

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
import pt.haslab.specassistant.data.models.HintExercise;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.data.models.Model;
import pt.haslab.specassistant.data.models.Test;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.repositories.TestRepository;
import pt.haslab.specassistant.services.policy.ProbabilityEvaluation;
import pt.haslab.specassistant.services.policy.RewardEvaluation;
import pt.haslab.specassistant.util.FutureUtil;
import pt.haslab.specassistant.util.Text;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;


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
            boolean b = r.repair(TarTimeoutSeconds * 1000).isPresent();

            return new Test.Data(b, System.nanoTime() - t);
        }).completeOnTimeout(new Test.Data(false, (double) TarTimeoutSeconds), TarTimeoutSeconds, TimeUnit.SECONDS);
    }


    public CompletableFuture<Void> testChallengeWithTAR(String modelId, Predicate<LocalDateTime> year_tester) {
        log.trace("Starting TAR test for challenge " + modelId);

        final String secrets = "\n" + Text.extractSecrets(modelRepo.findById(modelId).code);
        Repairer.opts.solver = A4Options.SatSolver.SAT4J;

        Map<String, HintExercise> exercises = exerciseRepo.findByModelIdAsCmdMap(modelId);

        return FutureUtil.runEachAsync(
                modelRepo.streamByOriginalAndUnSat(modelId)
                        .filter(x -> testRepo.findByIdOptional(new Test.ID(x.id, "TAR")).isEmpty())
                        .filter(x -> year_tester.test(Text.parseDate(x.time))),
                m -> ParseUtil
                        .parseModelAsync(m.code + secrets)
                        .exceptionally(FutureUtil.errorLog(log, "Error while parsing model " + m.id + ": "))
                        .thenCompose(w -> {
                            if (w != null) {
                                HintExercise ex = exercises.get(m.cmd_n);
                                if (ex != null && ex.isValidCommand(w, m.cmd_i)) {
                                    return this.doTarTest(w, ex)
                                            .thenAccept(d -> testRepo.updateOrCreate(new Test.ID(m.id, "TAR"), exercises.get(m.cmd_n).graph_id, d));
                                }
                            }
                            return CompletableFuture.completedFuture(null);
                        })
                        .exceptionally(FutureUtil.errorLog(log, "Error while parsing model " + m.id + ": "))
        ).whenComplete(FutureUtil.logTrace(log, "Completed TAR test on challenge " + modelId));
    }

    public CompletableFuture<Void> testAllChallengesWithTAR(Predicate<LocalDateTime> year_tester) {
        return FutureUtil.forEachOrderedAsync(exerciseRepo.getAllModelIds(), x -> this.testChallengeWithTAR(x, year_tester))
                .whenComplete(FutureUtil.logTrace(log, "Finished stressing all models with TAR"));
    }

    // SPEC TESTS ******************************************************************************************
    public Test.Data specTestMutation(CompModule world, HintExercise exercise) {
        long time = System.nanoTime();
        boolean b = hintGenerator.hintWithMutation(exercise, world).isPresent();
        time = System.nanoTime() - time;
        Integer hintDistance = hintGenerator.mutatedNextState(exercise, world).map(x -> x.hopDistance).orElse(null);
        return new Test.Data(b, time, hintDistance);
    }

    public Test.Data specTest(CompModule world, HintExercise exercise) {
        long time = System.nanoTime();
        boolean b = hintGenerator.hintWithGraph(exercise, world).isPresent();
        time = System.nanoTime() - time;
        Integer hintDistance = hintGenerator.nextState(exercise, world).map(x -> x.hopDistance).orElse(null);
        return new Test.Data(b, time, hintDistance);
    }

    public void specTestFull(Model m) {
        CompModule world = ParseUtil.parseModel(m.code);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(m.original, m.cmd_n).orElse(null);

        if (exercise != null && exercise.isValidCommand(world, m.cmd_i)) {
            testRepo.updateOrCreate(new Test.ID(m.id, "SPEC"), exercise.graph_id, specTest(world, exercise));
            testRepo.updateOrCreate(new Test.ID(m.id, "SPEC_MUTATION"), exercise.graph_id, specTestMutation(world, exercise));
        }
    }

    public CompletableFuture<Void> specTestChallenge(String challenge, Predicate<LocalDateTime> year_tester) {
        log.trace("Starting SpecAssistant test for challenge " + challenge);

        return FutureUtil.forEachAsync(modelRepo.streamByOriginalAndUnSat(challenge).filter(x -> year_tester.test(Text.parseDate(x.time))), this::specTestFull)
                .whenComplete(FutureUtil.logTrace(log, "Completed spec SpecAssistant with challenge " + challenge));
    }

    public CompletableFuture<Void> testAllChallengesWithSpec(Predicate<LocalDateTime> year_tester) {
        return FutureUtil.forEachOrderedAsync(exerciseRepo.getAllModelIds(), x -> this.specTestChallenge(x, year_tester));
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

    public void makeGraphAndExercisesFromCommands(List<String> model_ids, String prefix) {
        Map<String, ObjectId> graphspace = new HashMap<>();
        model_ids.forEach(id -> graphManager.generateExercisesWithGraphIdFromSecrets(l -> getAGraphID(graphspace, prefix, l), id));
    }

    public CompletableFuture<Void> autoSetupJob(List<String> model_ids, String prefix, YearRange range) {
        AtomicLong start = new AtomicLong();
        return CompletableFuture
                .runAsync(() -> start.set(System.nanoTime()))
                .thenRun(() -> log.debug("Starting setup for " + prefix + " with model ids " + model_ids))
                .thenRun(() -> graphManager.deleteExerciseByModelIDs(model_ids, true))
                .thenRun(() -> makeGraphAndExercisesFromCommands(model_ids, prefix))
                .thenRun(() -> log.trace("Scanning models " + model_ids))
                .thenCompose(nil -> FutureUtil.allFutures(model_ids.stream().map(id -> graphInjestor.parseModelTree(id, range::testDate))))
                .thenRun(() -> log.trace("Computing policies for " + prefix))
                .thenRun(() -> graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> policyManager.computePolicyForGraph(id, 0.99, RewardEvaluation.TED, ProbabilityEvaluation.EDGE)))
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

    public void computePoliciesForAll(Double discount, RewardEvaluation eval, ProbabilityEvaluation prob) {
        FutureUtil.forEachAsync(HintGraph.findAll().stream().map(x -> (HintGraph) x), x -> policyManager.computePolicyForGraph(x.id, discount, eval, prob))
                .whenComplete(FutureUtil.logTrace(log,"Finished computing policies"));
    }
}
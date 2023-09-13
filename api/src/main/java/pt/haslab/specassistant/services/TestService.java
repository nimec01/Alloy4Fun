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
import pt.haslab.specassistant.data.models.Test;
import pt.haslab.specassistant.repositories.HintExerciseRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.repositories.TestRepository;
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
import java.util.stream.Collectors;


@ApplicationScoped
public class TestService {

    private static final Logger LOG = Logger.getLogger(TestService.class);

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
            boolean b = r.repair(TarTimeoutSeconds * 100).isPresent();

            return new Test.Data(b, System.nanoTime() - t);
        }).completeOnTimeout(new Test.Data(false, -1L), TarTimeoutSeconds, TimeUnit.SECONDS);
    }


    public CompletableFuture<Void> testChallengeWithTAR(String modelId) {
        final String secrets = "\n" + Text.extractSecrets(modelRepo.findById(modelId).code);

        LOG.debug("Starting TAR test for " + modelId);

        Map<String, HintExercise> exercises = exerciseRepo.findByModelIdAsCmdMap(modelId);

        return FutureUtil.runEachAsync(
                modelRepo.streamByOriginalAndUnSat(modelId),
                m -> ParseUtil
                        .parseModelAsync(m.code + secrets)
                        .thenCompose(world -> this.doTarTest(world, exercises.get(m.cmd_n)))
                        .thenAccept(d -> testRepo.updateOrCreate(new Test.ID(m.id, exercises.get(m.cmd_n).graph_id, "TAR"), d))
        ).whenComplete(FutureUtil.logDebug(LOG, "Completed TAR test of " + modelId));
    }

    public CompletableFuture<Void> testAllChallengesWithTAR() {
        return FutureUtil.allFutures(exerciseRepo.findAll().stream().map(x -> x.model_id).collect(Collectors.toSet()).stream().map(this::testChallengeWithTAR));
    }

    // SPEC TESTS ******************************************************************************************
    public Test.Data specTestMutation(CompModule world, HintExercise exercise) {
        long startTime = System.nanoTime();
        boolean b = hintGenerator.hintWithMutation(exercise, world).isPresent();
        return new Test.Data(b, System.nanoTime() - startTime);
    }

    public Test.Data specTestFull(CompModule world, HintExercise exercise) {
        long startTime = System.nanoTime();
        boolean b = hintGenerator.hintWithGraph(exercise, world).isPresent();
        return new Test.Data(b, System.nanoTime() - startTime);
    }

    public void specTestFull(String model_id, String code, String original, String cmd_n) {
        CompModule world = ParseUtil.parseModel(code);
        HintExercise exercise = exerciseRepo.findByModelIdAndCmdN(original, cmd_n).orElse(null);

        if (exercise != null) {
            testRepo.updateOrCreate(new Test.ID(model_id, exercise.graph_id, "SPEC"), specTestFull(world, exercise));
            testRepo.updateOrCreate(new Test.ID(model_id, exercise.graph_id, "SPEC_MUTATION"), specTestMutation(world, exercise));
        }
    }

    public CompletableFuture<Void> specTestModel(String modelId, Predicate<LocalDateTime> year_tester) {
        Repairer.opts.solver = A4Options.SatSolver.MiniSatProverJNI;
        final String secrets = "\n" + Text.extractSecrets(modelRepo.findById(modelId).code);

        return FutureUtil.forEachAsync(
                        modelRepo.streamByOriginalAndUnSat(modelId).filter(x -> year_tester.test(Text.parseDate(x.time))),
                        m -> this.specTestFull(m.id, m.code + secrets, m.original, m.cmd_n))
                .whenComplete(FutureUtil.logInfo(LOG, "Test Complete"));
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
                .thenRun(() -> LOG.debug("Starting setup for " + prefix + " with model ids " + model_ids))
                .thenRun(() -> graphManager.deleteExerciseByModelIDs(model_ids, true))
                .thenRun(() -> makeGraphAndExercisesFromCommands(model_ids, prefix))
                .thenRun(() -> LOG.trace("Scanning models"))
                .thenCompose(nil -> FutureUtil.allFutures(model_ids.stream().map(id -> graphInjestor.parseModelTree(id, range::testDate))))
                .thenRun(() -> LOG.trace("Computing policies"))
                .thenRun(() -> graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> policyManager.computePolicyAndDebloatGraph(id)))
                .thenRun(() -> LOG.debug("Completed setup after " + 1e-9 * (System.nanoTime() - start.get()) + " seconds"))
                .whenComplete(FutureUtil.log(LOG));
    }


}
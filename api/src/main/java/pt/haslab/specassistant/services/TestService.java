package pt.haslab.specassistant.services;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.Repairer;
import pt.haslab.alloyaddons.AlloyUtil;
import pt.haslab.alloyaddons.ParseUtil;
import pt.haslab.specassistant.data.aggregation.EntityStringLong;
import pt.haslab.specassistant.data.aggregation.IdListLong;
import pt.haslab.specassistant.data.aggregation.Transition;
import pt.haslab.specassistant.data.models.*;
import pt.haslab.specassistant.data.policy.PolicyOption;
import pt.haslab.specassistant.repositories.ChallengeRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.repositories.TestRepository;
import pt.haslab.specassistant.util.DataUtil;
import pt.haslab.specassistant.util.FutureUtil;
import pt.haslab.specassistant.util.Text;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
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
    ChallengeRepository challengeRepo;

    @Inject
    HintGenerator hintGenerator;

    @Inject
    TestRepository testRepo;

    @Inject
    GraphManager graphManager;

    @Inject
    GraphIngestor graphIngestor;

    @Inject
    PolicyManager policyManager;

    // TAR TESTS *******************************************************************************************
    private static final long TarTimeoutSeconds = 60;

    public CompletableFuture<Test.Data> doTarTest(CompModule world, Challenge exercise) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<Func> repairTargets = AlloyUtil.getFuncsWithNames(world.getAllFunc().makeConstList(), exercise.getTargetFunctions());
            Command command = exercise.getValidCommand(world, exercise.getCmd_n()).orElseThrow();
            Repairer r = Repairer.make(world, command, repairTargets, 2);

            long t = System.nanoTime();
            boolean b = r.repair().isPresent();

            return new Test.Data(b, System.nanoTime() - t);
        }).completeOnTimeout(new Test.Data(false, (double) TarTimeoutSeconds), TarTimeoutSeconds, TimeUnit.SECONDS);
    }


    public CompletableFuture<Void> testChallengeWithTAR(String modelId, Predicate<Model> model_filter) {
        log.trace("Starting TAR test for challenge " + modelId);

        final String secrets = "\n" + Text.extractSecrets(modelRepo.findById(modelId).getCode());
        Repairer.opts.solver = A4Options.SatSolver.SAT4J;

        Map<String, Challenge> exercises = challengeRepo.findByModelIdAsCmdMap(modelId);

        return FutureUtil.runEachAsync(modelRepo.streamByOriginalAndUnSat(modelId).filter(x -> testRepo.findByIdOptional(new Test.ID(x.getId(), "TAR")).map(y -> !y.getData().success() && y.getData().time() > 60.0).orElse(true)).filter(model_filter), m -> {
            CompModule w;
            try {
                try {
                    w = ParseUtil.parseModel(m.getCode() + "\n" + secrets);
                } catch (Err e) {
                    if (Text.containsSecrets(m.getCode())) w = ParseUtil.parseModel(m.getCode());
                    else throw e;
                }
                Challenge ex = exercises.get(m.getCmd_n());
                if (ex != null && ex.isValidCommand(w, m.getCmd_i())) {
                    return this.doTarTest(w, ex).thenApply(d -> {
                        if (d.time() > 60.0) log.warn("YIKES " + d.time());
                        return d;
                    }).thenAccept(d -> testRepo.updateOrCreate(new Test.ID(m.getId(), "TAR"), exercises.get(m.getCmd_n()).getGraph_id(), d));

                }
            } catch (Err e) {
                log.error("Error while parsing model " + m.getId() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return CompletableFuture.completedFuture(null);
        }, FutureUtil.errorLog(log, "Failed to test a model")).whenComplete(FutureUtil.logTrace(log, "Completed TAR test on challenge " + modelId));
    }

    public CompletableFuture<Void> testAllChallengesWithTAR(Predicate<Model> model_filter) {
        return FutureUtil.forEachOrderedAsync(challengeRepo.getAllModelIds(), x -> this.testChallengeWithTAR(x, model_filter), FutureUtil.errorLog(log, "Failed to complete a model")).whenComplete(FutureUtil.logTrace(log, "Finished stressing all models with TAR"));
    }

    // SPEC TESTS ******************************************************************************************
    public Test.Data specTestMutation(CompModule world, Challenge exercise) {
        long time = System.nanoTime();
        Optional<Node> node = hintGenerator.mutatedNextState(exercise, world);
        time = System.nanoTime() - time;
        return new Test.Data(node.isPresent(), time, node.map(Node::getHopDistance).orElse(null));
    }

    public Test.Data specTest(CompModule world, Challenge exercise) {
        long time = System.nanoTime();
        Optional<Transition> t = hintGenerator.worldTransition(exercise, world);
        time = System.nanoTime() - time;
        return new Test.Data(t.isPresent(), time, t.map(x -> x.getTo().getHopDistance()).orElse(null), t.orElse(null), t.map(x -> x.getTo().getComplexity() - x.getFrom().getComplexity()).orElse(null));
    }


    public void specTestMutation(Model m) {
        CompModule world = ParseUtil.parseModel(m.getCode());
        Challenge exercise = challengeRepo.findByModelIdAndCmdN(m.getOriginal(), m.getCmd_n()).orElse(null);

        if (exercise != null && exercise.isValidCommand(world, m.getCmd_i())) {
            testRepo.updateOrCreate(new Test.ID(m.getId(), "SPEC_MUTATION"), exercise.getGraph_id(), specTestMutation(world, exercise));
        }
    }

    public void specTest(Model m, String preffix) {
        CompModule world = ParseUtil.parseModel(m.getCode());
        Challenge exercise = challengeRepo.findByModelIdAndCmdN(m.getOriginal(), m.getCmd_n()).orElse(null);

        if (exercise != null && exercise.isValidCommand(world, m.getCmd_i())) {
            testRepo.updateOrCreate(new Test.ID(m.getId(), preffix + ".SPEC"), exercise.getGraph_id(), specTest(world, exercise));
        }
    }

    public void remakeGraphManagement(Map<String, List<String>> map) {
        graphManager.dropEverything();
        map.forEach(this::makeGraphAndChallengesFromCommands);
        fixTestGraphIds();
    }


    public void specTestDefaultPolicies(Map<String, List<String>> map) {
        graphManager.dropEverything();
        testRepo.deleteTestsByNotType("TAR");
        map.forEach(this::makeGraphAndChallengesFromCommands);
        fixTestGraphIds();
        testal();
    }
    @SneakyThrows
    private void testal() {
        Map<String, Set<String>> mToC = challengeRepo.findAll().stream().collect(Collectors.groupingBy(Challenge::getModel_id, Collectors.mapping(Challenge::getCmd_n, Collectors.toSet())));

        Log.trace("Splittig dataset");

        Map<String, Map<String, Set<String>>> mToSubToC = mToC.entrySet().stream().parallel().map(this::doPartitions).collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        Log.trace("Splittig dataset phase 2");

        ConcurrentMap<String, List<Model>> test = mToSubToC
                .entrySet()
                .stream()
                .parallel()
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, x -> x.getValue().entrySet().stream().flatMap(y -> modelRepo.streamSubTreesByIdInAndChallengeInAndCmd(x.getKey(), y.getKey(), y.getValue())).toList()));

        Map<String, Set<String>> train = test.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().stream().map(Model::getId).collect(Collectors.toSet())));

        Log.trace("Training all");

        FutureUtil.forEachOrderedAsync(train.entrySet(), e -> graphIngestor.parseModelTree(e.getKey(), m -> !e.getValue().contains(m.getId()))).get();

        Log.trace("Testing all");

        PolicyOption.samples.forEach(option -> {
            Log.trace("Doing policy " + option);

            Graph.findAll().project(Graph.class).stream().forEach(x -> policyManager.computePolicyForGraph(x.getId(), option));

            test.values().stream().flatMap(Collection::stream).parallel().forEach(m -> specTest(m, option.getRule().toString()));
        });
    }

    private Map.Entry<String, Map<String, Set<String>>> doPartitions(Map.Entry<String, Set<String>> e) {
        Map<String, Map<String, Long>> root_counts = modelRepo.countSubTreeByDerivationOfAndCMDN(e.getKey(), e.getValue())
                .collect(Collectors.toMap(IdListLong::getId, x -> x.getList().stream().collect(Collectors.toMap(EntityStringLong::getId, EntityStringLong::getL))));

        Map<String, Long> integerMap = e.getValue().stream().collect(Collectors.toMap(x -> x, x -> 0L));
        root_counts.forEach((m, l) -> l.forEach((c, n) -> integerMap.put(c, integerMap.get(c) + n)));
        DataUtil.mapValues(integerMap, l -> (long) (0.3 * l));

        Map<String, Set<String>> r = root_counts.keySet().stream().collect(Collectors.toMap(x -> x, x -> new HashSet<>()));
        List<Map.Entry<String, Map<String, Long>>> shuffle = new ArrayList<>(root_counts.entrySet());
        Collections.shuffle(shuffle);

        shuffle.forEach(e1 -> {
            String root = e.getKey();
            e1.getValue().forEach((c, l) -> {
                if (l > 0 && integerMap.get(c) > 0) {
                    integerMap.put(c, integerMap.get(c) - l);
                    r.get(root).add(c);
                }
            });
        });
        return Map.entry(e.getKey(), r);
    }

    // AUTOSETUP *******************************************************************************************

    private static ObjectId getAGraphID(Map<String, ObjectId> graphspace, String prefix, String label) {
        if (!graphspace.containsKey(label)) graphspace.put(label, Graph.newGraph(prefix + "-" + label).id);
        return graphspace.get(label);
    }

    public void makeGraphAndChallengesFromCommands(String prefix, List<String> model_ids) {
        Map<String, ObjectId> graphspace = new HashMap<>();
        model_ids.forEach(id -> graphManager.generateExercisesWithGraphIdFromSecrets(l -> getAGraphID(graphspace, prefix, l), id));
    }

    public CompletableFuture<Void> autoSetupJob(List<String> model_ids, String prefix, Predicate<Model> model_filter) {
        AtomicLong start = new AtomicLong();
        return CompletableFuture
                .runAsync(() -> start.set(System.nanoTime()))
                .thenRun(() -> log.debug("Starting setup for " + prefix + " with model ids " + model_ids))
                .thenRun(() -> graphManager.deleteExerciseByModelIDs(model_ids, true))
                .thenRun(() -> makeGraphAndChallengesFromCommands(prefix, model_ids))
                .thenRun(() -> log.trace("Scanning models " + model_ids))
                .thenCompose(nil -> FutureUtil.allFutures(model_ids.stream().map(id -> graphIngestor.parseModelTree(id, model_filter))))
                .thenRun(() -> log.trace("Computing policies for " + prefix))
                .thenRun(() -> graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> policyManager.computePolicyForGraph(id, PolicyOption.samples.get(0))))
                .thenRun(() -> log.debug("Completed setup for " + prefix + " with model ids " + model_ids + " in " + 1e-9 * (System.nanoTime() - start.get()) + " seconds"))
                .whenComplete(FutureUtil.log(log));
    }

    @SneakyThrows
    public void fixTestGraphIds() {
        FutureUtil.forEachAsync(testRepo.findAll().stream(), x -> {
            Model m = modelRepo.findById(x.getId().model_id());
            x.setGraphId(challengeRepo.findByModelIdAndCmdN(m.getOriginal(), m.getCmd_n()).orElseThrow().getGraph_id()).persistOrUpdate();
        }).get();
    }

    public void computePoliciesForAll(PolicyOption eval) {
        FutureUtil.forEachAsync(Graph.findAll().stream().map(x -> (Graph) x), x -> policyManager.computePolicyForGraph(x.id, eval)).whenComplete(FutureUtil.logTrace(log, "Finished computing policies"));
    }

}
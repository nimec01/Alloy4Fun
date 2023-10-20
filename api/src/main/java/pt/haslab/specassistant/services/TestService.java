package pt.haslab.specassistant.services;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.Repairer;
import pt.haslab.alloyaddons.AlloyUtil;
import pt.haslab.alloyaddons.ParseUtil;
import pt.haslab.specassistant.data.aggregation.EntityStringLong;
import pt.haslab.specassistant.data.aggregation.Transition;
import pt.haslab.specassistant.data.models.*;
import pt.haslab.specassistant.data.policy.PolicyRule;
import pt.haslab.specassistant.data.policy.VarRule;
import pt.haslab.specassistant.repositories.ChallengeRepository;
import pt.haslab.specassistant.repositories.ModelRepository;
import pt.haslab.specassistant.repositories.TestRepository;
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
        return new Test.Data(t.isPresent(), time, t.map(x -> x.getTo().getHopDistance()).orElse(null), t.map(x -> x.getEdge().getEditDistance()).orElse(null), t.map(x -> x.getTo().getComplexity() - x.getFrom().getComplexity()).orElse(null));
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
        Graph.findAll().project(Graph.class).stream().forEach(x -> trainAndTestGraph(x.getId(), 0.3, List.of(PolicyRule.oneMinusPrefTimesCostPlusOld(VarRule.Name.TED, VarRule.Name.ARRIVALS))));
    }

    private void trainAndTestGraph(ObjectId graph_id, Double ratio, Collection<PolicyRule> policies) {
        ConcurrentMap<Challenge, Set<String>> testing_juice = challengeRepo.streamByGraphId(graph_id).parallel().collect(Collectors.toConcurrentMap(x -> x, c -> trainAndGetTestChallengeData(c, ratio)));

        policies.forEach(policy -> {
            policyManager.computePolicyForGraph(graph_id, policy);

            testing_juice.entrySet().stream().parallel().forEach(e -> {
                modelRepo.streamSubTreesByIdInAndChallengeInAndCommandIn(e.getKey().getModel_id(), e.getKey().getCmd_n(), e.getValue())
                        .parallel()
                        .forEach(x -> specTest(x, policy.toString()));
            });
        });
    }

    private Set<String> trainAndGetTestChallengeData(Challenge challenge, Double ratio) {
        long target = modelRepo.countByOriginalAndCmdNAndValid(challenge.getModel_id(), challenge.getCmd_n());
        target *= ratio;

        Set<String> roots = new HashSet<>();
        Set<String> checked = new HashSet<>();
        long count = 0L;
        while (count < target) {
            EntityStringLong sample = modelRepo.sampleTreeByDerivationOfAndCMDNAndIdNotIn(challenge.getModel_id(), challenge.getCmd_n(), checked).toList().get(0);
            if (sample.getL() > 0) {
                roots.add(sample.getId());
                count += sample.getL();
            }
            checked.add(sample.getId());
        }

        graphIngestor.parseModelTree(challenge.getModel_id(), x -> !checked.contains(x.getId()), List.of(challenge));

        return roots;
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
                .thenRun(() -> graphManager.getModelGraphs(model_ids.get(0)).forEach(id -> policyManager.computePolicyForGraph(id, PolicyRule.oneMinusPrefTimesCostPlusOld(VarRule.Name.TED, VarRule.Name.ARRIVALS))))
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

    public void computePoliciesForAll(PolicyRule eval) {
        FutureUtil.forEachAsync(Graph.findAll().stream().map(x -> (Graph) x), x -> policyManager.computePolicyForGraph(x.id, eval)).whenComplete(FutureUtil.logTrace(log, "Finished computing policies"));
    }

}
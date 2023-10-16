package pt.haslab.specassistant.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;
import pt.haslab.specassistant.services.policy.Probability;
import pt.haslab.specassistant.services.policy.Reward;
import pt.haslab.specassistant.util.FutureUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@ApplicationScoped
public class PolicyManager {

    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;

    public void computePolicyForGraph(ObjectId graph_id, Double policyDiscount, Reward reward, Probability probability) {
        HintGraph.removeAllPolicyStats(graph_id);
        clearPolicy(graph_id);
        long t = System.nanoTime();
        policy_iteration(graph_id, policyDiscount, reward, probability);
        computePolicyHopDistance(graph_id);
        HintGraph.registerPolicy(graph_id, System.nanoTime() - t, nodeRepo.getTotalVisitsFromScoredGraph(graph_id));
        //Cleanup
        disableUnreachebleHops(graph_id);
    }

    private void clearPolicy(ObjectId graph_id) {
        nodeRepo.clearPolicy(graph_id);
        edgeRepo.clearPolicyFromGraph(graph_id);
    }

    public void policy_iteration(ObjectId graph_id, Double gamma, Reward r, Probability p) {
        boolean unstable = true;
        FutureUtil.inlineRuntime(randoomPolicy(graph_id));
        nodeRepo.setScoresOnGraph(graph_id, 0.0);
        nodeRepo.setScoresOnValidGraphNodes(graph_id, 1.0);
        while (unstable) {
            policy_evaluation_heap_eager(graph_id, gamma, r, p);
            unstable = policy_improvement(graph_id, gamma, r, p);
        }
    }

    public void policy_evaluation(ObjectId graph_id, Double gamma, Reward r, Probability p) {
        AtomicReference<Double> delta = new AtomicReference<>(Double.POSITIVE_INFINITY);
        do {
            delta.set(0.0);
            FutureUtil.inlineRuntime(FutureUtil.forEachAsync(
                    nodeRepo.streamByGraphIdAndInvalid(graph_id), n -> {
                        double v = n.score;
                        n.score = edgeRepo.streamByOriginAndPolicy(n.id)
                                .map(e -> bellman(gamma, r, p, e, n, nodeRepo.findById(e.destination)))
                                .reduce(0.0, Double::sum);
                        delta.updateAndGet(x -> Double.max(x, Math.abs(v - n.score)));
                        n.persistOrUpdate();
                    }
            ));
        } while (delta.get() > r.getRequiredPrecision());
    }

    // A LOT FASTER SINCE IT PULLS EVERYTHIONG FROM THE DATABASE AT THE START, 3 queries instead of iter*n^2*e
    public void policy_evaluation_heap_eager(ObjectId graph_id, Double gamma, Reward r, Probability p) {
        AtomicReference<Double> delta = new AtomicReference<>();
        Map<ObjectId, HintNode> nodes = nodeRepo.mapByGraphId(graph_id);
        Map<ObjectId, HintEdge> policy = edgeRepo.streamGraphPolicy(graph_id).collect(Collectors.toMap(x -> x.origin, x -> x));
        do {
            delta.set(0.0);
            FutureUtil.inlineRuntime(FutureUtil.forEachAsync(
                    List.copyOf(nodes.values()).stream(), n -> {
                        double v = n.score;
                        HintEdge e = policy.get(n.id);
                        n.score = e == null ? 0 : bellman(gamma, r, p, e, n, nodes.get(e.destination));
                        delta.updateAndGet(x -> Double.max(x, Math.abs(v - n.score)));
                    }
            ));
        } while (delta.get() > r.getRequiredPrecision());
        nodeRepo.persistOrUpdate(nodes.values());
    }

    public void policy_evaluation_mongo_pipeline(ObjectId graph_id, Double gamma, Reward r, Probability p) {
        do {
            nodeRepo.unsetDelta(graph_id);
            nodeRepo.policyImprovemenentInnerLoop(graph_id, gamma, r, p);
        } while (nodeRepo.getHightestDelta(graph_id).map(x -> x.delta > r.getRequiredPrecision()).orElse(false));
    }

    public CompletableFuture<Void> randoomPolicy(ObjectId graph_id) {
        clearPolicy(graph_id);
        return FutureUtil.forEachAsync(nodeRepo.streamByGraphIdAndInvalid(graph_id), n -> {
            List<HintEdge> list = edgeRepo.streamByOrigin(n.id).toList();
            try {
                edgeRepo.setAsPolicy(list.get((int) (Math.random() * list.size())).id);
            } catch (IndexOutOfBoundsException ignored) {
            }
        });
    }

    public static double bellman(Double gamma, Reward r, Probability p, HintEdge action, HintNode origin, HintNode destination) {
        return p.apply(origin, action) * (r.apply(origin, action) + gamma * destination.score);
    }

    public Boolean policy_improvement(ObjectId graph_id, Double gamma, Reward r, Probability p) {
        AtomicBoolean improved = new AtomicBoolean(false);
        nodeRepo.streamByGraphIdAndInvalid(graph_id).forEach(
                n -> {
                    List<HintEdge> actions = edgeRepo.streamByOrigin(n.id).toList();
                    if (!actions.isEmpty()) {
                        try {

                        HintEdge old_ = actions.stream().filter(HintEdge::getPolicy).findFirst().orElseThrow();
                        HintEdge new_ = actions.stream().max(Comparator.comparingDouble(x -> bellman(gamma, r, p, x, n, nodeRepo.findById(x.destination)))).orElseThrow();

                        if (!Objects.equals(old_.id, new_.id)) {
                            edgeRepo.removeFromPolicy(old_.id);
                            edgeRepo.setAsPolicy(new_.id);
                            improved.set(true);
                        }
                        }catch (NoSuchElementException e){
                            throw e;
                        }
                    }
                }
        );
        return improved.get();
    }

    public void computePolicyHopDistance(ObjectId graph_id) {
        int distance = 0;
        nodeRepo.unsetHopDistanceInGraph(graph_id);
        List<ObjectId> node_batch = nodeRepo.streamByGraphIdAndValid(graph_id).map(x -> x.id).toList();
        while (!node_batch.isEmpty()) {
            nodeRepo.setHopDistanceInNodes(node_batch, distance++);
            List<ObjectId> list = edgeRepo.streamByDestinationInAndPolicy(node_batch).map(x -> x.origin).toList();
            node_batch = nodeRepo.streamByIdInAndHopNull(list).map(x -> x.id).toList();
        }
    }

    public void disableUnreachebleHops(ObjectId graph_id) {
        edgeRepo.clearPolicyFromOrigins(nodeRepo.streamByGraphIdAndHopNull(graph_id).map(x -> x.id).toList());
        nodeRepo.unsetPolicyByGraphIdAndHopNull(graph_id);
    }


    public void deleteUnreachebleHops(ObjectId graph_id) {
        edgeRepo.deleteByOriginIn(nodeRepo.streamByGraphIdAndHopNull(graph_id).map(x -> x.id).toList());
        nodeRepo.deleteByGraphIdAndHopNull(graph_id);
    }
}
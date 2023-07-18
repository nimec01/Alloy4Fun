package pt.haslab.specassistant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintGraph;
import pt.haslab.specassistant.data.models.HintNode;
import pt.haslab.specassistant.repositories.HintEdgeRepository;
import pt.haslab.specassistant.repositories.HintNodeRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class PolicyManager {

    private static final Logger LOG = Logger.getLogger(PolicyManager.class);
    @Inject
    HintNodeRepository nodeRepo;
    @Inject
    HintEdgeRepository edgeRepo;


    public void computePolicyForGraph(ObjectId graph_id) {
        HintGraph.removeAllPolicyStats(graph_id);
        long t = System.nanoTime();
        Collection<PolicyContext> batch = nodeRepo.streamByGraphIdAndValidTrue(graph_id).map(PolicyContext::init).toList();

        while (!batch.isEmpty()) {
            PolicyContext targetScore = Collections.min(batch);

            Map<Boolean, List<PolicyContext>> targetIds = batch.stream().collect(Collectors.partitioningBy(x -> x.compareTo(targetScore) <= 0));

            try {
                List<CompletableFuture<List<PolicyContext>>> actionPool = targetIds.get(true)
                        .stream()
                        .peek(PolicyContext::assignScore)
                        .map(x -> CompletableFuture.supplyAsync(() ->
                                edgeRepo.streamByDestinationNodeIdAndAllScoreGT(x.nodeId(), x.cost)
                                        .map(y -> x.scoreEdgeOrigin(y, nodeRepo::findById)).filter(Objects::nonNull).toList())).toList();

                CompletableFuture.allOf(actionPool.toArray(CompletableFuture[]::new)).get();

                List<List<PolicyContext>> result = new ArrayList<>();
                result.add(targetIds.get(false));
                for (CompletableFuture<List<PolicyContext>> l : actionPool) {
                    result.add(l.get());
                }

                batch = List.copyOf(result.stream().flatMap(Collection::stream).collect(Collectors.toMap(PolicyContext::nodeId, x -> x, PolicyContext::bestScored)).values());

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
            }
        }
        HintGraph.registerPolicyCalculationTime(graph_id, System.nanoTime() - t);
    }


    public static class PolicyContext implements Comparable<PolicyContext> {
        public HintNode node;

        public Double cost;
        public int distance;

        public static PolicyContext init(HintNode n) {
            PolicyContext result = new PolicyContext();
            result.node = n;
            result.cost = 0.0;
            result.distance = 0;
            return result;
        }

        public PolicyContext bestScored(PolicyContext policyContext) {
            if (this.cost < policyContext.cost) return this;
            return policyContext;
        }

        public PolicyContext scoreEdgeOrigin(HintEdge edge, Function<ObjectId, HintNode> nodeGetter) {
            PolicyContext result = new PolicyContext();

            result.node = nodeGetter.apply(edge.origin);

            double prob;
            //origin_node.leaves == 0 means that every edge of the node is an addition by an algorithm (ex: a mutation)
            //In this case, the probability of it being traversed will be 0
            if (result.node.leaves != 0)
                prob = (double) edge.count / (double) result.node.leaves;
            else
                prob = 0.0;

            edge.hopDistance = result.distance = distance + 1;

            edge.score = result.cost = edge.computeImminentCost() + prob * this.cost;

            edge.update();

            return result;
        }

        @Override
        public int compareTo(PolicyContext o) {
            return this.cost.compareTo(o.cost);
        }

        @Override
        public String toString() {
            return "{cost=%s, distance=%d}".formatted(cost, distance);
        }

        public ObjectId nodeId() {
            return node.id;
        }

        public void assignScore() {
            this.node.score = this.cost;
            this.node.persistOrUpdate();
        }
    }


}

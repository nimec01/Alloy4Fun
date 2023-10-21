package pt.haslab.specassistant.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.aggregation.Transition;
import pt.haslab.specassistant.data.models.Edge;
import pt.haslab.specassistant.data.models.Graph;
import pt.haslab.specassistant.data.models.Node;
import pt.haslab.specassistant.data.policy.PolicyOption;
import pt.haslab.specassistant.data.policy.PolicyRule;
import pt.haslab.specassistant.repositories.EdgeRepository;
import pt.haslab.specassistant.repositories.NodeRepository;
import pt.haslab.specassistant.util.Ordered;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class PolicyManager {

    @Inject
    NodeRepository nodeRepo;
    @Inject
    EdgeRepository edgeRepo;

    public void computePolicyForGraph(ObjectId graph_id, PolicyOption policyOption) {
        Graph.removeAllPolicyStats(graph_id);
        clearPolicy(graph_id);
        policyOption.getRule().normalizeByGraph(graph_id);
        long t = System.nanoTime();
        djisktra(graph_id, policyOption);
        Graph.registerPolicy(graph_id, System.nanoTime() - t, nodeRepo.getTotalVisitsFromScoredGraph(graph_id));

    }

    private void clearPolicy(ObjectId graph_id) {
        nodeRepo.clearPolicy(graph_id);
        edgeRepo.clearPolicyFromGraph(graph_id);
    }

    public void djisktra(ObjectId graph_id, PolicyOption policyOption) {
        Collection<DjisktraNode> batch = nodeRepo.streamByGraphIdAndValid(graph_id).map(n -> DjisktraNode.builder().node(n).score(policyOption.getIdentity()).build()).toList();
        while (!batch.isEmpty()) {
            Double min = batch.stream().map(x -> x.score).min(Double::compare).orElseThrow();
            Map<Boolean, List<DjisktraNode>> nodes = batch.stream().collect(Collectors.groupingBy(x -> x.score <= min, Collectors.toList()));
            ConcurrentMap<ObjectId, DjisktraNode> map = nodes.getOrDefault(false, List.of()).stream().collect(Collectors.toConcurrentMap(x -> x.node.id, x -> x));
            batch = nodes.get(true)
                    .stream().parallel()
                    .peek(DjisktraNode::save)
                    .flatMap(n -> edgeRepo.streamTransitionsByDestinationScoreGT(n.node, min).parallel().map(t -> n.apply(t, policyOption.getRule())))
                    .collect(Collectors.toConcurrentMap(x -> x.node.id, x -> x, policyOption.getObjective() == PolicyOption.Objective.MIN ? Ordered::min : Ordered::max, () -> map)).values();
        }
    }

    @Data
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    static class DjisktraNode implements Ordered<DjisktraNode> {
        Node node;
        Edge parent_edge = null;
        Double score = 0.0;
        int distance = 0;

        public void save() {
            node.setScore(score);
            node.setHopDistance(distance);
            node.update();
            if (parent_edge != null) {
                parent_edge.setPolicy(true);
                parent_edge.update();
            }
        }

        public DjisktraNode apply(Transition t, PolicyRule r) {
            return new DjisktraNode(t.getFrom(), t.getEdge(), r.apply(t), distance + 1);
        }

        @Override
        public int compareTo(DjisktraNode o) {
            return Double.compare(score, o.score);
        }
    }


}
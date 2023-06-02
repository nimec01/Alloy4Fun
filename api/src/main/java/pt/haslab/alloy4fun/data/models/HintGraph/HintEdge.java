package pt.haslab.alloy4fun.data.models.HintGraph;


import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.util.Objects;
import java.util.Set;

@MongoEntity(collection = "HintEdge")
public class HintEdge extends PanacheMongoEntity {

    public Set<Endpoint> endpoints;

    public Double score;

    public static HintEdge createEmpty(String originNodeId, String destinationNodeId) {
        HintEdge result = new HintEdge();
        result.endpoints = Set.of(Endpoint.createEmpty(originNodeId), Endpoint.createEmpty(destinationNodeId));
        return result;
    }

    private Endpoint findEndpoint(String node_id) {
        return endpoints.stream().filter(endpoint -> endpoint.node_id.equals(node_id)).findFirst().orElse(null);
    }

    public HintEdge directedIncrement(String destination) {
        findEndpoint(destination).count++;
        return this;
    }

    public String getDifferentNodeId(String node_id) {
        return Objects.requireNonNull(endpoints.stream().filter(endpoint -> !endpoint.node_id.equals(node_id)).findFirst().orElse(null)).node_id;
    }

    public static class Endpoint {
        public String node_id;
        public int count;

        public Double score;

        public Endpoint() {
        }

        public Endpoint(String node_id, int count, Double score) {
            this.node_id = node_id;
            this.count = count;
            this.score = score;
        }

        public static Endpoint createEmpty(String node_id) {
            return new Endpoint(node_id, 0, null);
        }
    }
}

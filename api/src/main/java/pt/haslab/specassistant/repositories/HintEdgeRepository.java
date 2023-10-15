package pt.haslab.specassistant.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.HintEdge;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintEdgeRepository implements PanacheMongoRepository<HintEdge> {

    public Optional<HintEdge> findByOriginAndDestination(ObjectId origin, ObjectId destination) {
        return find(new Document("origin", origin).append("destination", destination)).firstResultOptional();
    }

    public synchronized HintEdge incrementOrCreate(ObjectId graph_id, ObjectId origin, ObjectId destination) {
        HintEdge edge = findByOriginAndDestination(origin, destination).orElseGet(() -> HintEdge.createEmpty(graph_id, origin, destination)).visit();
        edge.persistOrUpdate();
        return edge;
    }

    public Optional<HintEdge> policyByOriginNode(ObjectId origin_id) {
        return find(new Document("origin", origin_id).append("policy", true)).firstResultOptional();
    }

    public Stream<HintEdge> streamByOriginAndPolicy(ObjectId destination) {
        return find(new Document("origin", destination).append("policy", true)).stream();
    }

    public Stream<HintEdge> streamGraphPolicy(ObjectId graph_id) {
        return find(new Document("graph_id", graph_id).append("policy", true)).stream();
    }

    public Stream<HintEdge> streamByOrigin(ObjectId origin) {
        return find(new Document("origin", origin)).stream();
    }

    public void deleteByGraphId(ObjectId graph_id) {
        delete(new Document("graph_id", graph_id));
    }

    public void deleteByOriginIn(Collection<ObjectId> origin) {
        delete(new Document("origin", new Document("$in", origin)));
    }

    public Stream<HintEdge> streamByGraphId(ObjectId graphId) {
        return find(new Document("graph_id", graphId)).stream();
    }

    public void clearPolicyFromGraph(ObjectId graph_id) {
        update(new Document("$unset", new Document("policy", null))).where("graph_id", graph_id);
    }

    public void setAsPolicy(ObjectId id) {
        update(new Document("$set", new Document("policy", true))).where("_id", id);
    }

    public void removeFromPolicy(ObjectId id) {
        update(new Document("$unset", new Document("policy", null))).where("_id", id);
    }

    public Stream<HintEdge> streamByDestinationInAndPolicy(List<ObjectId> destinations) {
        return find(new Document("destination", new Document("$in", destinations)).append("policy", true)).stream();
    }


    public void clearPolicyFromOrigins(List<ObjectId> origins) {
        update(new Document("$unset", new Document("policy", null))).where(new Document("origin", new Document("$in", origins)));
    }

    public void clearPolicyFromDestinations(List<ObjectId> origins) {
        update(new Document("$unset", new Document("policy", null))).where(new Document("origin", new Document("$in", origins)));
    }
}

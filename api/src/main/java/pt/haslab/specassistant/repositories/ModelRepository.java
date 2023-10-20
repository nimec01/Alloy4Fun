package pt.haslab.specassistant.repositories;

import io.quarkus.logging.Log;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import pt.haslab.specassistant.data.aggregation.EntityStringLong;
import pt.haslab.specassistant.data.models.Model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ModelRepository implements PanacheMongoRepositoryBase<Model, String> {

    public Stream<Model> streamByDerivationOfAndOriginal(String derivationOf, String original) {
        return find("derivationOf = ?1 and original = ?2", derivationOf, original).stream();
    }

    public Stream<Model> streamByOriginalAndUnSat(String original_id) {
        return ByOriginalAndUnSat(original_id).stream();
    }

    public String getOriginalById(String model_id) {
        return findById(model_id).getOriginal();
    }

    public PanacheQuery<Model> ByOriginalAndUnSat(String original_id) {
        return find(new Document("original", original_id).append("sat", 1));
    }


    public Long countByOriginalAndCmdNAndValid(String original_id, String cmd_n) {
        return count(new Document("original", original_id).append("cmd_n", cmd_n).append("sat", new Document("$in", List.of(0, 1))));
    }

    public Stream<EntityStringLong> sampleTreeByDerivationOfAndCMDNAndIdNotIn(String challengeId, String cmd_n, Collection<String> blacklist) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("derivationOf", challengeId).append("$or", List.of(new Document("_id", new Document("$nin", blacklist)), new Document("_id", challengeId)))),
                new Document("$sample", new Document("size", 1)),
                new Document("$addFields", new Document("root", List.of("$$ROOT"))),
                new Document("$graphLookup", new Document("from", "Model").append("startWith", "$_id").append("connectFromField", "_id").append("connectToField", "derivationOf").append("as", "subnodes").append("restrictSearchWithMatch", new Document("original", challengeId))),
                new Document("$project", new Document("result", new Document("$concatArrays", List.of("$root", "$subnodes")))),
                new Document("$unwind", "$result"),
                new Document("$group", new Document("_id", "$_id").append("l", new Document("$sum", new Document("$cond", List.of(new Document("$eq", List.of("$result.cmd_n", cmd_n)), 1, 0)))))
        );
        return StreamSupport.stream(mongoCollection().aggregate(pipeline, EntityStringLong.class).allowDiskUse(true).spliterator(), false);
    }

    public Stream<Model> streamSubTreesByIdInAndChallengeInAndCommandIn(String challengeId, String cmd_n, Collection<String> roots) {
        return StreamSupport.stream(mongoCollection().aggregate(List.of(
                new Document("$match", new Document("original", challengeId).append("_id", new Document("$in", roots))),
                new Document("$addFields", new Document("root", List.of("$$ROOT"))),
                new Document("$graphLookup", new Document("from", "Model").append("startWith", "$_id").append("connectFromField", "_id").append("connectToField", "derivationOf").append("as", "subnodes").append("restrictSearchWithMatch", new Document("original", challengeId))),
                new Document("$project", new Document("result", new Document("$concatArrays", List.of("$root", "$subnodes")))),
                new Document("$unwind", "$result"),
                new Document("$replaceRoot", new Document("newRoot", "$result")),
                new Document("$match", new Document("cmd_n", cmd_n).append("sat", new Document("$gte", 0)))
        ), Model.class).allowDiskUse(true).spliterator(), false);
    }

}
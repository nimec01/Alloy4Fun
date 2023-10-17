package pt.haslab.specassistant.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.Model;
import pt.haslab.specassistant.data.transfer.EntityStringLong;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ModelRepository implements PanacheMongoRepositoryBase<Model, String> {

    public Stream<Model> streamByDerivationOfAndOriginal(String derivationOf, String original) {
        return find("derivationOf = ?1 and original = ?2", derivationOf, original).stream();
    }

    public Stream<Model> streamByDerivationOfInAndOriginal(Collection<String> derivationOf, String original) {
        return find(new Document("derivationOf", new Document("$in", derivationOf)).append("original", original)).stream();
    }


    public Stream<Model> streamByDerivationOf(String derivationOf) {
        return find(new Document("derivationOf", derivationOf)).stream();
    }

    public Stream<Model> streamByOriginalAndUnSat(String original_id) {
        return ByOriginalAndUnSat(original_id).stream();
    }

    public List<Model> getPageByOriginalAndUnSat(String original_id, Page page) {
        return ByOriginalAndUnSat(original_id).page(page).list();
    }

    public String getOriginalById(String model_id) {
        return findById(model_id).original;
    }

    public PanacheQuery<Model> ByOriginalAndUnSat(String original_id) {
        return find(new Document("original", original_id).append("sat", 1));
    }

    public Stream<Model> streamByOriginalAndIdIn(ObjectId original_id, Collection<Model> ids) {
        return find(new Document("original", original_id).append("_id", new Document("$in", ids))).stream();
    }

    public Long countByChallengeAndValid(String original_id) {
        return count(new Document("original", original_id).append("sat", new Document("$in", List.of(0, 1))));
    }

    public Long countByDerivationOf(String original_id) {
        return count(new Document("original", original_id).append("sat", new Document("$in", List.of(0, 1))));
    }

    public Stream<Model> sampleByAndDerivationOf(long size, String challengeId) {
        return StreamSupport.stream(mongoCollection().aggregate(List.of(
                new Document("$match", new Document("derivationOf", challengeId)),
                new Document("$sample", new Document("$size", size))
        ), Model.class).spliterator(), false);
    }

    public Stream<Model> sampleByAnd(long size, String challengeId) {
        return StreamSupport.stream(mongoCollection().aggregate(List.of(
                new Document("$match", new Document("derivationOf", challengeId)),
                new Document("$sample", new Document("$size", size))
        ), Model.class).spliterator(), false);
    }

    public Stream<EntityStringLong> countSubTreeSizesByValidDerivationOfChallenge(String challengeId) {
        return StreamSupport.stream(mongoCollection().aggregate(List.of(
                new Document("$match", new Document("derivationOf", challengeId).append("_id", new Document("$ne", challengeId))),
                new Document("$graphLookup",
                        new Document("from", "Model").append("startWith", "$_id").append("connectFromField", "_id").append("connectToField", "derivationOf").append("as", "subnodes").append("restrictSearchWithMatch", new Document("original", challengeId).append("sat", new Document("$gte", 0)))
                ),
                new Document("$project", new Document("s", "$_id").append("l", new Document("$size", "$subnodes")))
        ), EntityStringLong.class).allowDiskUse(true).spliterator(), false);
    }

    public Stream<Model> streamSubTreesByIdInAndChallengeIn(Collection<String> roots, Collection<String> challengeId) {
        return StreamSupport.stream(mongoCollection().aggregate(List.of(
                new Document("$match", new Document("original", new Document("$in", challengeId)).append("_id", new Document("$in", roots))),
                new Document("$addFields", new Document("root", List.of("$$ROOT"))),
                new Document("$graphLookup",
                        new Document("from", "Model")
                                .append("startWith", "$_id")
                                .append("connectFromField", "_id")
                                .append("connectToField", "derivationOf")
                                .append("as", "subnodes")
                                .append("restrictSearchWithMatch", new Document("original", new Document("$in", challengeId)).append("sat", 1))
                ),
                new Document("$project", new Document("result", new Document("$concatArrays", List.of("$root", "$subnodes")))),
                new Document("$unwind", "$result"),
                new Document("$replaceRoot", new Document("newRoot", "$result"))
        ), Model.class).allowDiskUse(true).spliterator(), false);
    }

    public Stream<Model> sampleSubTreeRoots(Double ratio, String challenge_id) {
        long c = countByDerivationOf(challenge_id);
        return sampleByAndDerivationOf((long) (c * ratio), challenge_id);
    }

    public Stream<Model> sampleSubTreeRootsBySize(Double ratio, String challenge_id) {
        List<EntityStringLong> ms = countSubTreeSizesByValidDerivationOfChallenge(challenge_id).collect(Collectors.toList());
        Collections.shuffle(ms);
        long cuttoff = (long) (ratio * ms.stream().map(x -> x.l).reduce(0L, Long::sum));
        int i, t = 0;
        for (i = 0; t < cuttoff && i < ms.size(); i++) {
            t += ms.get(i).l;
        }
        return streamByIdIn(ms.subList(0, i).stream().map(x -> x.s).toList());
    }

    public Stream<Model> streamByIdIn(List<String> list) {
        return find(new Document("_id", new Document("$in", list))).stream();
    }
}
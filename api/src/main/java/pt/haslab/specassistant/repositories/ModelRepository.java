package pt.haslab.specassistant.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import pt.haslab.specassistant.data.models.Model;

import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class ModelRepository implements PanacheMongoRepositoryBase<Model, String> {

    public Stream<Model> streamByDerivationOfAndOriginal(String derivationOf, String original) {
        return find("derivationOf = ?1 and original = ?2", derivationOf, original).stream();
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

    private PanacheQuery<Model> ByOriginalAndUnSat(String original_id) {
        return find(new Document("original", original_id).append("sat", 1));
    }

}

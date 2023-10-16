package pt.haslab.specassistant.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.Test;

@ApplicationScoped
public class TestRepository implements PanacheMongoRepositoryBase<Test, Test.ID> {

    public Test findOrCreate(Test.ID id) {
        return findByIdOptional(id).orElseGet(() -> new Test(id));
    }

    public void updateOrCreate(Test.ID id, ObjectId graph_id, Test.Data data) {
        findOrCreate(id).setData(data).setGraphId(graph_id).persistOrUpdate();
    }

    public void deleteTestsByType(String type) {
        delete(new Document("_id.type", type));
    }

    public void deleteTestsByNotType(String type) {
        delete(new Document("_id.type", new Document("$neq", type)));
    }
}

package pt.haslab.specassistant.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.models.Test;

@ApplicationScoped
public class TestRepository implements PanacheMongoRepositoryBase<Test, String> {

    public Test findOrCreate(String model_id, ObjectId graph_id) {
        return findByIdOptional(model_id).orElseGet(() -> new Test(model_id, graph_id));
    }

    public synchronized void register(String id, ObjectId graph_id, String type, Test.Data data) {
        Test t = findOrCreate(id, graph_id);
            t.graph_id = graph_id;
        t.register(type, data);
        t.persistOrUpdate();
    }
}

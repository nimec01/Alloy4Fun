package pt.haslab.alloy4fun.repositories;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import pt.haslab.alloy4fun.data.models.HintGraph.HintExercise;

import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class HintExerciseRepository implements PanacheMongoRepository<HintExercise> {

    public Stream<HintExercise> streamByModelId(String model_id) {
        return find("model_id = ?1", model_id).stream();
    }

    public Optional<HintExercise> findByModelIdAndCmdN(String model_id, String cmd_n) {
        return find("model_id = ?1 and cmd_n = ?2", model_id, cmd_n).firstResultOptional();
    }
}

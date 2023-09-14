package pt.haslab.specassistant.data.models;

import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import pt.haslab.alloyaddons.AlloyUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@MongoEntity(collection = "HintExercise")
public class HintExercise extends PanacheMongoEntity {
    public String model_id;

    public ObjectId graph_id;

    //Indicates the number of secret commands introduced by the model
    //Allows the program to filter commands with repeated names under normal conditions
    //(i.e., the secret commands are always last in the getAllCommands method list)
    public Integer end_offset;

    public String cmd_n;

    public Set<String> targetFunctions;


    public HintExercise() {
    }

    public ObjectId getGraph_id() {
        return graph_id;
    }

    public HintExercise(String model_id, ObjectId graph_id, Integer end_offset, String cmd_n, Set<String> targetFunctions) {
        this.model_id = model_id;
        this.graph_id = graph_id;
        this.end_offset = end_offset;
        this.cmd_n = cmd_n;
        this.targetFunctions = targetFunctions;
    }

    /**
     * Tests if the command index is contained wothing the last "end_offset" defined comands
     * (meteor currently places secrets as the last defined predicates)
     *
     * @param world Alloy Module
     * @param index Command index
     * @return True if valid, False otherwise
     */
    public boolean isValidCommand(CompModule world, Integer index) {
        return index >= world.getAllCommands().size() - end_offset;
    }

    public Optional<Command> getValidCommand(CompModule world, String label) {
        List<Command> l = List.copyOf(world.getAllCommands());
        int from = Integer.max(0, l.size() - end_offset);

        return AlloyUtil.getCommand(l.subList(from, l.size()), label);
    }


}
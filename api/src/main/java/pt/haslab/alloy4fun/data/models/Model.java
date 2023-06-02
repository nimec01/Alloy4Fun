package pt.haslab.alloy4fun.data.models;


import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

@MongoEntity(collection = "Model")
public class Model extends PanacheMongoEntityBase {

    @BsonId
    public String id;

    /**
     * the complete code of the model.
     */
    public String code;

    /**
     * which model does it derive from (null if original).
     */
    public String derivationOf;

    /**
     * The root of the derivation tree. Different from derivation, as this is
     * the original model and remains the same after derivation to preserve
     * the original secrets. It Should only change when a model with secrets is
     * shared (i.e., sharing public versions of a model with secrets should
     * not break the derivation).
     */
    public String original;

    /**
     * optional field for the index of the executed command, if created by
     * execution.
     */
    public Integer cmd_i;

    /**
     * optional field for the name of the executed command, if created by
     * execution.
     */
    public String cmd_n;

    /**
     * optional field, whether the command was a check (1) or a run (0), if
     * created by execution.
     */
    public Boolean cmd_c;

    /**
     * optional field, whether the command was satisfiable (1) or unsatisfiable
     * (0), if created by execution. if execution fails, then -1.
     */
    public Integer sat;

    /**
     * optional field, a possible error or warning message.
     */
    public String msg;

    /**
     * the theme associated with this model.
     */
    //public Theme theme;

    /**
     * the timestamp.
     */
    public String time;

    public Model() {
    }

    @BsonIgnore
    public boolean isOriginal_() {
        return id.equals(original);
    }

    @BsonIgnore
    public boolean isRoot() {
        return derivationOf == null;
    }

}

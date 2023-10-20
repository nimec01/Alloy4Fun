package pt.haslab.specassistant.data.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.aggregation.Transition;

import java.util.function.Function;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@NoArgsConstructor
@JsonSubTypes({
        @JsonSubTypes.Type(value = VarRule.class, name = "var"),
        @JsonSubTypes.Type(value = BinaryRule.class, name = "operation"),
        @JsonSubTypes.Type(value = ConstantRule.class, name = "number")
})
public abstract class PolicyRule implements Function<Transition, Double> {

    public abstract void normalizeByGraph(ObjectId objectId);

    public static PolicyRule oneMinusPrefTimesCostPlusOld(VarRule.Name cost, VarRule.Name pref) {
        return BinaryRule.binaryOld("+", new BinaryRule("*", VarRule.of(cost), new BinaryRule("-", new ConstantRule(1.0), VarRule.of(pref))));
    }


}

package pt.haslab.specassistant.data.policy;


import lombok.*;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.aggregation.Transition;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BinaryRule extends PolicyRule {
    String operator;
    PolicyRule left, right;

    @Override
    public void normalizeByGraph(ObjectId objectId) {
        left.normalizeByGraph(objectId);
        right.normalizeByGraph(objectId);
    }

    @Override
    public Double apply(Transition transition) {
        return switch (operator) {
            case "*" -> left.apply(transition) * right.apply(transition);
            case "/" -> left.apply(transition) / right.apply(transition);
            case "+" -> left.apply(transition) + right.apply(transition);
            case "-" -> left.apply(transition) - right.apply(transition);
            default -> throw new UnkownOperationException("Unkown operation " + operator);
        };
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }


    public static PolicyRule binaryOld(String operator, PolicyRule p) {
        return new BinaryRule(operator, p, VarRule.of(VarRule.Name.OLD));
    }

    public static PolicyRule sumOld(PolicyRule p) {
        return new BinaryRule("+", p, VarRule.of(VarRule.Name.OLD));
    }

    public static PolicyRule sumOld(VarRule.Name p) {
        return new BinaryRule("+", VarRule.of(p), VarRule.of(VarRule.Name.OLD));
    }
}

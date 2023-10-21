package pt.haslab.specassistant.data.policy;


import lombok.*;
import org.bson.types.ObjectId;
import pt.haslab.specassistant.data.aggregation.Transition;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Binary extends PolicyRule {
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

    public static Binary mult(PolicyRule left, PolicyRule right) {
        return new Binary("*", left, right);
    }

    public static Binary div(PolicyRule left, PolicyRule right) {
        return new Binary("/", left, right);
    }

    public static Binary sum(PolicyRule left, PolicyRule right) {
        return new Binary("+", left, right);
    }

    public static Binary sub(PolicyRule left, PolicyRule right) {
        return new Binary("-", left, right);
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }


    public static PolicyRule scale(Double scalar, PolicyRule p) {
        return Binary.mult(new Constant(scalar), p);
    }

    public static PolicyRule sumOld(PolicyRule p) {
        return new Binary("+", p, Var.old());
    }
}

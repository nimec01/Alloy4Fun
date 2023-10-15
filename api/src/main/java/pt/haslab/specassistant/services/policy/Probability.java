package pt.haslab.specassistant.services.policy;

import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;

import java.util.function.BiFunction;

public enum Probability implements BiFunction<HintNode, HintEdge, Double> {
    NONE, EDGE,
    ;


    public Double apply(HintNode state, HintEdge action) {
        return switch (this) {
            case NONE -> 1.0;
            case EDGE -> (double) action.count / (double) state.leaves;
        };
    }

    public String jsApply(String state_field, String action_field) {
        return switch (this) {
            case NONE -> "0";
            case EDGE -> action_field + ".count/" + state_field + ".leaves";
        };
    }
}

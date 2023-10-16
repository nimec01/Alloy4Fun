package pt.haslab.specassistant.services.policy;

import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;

import java.util.function.BiFunction;

public enum Reward implements BiFunction<HintNode, HintEdge, Double> {
    COST_TED,
    COST_COMPLEXITY,
    COST_ONE,
    NONE,
    REWARD_ONE,
    REWARD_LEAVES,
    REWARD_VISITS;

    public Double apply(HintNode state, HintEdge action) {

        return (double) switch (this) {
            case NONE -> 0.0;
            case REWARD_ONE -> 1.0;
            case COST_ONE -> -1;
            case COST_TED -> -action.editDistance;
            case REWARD_VISITS -> state.visits;
            case REWARD_LEAVES -> state.leaves;
            case COST_COMPLEXITY -> -state.complexity;
        };
    }

    public String jsApply(String state_field, String action_field) {
        return switch (this) {
            case NONE -> "0";
            case REWARD_ONE -> "1";
            case COST_ONE -> "-1";
            case COST_TED -> "-" + action_field + ".editDistance";
            case REWARD_VISITS -> state_field + ".visits";
            case REWARD_LEAVES -> state_field + ".leaves";
            case COST_COMPLEXITY -> "-" + state_field + ".complexity";
        };
    }

    public Double getRequiredPrecision() {
        return switch (this) {
            case NONE, REWARD_ONE, COST_ONE -> 1e-4;
            case COST_TED, REWARD_VISITS, REWARD_LEAVES, COST_COMPLEXITY -> 0.1;
        };
    }

}

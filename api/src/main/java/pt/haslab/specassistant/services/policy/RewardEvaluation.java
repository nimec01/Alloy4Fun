package pt.haslab.specassistant.services.policy;

import pt.haslab.specassistant.data.models.HintEdge;
import pt.haslab.specassistant.data.models.HintNode;

import java.util.function.BiFunction;

public enum RewardEvaluation implements BiFunction<HintNode, HintEdge, Double> {
    NONE,
    ONE,
    TED,
    HOPS,
    VISITS,
    LEAVES,
    COMPLEXITY;

    public Double apply(HintNode state, HintEdge action) {
        return (double) switch (this) {
            case NONE -> 0.0;
            case ONE, HOPS -> 1.0;
            case TED -> action.editDistance;
            case VISITS -> state.visits;
            case LEAVES -> state.leaves;
            case COMPLEXITY -> state.complexity;
        };
    }

}

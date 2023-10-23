package pt.haslab.specassistant.data.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PolicyOption {
    PolicyRule rule;
    Double identity;
    Objective objective;

    public enum Objective {
        MAX, MIN
    }

    public static PolicyOption minimize(PolicyRule r) {
        return new PolicyOption(r, 0.0, Objective.MIN);
    }

    public static PolicyOption maxPercentage(PolicyRule r) {
        return new PolicyOption(r, 1.0, Objective.MAX);
    }

    public static final Map<String,PolicyOption> samples = Map.of(
            "TEDxArrival",minimize(Binary.oneMinusPrefTimesCost(Var.arrivals(), Var.ted())),
            "TEDCOMPxArrival",minimize(Binary.oneMinusPrefTimesCost(Var.arrivals(), Binary.sum(Binary.scale(0.7, Var.ted()), Binary.scale(0.3, Var.complexity())))),
            "TED",minimize(Binary.sumOld(Var.ted())),
            "COMP",minimize(Binary.sumOld(Var.complexity())),
            "ONE",minimize(Binary.sumOld(Constant.of(1.0))),
            "TEDCOMP",minimize(Binary.sumOld(Binary.sum(Binary.scale(0.7, Var.ted()), Binary.scale(0.3, Var.complexity())))),
            "Arrival",maxPercentage(Binary.mult(Var.arrivals(), Var.old())),
            "Departure",maxPercentage(Binary.mult(Var.departures(), Var.old()))
    );

}

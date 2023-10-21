package pt.haslab.specassistant.data.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    public static final List<PolicyOption> samples = List.of(
            minimize(PolicyRule.costPrefMix(Var.arrivals(), Var.ted())),
            minimize(PolicyRule.costPrefMix(Var.arrivals(), Binary.sum(Binary.scale(0.7, Var.ted()), Binary.scale(0.3, Var.complexity())))),
            minimize(Binary.sumOld(Var.ted())),
            minimize(Binary.sumOld(Var.complexity())),
            minimize(Binary.sumOld(Constant.of(1.0))),
            minimize(Binary.sumOld(Binary.sum(Binary.scale(0.7, Var.ted()), Binary.scale(0.3, Var.complexity())))),
            maxPercentage(Binary.mult(Var.arrivals(), Var.old())),
            maxPercentage(Binary.mult(Var.arrivals(), Var.old()))

    );

}

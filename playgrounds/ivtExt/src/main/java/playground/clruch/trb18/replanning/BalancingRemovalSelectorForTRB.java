package playground.clruch.trb18.replanning;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.sebhoerl.avtaxi.framework.AVModule;

/**
 * Makes sure that the number of available plans for an agent are balanced.
 *
 * Assuming 4 plans in the repository, there should always be 2 AV and 2 others be kept.
 * This way we can measure the "unobserved" travel statistics in comparison.
 *
 * WE PROBABLY DON'T NEED THIS! NEW IDEA! (Plan repositories only contain 2 plans ...)
 */
public class BalancingRemovalSelectorForTRB implements PlanSelector<Plan, Person> {
    private boolean isAVPlan(Plan plan) {
        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Leg) {
                if (((Leg) element).getMode().equals(AVModule.AV_MODE)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Plan selectPlan(HasPlansAndId<Plan, Person> agent) {
        double worstAVScore = Double.NEGATIVE_INFINITY;
        double worstNonAVScore = Double.NEGATIVE_INFINITY;

        Plan worstAVPlan = null;
        Plan worstNonAVPlan = null;

        Set<Plan> avPlans = new HashSet<>();
        Set<Plan> nonAVPlans = new HashSet<>();

        for (Plan plan : agent.getPlans()) {
            if (plan.getScore() != null) {
                if (isAVPlan(plan)) {
                    if (plan.getScore() < worstAVScore) {
                        worstAVScore = plan.getScore();
                        worstAVPlan = plan;
                    }

                    avPlans.add(plan);
                } else {
                    if (plan.getScore() < worstNonAVScore) {
                        worstNonAVScore = plan.getScore();
                        worstNonAVPlan = plan;
                    }

                    nonAVPlans.add(plan);
                }
            }
        }

        if (worstAVPlan == null && worstNonAVPlan == null) {
            throw new RuntimeException("Only null score plans... problem!");
        }

        if (worstAVPlan == null) {
            return worstNonAVPlan;
        }

        if (worstNonAVPlan == null) {
            return worstAVPlan;
        }

        if (avPlans.size() == nonAVPlans.size()) {
            return (worstAVScore < worstNonAVScore) ? worstAVPlan : worstNonAVPlan;
        }

        if (avPlans.size() > nonAVPlans.size()) {
            return worstAVPlan;
        } else {
            return worstNonAVPlan;
        }
    }
}

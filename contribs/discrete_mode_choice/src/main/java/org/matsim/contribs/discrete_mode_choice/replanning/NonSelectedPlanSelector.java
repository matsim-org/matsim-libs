package org.matsim.contribs.discrete_mode_choice.replanning;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * This is a plan selector for replanning that always selects a plan that is
 * *not* selected currently. This is especially useful when keeping only one
 * plan in an agent's memory but replanning is frequently. This way always the
 * new replanned version will be kept.
 * 
 * @author sebhoerl
 */
public class NonSelectedPlanSelector implements PlanSelector<Plan, Person> {
	static public final String NAME = "NonSelectedPlanSelector";

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> member) {
		if (member.getPlans().size() > 2) {
			throw new IllegalStateException(
					"NonSelectedPlanSelector only makes sense if there is no more than two plans!");
		}

		for (Plan plan : member.getPlans()) {
			if (!plan.equals(member.getSelectedPlan())) {
				return plan;
			}
		}
		return null;
	}
}

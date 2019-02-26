package org.matsim.contrib.carsharing.relocation.replanning;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 */
public class RelocationPlanSelector implements PlanSelector<Plan, Person>
{

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		return person.getSelectedPlan();
	}

}

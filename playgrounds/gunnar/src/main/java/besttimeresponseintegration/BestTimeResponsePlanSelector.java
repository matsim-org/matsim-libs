package besttimeresponseintegration;

import static org.matsim.core.gbl.MatsimRandom.getRandom;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
class BestTimeResponsePlanSelector implements PlanSelector<Plan, Person> {

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		/*
		 * Uniform random plan selection. This probably already exists
		 * somewhere.
		 */
		if ((person.getPlans() == null) || (person.getPlans().size() == 0)) {
			return null;
		} else {
			return person.getPlans().get(getRandom().nextInt(person.getPlans().size()));
		}
	}
}

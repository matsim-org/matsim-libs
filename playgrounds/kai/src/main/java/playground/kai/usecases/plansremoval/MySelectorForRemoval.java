package playground.kai.usecases.plansremoval;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

public class MySelectorForRemoval implements PlanSelector<Plan, Person> {

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> member) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented");
	}

}

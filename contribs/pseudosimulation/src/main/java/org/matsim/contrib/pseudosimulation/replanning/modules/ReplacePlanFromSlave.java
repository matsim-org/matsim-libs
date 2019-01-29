package org.matsim.contrib.pseudosimulation.replanning.modules;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

import java.util.HashMap;

public class ReplacePlanFromSlave implements PlanStrategy {

	private final HashMap<String, Plan> plans;

	public ReplacePlanFromSlave(HashMap<String, Plan> plans) {
		this.plans=plans;
	}

	@Override
	public void run(HasPlansAndId<Plan,Person> person) {
//		leave removing of plans to the plans remover. Thsi was initially so that the master always only had one plan in memory
//		however, this doesn't have to be the case for all scenarios.
//		person.removePlan(person.getSelectedPlan());
		Plan plan = plans.get(person.getId().toString());
		person.addPlan(plan);	
		person.setSelectedPlan(plan);

	}

	@Override
	public void init(ReplanningContext replanningContext) {

	}

	@Override
	public void finish() {

	}

}

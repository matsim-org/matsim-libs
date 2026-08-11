package org.matsim.contrib.pseudosimulation.replanning.selectors;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

class DistributedPlanSelectorTest {

	@Test
	void extractsThePersonPlanSelectorFromARegisteredStrategy() {
		PlanSelector<Plan, Person> expected = member -> member.getSelectedPlan();
		PlanStrategy strategy = new PlanStrategyImpl.Builder(expected).build();

		PlanSelector<Plan, Person> actual = DistributedPlanSelector.planSelectorOf(strategy);

		assertSame(expected, actual);
	}

	@Test
	void selectedPlanIsReturnedAndCapturedWhenSelectionAlwaysRuns() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		Person person = population.getFactory().createPerson(org.matsim.api.core.v01.Id.createPersonId("person"));
		Plan plan = population.getFactory().createPlan();
		person.addPlan(plan);
		PlanCatcher catcher = new PlanCatcher();
		catcher.init();
		PlanSelector<Plan, Person> delegate = ignored -> plan;
		DistributedPlanSelector selector = new DistributedPlanSelector(delegate, catcher, 1.0);

		Plan selected = selector.selectPlan(person);

		assertSame(plan, selected);
		assertSame(plan, catcher.getPlansForPSim().iterator().next());
	}
}

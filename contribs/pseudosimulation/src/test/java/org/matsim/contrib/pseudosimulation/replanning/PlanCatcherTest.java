package org.matsim.contrib.pseudosimulation.replanning;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.plans.PlanGenome;
import org.matsim.core.population.PopulationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanCatcherTest {

	@Test
	void accessBeforeInitializationThrowsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new PlanCatcher().getPlansForPSim());
	}

	@Test
	void addingPlansLazilyInitializesAndReplacesByPersonId() {
		PlanCatcher catcher = new PlanCatcher();
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
		Plan first = new PlanGenome(person);
		Plan replacement = new PlanGenome(person);

		catcher.addPlansForPsim(first);
		catcher.addPlansForPsim(replacement);

		assertEquals(1, catcher.getPlansForPSim().size());
		assertSame(replacement, catcher.getPlansForPSim().iterator().next());
	}

	@Test
	void togglesTheExactPlanInstanceAndInitClearsAllPlans() {
		PlanCatcher catcher = new PlanCatcher();
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
		Plan plan = new PlanGenome(person);

		catcher.removeExistingPlanOrAddNewPlan(plan);
		assertEquals(1, catcher.getPlansForPSim().size());
		catcher.removeExistingPlanOrAddNewPlan(plan);
		assertEquals(0, catcher.getPlansForPSim().size());
		catcher.removeExistingPlanOrAddNewPlan(plan);
		catcher.init();
		assertEquals(0, catcher.getPlansForPSim().size());
	}
}

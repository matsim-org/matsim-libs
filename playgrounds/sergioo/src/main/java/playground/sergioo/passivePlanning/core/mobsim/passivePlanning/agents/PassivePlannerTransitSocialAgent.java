package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents;

import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning.api.population.BasePerson;
import playground.sergioo.passivePlanning.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerTransitSocialAgent extends PassivePlannerTransitAgent  {

	//Constructors
	public PassivePlannerTransitSocialAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, final IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		super(basePerson, simulation, passivePlannerManager);
		planner = new SinglePlannerSocialAgent((ScenarioSimplerNetwork) simulation.getScenario(), household, leastCostPathCalculator, basePerson.getBasePlan());
	}

}

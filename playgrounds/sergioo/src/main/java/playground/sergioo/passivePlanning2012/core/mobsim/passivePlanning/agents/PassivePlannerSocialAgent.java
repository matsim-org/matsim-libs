package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerSocialAgent extends PassivePlannerAgent  {

	//Constructors
	public PassivePlannerSocialAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, final IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		super(basePerson, simulation, passivePlannerManager);
		planner = new SinglePlannerSocialAgent((ScenarioSimplerNetwork) simulation.getScenario(), household, leastCostPathCalculator, basePerson.getBasePlan());
	}

}

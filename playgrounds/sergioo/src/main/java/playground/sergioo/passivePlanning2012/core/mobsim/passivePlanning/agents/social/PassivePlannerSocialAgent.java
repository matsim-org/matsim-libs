package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerSocialAgent extends PassivePlannerDriverAgent  {

	//Constructors
	public PassivePlannerSocialAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, Set<String> modes) {
		super(basePerson, simulation, passivePlannerManager);
		boolean carAvailability = false;
		Collection<String> mainModes = simulation.getScenario().getConfig().qsim().getMainModes();
		for(PlanElement planElement:basePerson.getBasePlan().getPlanElements())
			if(planElement instanceof Leg)
				if(mainModes.contains(((Leg)planElement).getMode()))
					carAvailability = true;
		planner = new SinglePlannerSocialAgent((ScenarioSimplerNetwork) simulation.getScenario(), carAvailability, household, modes, this);
	}
	
	//Methods
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		((SinglePlannerSocialAgent)planner).shareKnownPlace(prevAct.getFacilityId(), prevAct.getStartTime(), prevAct.getType());
		super.endActivityAndComputeNextState(now);
	}
	
}

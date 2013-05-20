package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.TripRouter;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.SocialDecisionMaker;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerSocialAgent extends PassivePlannerDriverAgent  {

	//Constructors
	public PassivePlannerSocialAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, Set<String> modes) {
		super(basePerson, simulation, passivePlannerManager);
		boolean carAvailability = false;
		Collection<String> mainModes = simulation.getScenario().getConfig().getQSimConfigGroup().getMainMode();
		for(PlanElement planElement:basePerson.getBasePlan().getPlanElements())
			if(planElement instanceof Leg)
				if(mainModes.contains(((Leg)planElement).getMode()))
					carAvailability = true;
		planner = new SinglePlannerSocialAgent((ScenarioSimplerNetwork) simulation.getScenario(), carAvailability, household, basePerson.getBasePlan(), modes, this);
		planner.setPlanElementIndex(0);
	}
	
	//Methods
	@Override
	public void endActivityAndComputeNextState(double now) {
		super.endActivityAndComputeNextState(now);
		Activity prevAct = (Activity)getCurrentPlanElement();
		for(SocialDecisionMaker socialDecisionMaker:((SinglePlannerSocialAgent)planner).getSocialDecisionMaker().getKnownPeople())
			socialDecisionMaker.addKnownPlace(prevAct.getFacilityId(), prevAct.getStartTime(), prevAct.getType());
	}
	
}

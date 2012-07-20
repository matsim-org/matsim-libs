package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning.api.population.EmptyActivity;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.SinglePlannerAgentImpl;
import playground.sergioo.passivePlanning.core.population.decisionMakers.SocialDecisionMaker;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.DecisionMaker;
import playground.sergioo.passivePlanning.core.scenario.ScenarioSimplerNetwork;

public class SinglePlannerSocialAgent extends SinglePlannerAgentImpl {

	//Constructors
	public SinglePlannerSocialAgent(ScenarioSimplerNetwork scenario, Household household, IntermodalLeastCostPathCalculator leastCostPathCalculator, Plan plan) {
		super(new DecisionMaker[]{new SocialDecisionMaker(scenario, household, leastCostPathCalculator)}, plan);
	}

	//Methods
	@Override
	public Tuple<Leg, Activity> getLegActivity() {
		SocialDecisionMaker socialDecisionMaker = ((SocialDecisionMaker)decisionMakers[0]);
		Activity activity = new ActivityImpl(socialDecisionMaker.decideTypeOfActivity(), socialDecisionMaker.decideFacility().getLinkId());
		Leg leg = new LegImpl("car");
		socialDecisionMaker.setLeg(leg);
		socialDecisionMaker.decideModeRoute();
		socialDecisionMaker.setMinimumStartTime(socialDecisionMaker.getTime()+leg.getTravelTime());
		activity.setStartTime(socialDecisionMaker.decideStartTime());
		leg.setDepartureTime(activity.getStartTime()-leg.getTravelTime());
		socialDecisionMaker.setMaximumEndTime(((EmptyActivity)plan.getPlanElements().get(currentElementIndex)).getEndTime());
		activity.setEndTime(socialDecisionMaker.decideEndTime());
		return new Tuple<Leg, Activity>(leg, activity);
	}

}

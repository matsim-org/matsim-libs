package playground.sergioo.passivePlanning.core.mobsim.passivePlanning;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.passivePlanning.api.population.EmptyActivity;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.SinglePlannerAgentImpl;
import playground.sergioo.passivePlanning.core.population.decisionMakers.SocialDecisionMaker;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.DecisionMaker;

public class SinglePlannerAgentSocial extends SinglePlannerAgentImpl {

	//Constructors
	public SinglePlannerAgentSocial(SocialDecisionMaker socialDecisionMaker, Plan plan) {
		super(new DecisionMaker[]{socialDecisionMaker}, plan);
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

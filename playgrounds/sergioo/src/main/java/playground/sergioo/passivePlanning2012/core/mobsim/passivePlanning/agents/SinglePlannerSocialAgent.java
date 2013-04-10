package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.SinglePlannerAgentImpl;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.SocialDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.DecisionMaker;
import playground.sergioo.passivePlanning2012.core.router.TripUtils;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;

public class SinglePlannerSocialAgent extends SinglePlannerAgentImpl {

	//Constructors
	public SinglePlannerSocialAgent(ScenarioSimplerNetwork scenario, boolean carAvailability, Household household, TripRouter tripRouter, Plan plan, Set<String> modes, PassivePlannerDriverAgent agent) {
		super(new DecisionMaker[]{new SocialDecisionMaker(scenario, carAvailability, household, tripRouter, modes)}, plan, agent);
	}

	//Methods
	@Override
	public List<? extends PlanElement> getLegActivityLeg(double startTime, Id startFacilityId, double endTime, Id endFacilityId) {
		SocialDecisionMaker socialDecisionMaker = ((SocialDecisionMaker)decisionMakers[0]);
		Tuple<String, Id> typeOfActivityFacility = socialDecisionMaker.decideTypeOfActivityFacility(startTime, startFacilityId);
		if(typeOfActivityFacility==null)
			return null;
		ActivityFacility facility = socialDecisionMaker.getScenario().getActivityFacilities().getFacilities().get(typeOfActivityFacility.getSecond());
		Activity activity = new ActivityImpl(typeOfActivityFacility.getFirst(), facility.getLinkId());
		((ActivityImpl)activity).setFacilityId(typeOfActivityFacility.getSecond());
		List<? extends PlanElement> trip = socialDecisionMaker.decideModeRoute(startTime, startFacilityId, facility.getId());
		if(trip==null)
			return null;
		double tripTravelTime = TripUtils.calcTravelTime(trip);
		List<? extends PlanElement> trip2 = socialDecisionMaker.decideModeRoute(endTime-tripTravelTime, facility.getId(), endFacilityId);
		if(trip2==null)
			return null;
		double tripTravelTime2 = TripUtils.calcTravelTime(trip2);
		if(endTime-startTime<tripTravelTime+tripTravelTime2)
			return null;
		activity.setEndTime(socialDecisionMaker.decideEndTime(startTime+tripTravelTime, endTime-tripTravelTime2, typeOfActivityFacility.getFirst(), typeOfActivityFacility.getSecond()));
		List<PlanElement> tripActivityTrip = new ArrayList<PlanElement>(trip);
		tripActivityTrip.add(activity);
		tripActivityTrip.addAll(trip2);
		return tripActivityTrip;
	}
	public SocialDecisionMaker getSocialDecisionMaker() {
		return (SocialDecisionMaker)decisionMakers[0];
	}

}

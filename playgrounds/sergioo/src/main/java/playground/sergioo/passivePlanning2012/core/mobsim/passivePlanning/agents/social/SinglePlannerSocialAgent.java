package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.SinglePlannerAgentImpl;
import playground.sergioo.passivePlanning2012.core.population.PlacesSharer;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.SocialDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.DecisionMaker;
import playground.sergioo.passivePlanning2012.core.router.TripUtils;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;

public class SinglePlannerSocialAgent extends SinglePlannerAgentImpl {

	//Constructors
	public SinglePlannerSocialAgent(ScenarioSimplerNetwork scenario, boolean carAvailability, Household household, Set<String> modes, PassivePlannerDriverAgent agent) {
		super(new DecisionMaker[]{new SocialDecisionMaker(scenario, carAvailability, household, modes)}, agent);
	}

	//Methods
	@Override
	public List<? extends PlanElement> getLegActivityLeg(double startTime, CurrentTime now, Id<ActivityFacility> startFacilityId, double endTime, Id<ActivityFacility> endFacilityId, final MobsimStatus mobsimStatus) {
		SocialDecisionMaker socialDecisionMaker = ((SocialDecisionMaker)decisionMakers[0]);
		Tuple<String, Id<ActivityFacility>> typeOfActivityFacility = socialDecisionMaker.decideTypeOfActivityFacility(startTime, startFacilityId);
		if(typeOfActivityFacility==null)
			return null;
		ActivityFacility facility = socialDecisionMaker.getScenario().getActivityFacilities().getFacilities().get(typeOfActivityFacility.getSecond());
		Activity activity = new ActivityImpl(typeOfActivityFacility.getFirst(), facility.getLinkId());
		((ActivityImpl)activity).setFacilityId(typeOfActivityFacility.getSecond());
		List<? extends PlanElement> trip = socialDecisionMaker.decideModeRoute(startTime, startFacilityId, facility.getId(), tripRouter);
		if(trip==null)
			return null;
		double tripTravelTime = TripUtils.calcTravelTime(trip);
		List<? extends PlanElement> trip2 = socialDecisionMaker.decideModeRoute(endTime-tripTravelTime, facility.getId(), endFacilityId, tripRouter);
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
	public void addKnownPerson(PlacesSharer sharer) {
		((PlacesSharer)decisionMakers[0]).addKnownPerson(sharer);
	}
	public void shareKnownPlace(Id<ActivityFacility> facilityId, double startTime, String type) {
		((PlacesSharer)decisionMakers[0]).shareKnownPlace(facilityId, startTime, type);
	}
	public PlacesSharer getPlaceSharer() {
		return (PlacesSharer)decisionMakers[0];
	}

}

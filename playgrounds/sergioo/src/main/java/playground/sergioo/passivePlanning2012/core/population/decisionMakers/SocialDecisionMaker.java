package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.EndTimeDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.ModeRouteDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.TypeOfActivityFacilityDecisionMaker;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;

public class SocialDecisionMaker extends PlaceSharer implements EndTimeDecisionMaker, TypeOfActivityFacilityDecisionMaker, ModeRouteDecisionMaker {

	//Constants
	private static final double MAXIMUM_SEARCHING_DISTANCE = 3000;
	
	//Attributes
	private final ScenarioSimplerNetwork scenario;
	private final Household household;
	private final Set<String> modes;
	private final boolean carAvailability;
	
	//Constructors
	public SocialDecisionMaker(ScenarioSimplerNetwork scenario, boolean carAvailability, Household household, Set<String> modes) {
		this.scenario = scenario;
		this.household = household;
		this.carAvailability = carAvailability;
		this.modes = modes;
	}
	
	//Methods
	public Household getHousehold() {
		return household;
	}
	public ScenarioSimplerNetwork getScenario() {
		return scenario;
	}
	@Override
	public Tuple<String, Id> decideTypeOfActivityFacility(double time, Id startFacilityId) {
		Coord location = scenario.getActivityFacilities().getFacilities().get(startFacilityId).getCoord();
		List<Tuple<String, Id>> options = new ArrayList<Tuple<String, Id>>();
		double maximumDistance = MAXIMUM_SEARCHING_DISTANCE/2;
		if(knownPlaces.size()>0)
			//Known places
			while(options.size()==0) {
				for(KnownPlace knownPlace:knownPlaces.values()) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(knownPlace.getFacilityId());
					if(CoordUtils.calcDistance(location, facility.getCoord())<maximumDistance)
						for(String type:knownPlace.getActivityTypes(time))
							options.add(new Tuple<String, Id>(type, knownPlace.getFacilityId()));
				}
				maximumDistance *= 2;
			}
		else {
			//Activities of the household
			Period period = Period.getPeriod(time);
			for(Id memberID:household.getMemberIds()) {
				Person member = scenario.getPopulation().getPersons().get(memberID); 
				if(member instanceof BasePerson && !((BasePerson)member).isPlanning())
					for(Plan plan:member.getPlans())
						for(PlanElement planElement:plan.getPlanElements())
							if(planElement instanceof Activity && ((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME && Period.getPeriod(((Activity)planElement).getEndTime()).equals(period))
								options.add(new Tuple<String, Id>(((Activity)planElement).getType(), ((Activity)planElement).getFacilityId()));
			}
		}
		return options.size()==0?null:options.get((int) (Math.random()*options.size()));
	}
	@Override
	public List<? extends PlanElement> decideModeRoute(double time, Id startFacilityId, Id endFacilityId, TripRouter tripRouter) {
		List<? extends PlanElement> bestTrip = null;
		double minTime = Double.MAX_VALUE;
		ActivityFacility startFacility = scenario.getActivityFacilities().getFacilities().get(startFacilityId);
		ActivityFacility endFacility = scenario.getActivityFacilities().getFacilities().get(endFacilityId);
		for(String mode:modes)
			if(carAvailability || !mode.equals("car")) {
				List<? extends PlanElement> trip = tripRouter.calcRoute(mode, startFacility, endFacility, time, null);
				double currTime = time;
				for(PlanElement planElement:trip)
					if(planElement instanceof Leg)
						currTime += ((Leg)planElement).getTravelTime();
					else if(((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME)
						currTime = ((Activity)planElement).getEndTime();
					else if(((Activity)planElement).getMaximumDuration()!=Time.UNDEFINED_TIME)
						currTime += ((Activity)planElement).getMaximumDuration();
					else
						throw new RuntimeException("Plan element without time information");
				if(minTime>currTime-time) {
					minTime = currTime-time;
					bestTrip = trip; 
				}
			}
		return bestTrip;
	}
	@Override
	public double decideEndTime(double startTime, double maximumEndTime, String typeOfActivity, Id facilityId) {
		OpeningTime startTimeOpeningTime = null;
		ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		double maxFacilityTime= Time.MIDNIGHT;
		if(facility.getActivityOptions().get(typeOfActivity).getOpeningTimes(DayType.wkday)!=null) {
			for(OpeningTime openingTime:facility.getActivityOptions().get(typeOfActivity).getOpeningTimes(DayType.wkday))
				if(openingTime.getStartTime()<=startTime && startTime<=openingTime.getEndTime())
					startTimeOpeningTime = openingTime;
			if(startTimeOpeningTime!=null)
				maxFacilityTime = startTimeOpeningTime.getEndTime();
		}
		return Math.min(maxFacilityTime, maximumEndTime);
	}

}

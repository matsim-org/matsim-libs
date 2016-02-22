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
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.OpeningTime;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.population.PlacesSharer;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.EndTimeDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.ModeRouteDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.TypeOfActivityFacilityDecisionMaker;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.weeklySimulation.util.misc.Time;

public class SocialDecisionMaker extends PlacesSharer implements EndTimeDecisionMaker, TypeOfActivityFacilityDecisionMaker, ModeRouteDecisionMaker {

	//Constants
	private static final double MAXIMUM_SEARCHING_DISTANCE = 3000;
	
	//Attributes
	private final ScenarioSimplerNetwork scenario;
	private final Household household;
	private final Set<String> modes;
	private final boolean carAvailability;
	
	//Constructors
	public SocialDecisionMaker(ScenarioSimplerNetwork scenario, boolean carAvailability, Household household, Set<String> modes) {
		super();
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
	public Tuple<String, Id<ActivityFacility>> decideTypeOfActivityFacility(double time, Id<ActivityFacility> startFacilityId) {
		Coord location = scenario.getActivityFacilities().getFacilities().get(startFacilityId).getCoord();
		List<Tuple<String, Id<ActivityFacility>>> options = new ArrayList<Tuple<String, Id<ActivityFacility>>>();
		double maximumDistance = MAXIMUM_SEARCHING_DISTANCE/2;
		if(knownPlaces.size()>0)
			//Known places
			while(options.size()==0) {
				for(KnownPlace knownPlace:knownPlaces.values()) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(knownPlace.getFacilityId());
					if(CoordUtils.calcEuclideanDistance(location, facility.getCoord())<maximumDistance)
						for(String type:knownPlace.getActivityTypes(time))
							options.add(new Tuple<String, Id<ActivityFacility>>(type, knownPlace.getFacilityId()));
				}
				maximumDistance *= 2;
			}
		else {
			//Activities of the household
			Time.Period period = Time.Period.getPeriod(time);
			for(Id<Person> memberID:household.getMemberIds()) {
				Person member = scenario.getPopulation().getPersons().get(memberID); 
				if(member instanceof BasePerson)
					for(Plan plan:member.getPlans())
						for(PlanElement planElement:plan.getPlanElements())
							if(planElement instanceof Activity && ((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME && Time.Period.getPeriod(((Activity)planElement).getEndTime()).equals(period))
								options.add(new Tuple<String, Id<ActivityFacility>>(((Activity)planElement).getType(), ((Activity)planElement).getFacilityId()));
			}
		}
		return options.size()==0?null:options.get((int) (Math.random()*options.size()));
	}
	@Override
	public List<? extends PlanElement> decideModeRoute(double time, Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId, TripRouter tripRouter) {
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
	public double decideEndTime(double startTime, double maximumEndTime, String typeOfActivity, Id<ActivityFacility> facilityId) {
		OpeningTime startTimeOpeningTime = null;
		ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		double maxFacilityTime= Time.MIDNIGHT;
		if(facility.getActivityOptions().get(typeOfActivity).getOpeningTimes()!=null) {
			for(OpeningTime openingTime:facility.getActivityOptions().get(typeOfActivity).getOpeningTimes())
				if(openingTime.getStartTime()<=startTime && startTime<=openingTime.getEndTime())
					startTimeOpeningTime = openingTime;
			if(startTimeOpeningTime!=null)
				maxFacilityTime = startTimeOpeningTime.getEndTime();
		}
		return Math.min(maxFacilityTime, maximumEndTime);
	}

}

package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.EndTimeDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.ModeRouteDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.TypeOfActivityFacilityDecisionMaker;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;

public class SocialDecisionMaker implements EndTimeDecisionMaker, TypeOfActivityFacilityDecisionMaker, ModeRouteDecisionMaker {

	//Classes
	private enum Period {
	
		EARLY_MORNING(0, 7*3600-1),
		MORNING_PEAK(7*3600, 10*3600-1),
		BEFORE_LUNCH(10*3600, 13*3600-1),
		AFTER_LUNCH(13*3600, 18*3600-1),
		EVENING_PEAK(18*3600, 21*3600-1),
		NIGHT(21*3600, 24*3600-1);
		
		//Constants
		private static final double PERIODS_TIME = 24*3600;
		
		//Attributes
		private final double startTime;
		private final double endTime;
	
		//Constructors
		private Period(double startTime, double endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}
		private static Period getPeriod(double time) {
			for(Period period:Period.values())
				if(period.isPeriod(time))
					return period;
			return null;
		}
		private boolean isPeriod(double time) {
			time = time%PERIODS_TIME;
			if(startTime<=time && time<=endTime)
				return true;
			return false;
		}
		private double getInterval() {
			return endTime-startTime;
		}
	
	}
	private class KnownPlace {
		
		//Attributes
		public Id facilityId;
		public List<Tuple<Period, String>> timeTypes = new ArrayList<Tuple<Period, String>>();
		
		//Constructors
		public KnownPlace(Id facilityId) {
			this.facilityId = facilityId;
		}
		
	}
	
	//Constants
	private static final double MAXIMUM_SEARCHING_DISTANCE = 3000;
	
	//Attributes
	private final ScenarioSimplerNetwork scenario;
	private final Household household;
	private final Set<SocialDecisionMaker> knownPeople = new HashSet<SocialDecisionMaker>();
	private final Map<Id, KnownPlace> knownPlaces = new ConcurrentHashMap<Id, KnownPlace>();
	private final Map<String, Map<Id, Double[]>> knownTravelTimes = new HashMap<String, Map<Id, Double[]>>();
	private final TripRouter tripRouter;
	private Set<String> modes;
	private boolean carAvailability;
	
	//Constructors
	public SocialDecisionMaker(ScenarioSimplerNetwork scenario, boolean carAvailability, Household household, TripRouter tripRouter, Set<String> modes) {
		this.scenario = scenario;
		this.household = household;
		this.tripRouter = tripRouter;
		for(String mode:scenario.getConfig().plansCalcRoute().getNetworkModes())
			knownTravelTimes.put(mode, new HashMap<Id, Double[]>());
		this.carAvailability = carAvailability;
		this.modes = modes;
	}
	
	//Methods
	public Household getHousehold() {
		return household;
	}
	public Set<SocialDecisionMaker> getKnownPeople() {
		return knownPeople;
	}
	public void addKnownPerson(SocialDecisionMaker socialDecisionMaker) {
		knownPeople.add(socialDecisionMaker);
	}
	public void addKnownPlace(Id facilityId, Double time, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		knownPlace.timeTypes.add(new Tuple<Period, String>(Period.getPeriod(time), typeOfActivity));
	}
	public void setKnownTravelTime(double time, String mode, Id linkId, double travelTime) {
		for(Period period:Period.values())
			if(period.isPeriod(time))
				knownTravelTimes.get(mode).get(linkId)[period.ordinal()] = travelTime;
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
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(knownPlace.facilityId);
					if(CoordUtils.calcDistance(location, facility.getCoord())<maximumDistance)
						for(Tuple<Period, String> types:knownPlace.timeTypes)
							if(Period.getPeriod(time).equals(types.getFirst()))
								options.add(new Tuple<String, Id>(types.getSecond(), knownPlace.facilityId));
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
	public List<? extends PlanElement> decideModeRoute(double time, Id startFacilityId, Id endFacilityId) {
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

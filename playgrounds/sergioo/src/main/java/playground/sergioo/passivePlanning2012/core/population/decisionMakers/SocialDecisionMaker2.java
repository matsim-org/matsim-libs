package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import playground.sergioo.passivePlanning2012.core.network.ComposedLink;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.EndTimeDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.ModeRouteDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.StartTimeDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.TypeOfActivityFacilityDecisionMaker;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;

import java.util.*;

public class SocialDecisionMaker2 implements StartTimeDecisionMaker, EndTimeDecisionMaker, TypeOfActivityFacilityDecisionMaker, ModeRouteDecisionMaker {

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
			if(startTime<=time && time<=endTime)
				return true;
			return false;
		}
		private double getInterval() {
			return endTime-startTime;
		}
	
	}
	private class SimplerNetworkTravelDisutility implements TravelDisutility {
	
		//Attributes
		private String mode;
	
		//Methods
		private void setMode(String mode) {
			this.mode = mode;
		}
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			time = time%Period.PERIODS_TIME;
			double sum = 0, tSum = 0;
			for(Period period:Period.values()) {
				if(period.isPeriod(time))
					return knownTravelTimes.get(mode).get(link.getId())[period.ordinal()];
				sum += knownTravelTimes.get(mode).get(link.getId())[period.ordinal()]*period.getInterval();
				tSum += period.getInterval();
			}
			return sum/tSum;
		}
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			double min = Double.MAX_VALUE;
			for(Period period:Period.values())
				if(knownTravelTimes.get(mode).get(link.getId())[period.ordinal()]<min)
					min = knownTravelTimes.get(mode).get(link.getId())[period.ordinal()];
			return min;
		}
	
	}
	private class SimplerNetworkTravelTime implements TravelTime {
	
		//Attributes
		private String mode;
	
		//Methods
		public void setMode(String mode) {
			this.mode = mode;
		}
		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			time = time%Period.PERIODS_TIME;
			double sum = 0, tSum = 0;
			for(Period period:Period.values()) {
				if(period.isPeriod(time))
					return knownTravelTimes.get(mode).get(link.getId())[period.ordinal()];
				sum += knownTravelTimes.get(mode).get(link.getId())[period.ordinal()]*period.getInterval();
				tSum += period.getInterval();
			}
			return sum/tSum;
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
	private final Map<Id, KnownPlace> knownPlaces = new HashMap<Id, KnownPlace>();
	private final Map<String, Map<Id, Double[]>> knownTravelTimes = new HashMap<String, Map<Id, Double[]>>();
	private final SimplerNetworkTravelDisutility simpleTravelDisutility =  new SimplerNetworkTravelDisutility();
	private final SimplerNetworkTravelTime simpleTravelTime =  new SimplerNetworkTravelTime();
	private final IntermodalLeastCostPathCalculator leastCostPathCalculator;
	
	//Constructors
	public SocialDecisionMaker2(ScenarioSimplerNetwork scenario, Household household, IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this.scenario = scenario;
		if(household!=null)
			this.household = household;
		else
			this.household = null;
		this.leastCostPathCalculator = leastCostPathCalculator;
		for(String mode:scenario.getConfig().plansCalcRoute().getNetworkModes())
			knownTravelTimes.put(mode, new HashMap<Id, Double[]>());
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
		while(options.size()==0) {
			for(KnownPlace knownPlace:knownPlaces.values()) {
				ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(knownPlace.facilityId);
				if(CoordUtils.calcDistance(location, facility.getCoord())<maximumDistance)
					for(Tuple<Period, String> types:knownPlace.timeTypes)
						if(Period.getPeriod(time).equals(types.getFirst()))
							options.add(new Tuple<String, Id>(types.getSecond(), knownPlace.facilityId));
			}
			maximumDistance*=2;
		}
		return options.get((int) (Math.random()*options.size()));
	}
	@Override
	public List<? extends PlanElement> decideModeRoute(double time, Id startLinkId, Id endLinkId, TripRouter tripRouter) {
		String bestMode = null;
		List<Link> bestPath = null;
		double minTime = Double.MAX_VALUE;
		Link startLink = scenario.getNetwork().getLinks().get(startLinkId);
		Link endLink = scenario.getNetwork().getLinks().get(endLinkId);
		for(String mode:knownTravelTimes.keySet()) {
			simpleTravelDisutility.setMode(mode);
			simpleTravelTime.setMode(mode);
			LeastCostPathCalculatorFactory routerFactory = new FastDijkstraFactory();
			LeastCostPathCalculator leastCostPathCalculator = routerFactory.createPathCalculator(scenario.getSimplerNetwork(mode), simpleTravelDisutility, simpleTravelTime); 
			Path path = leastCostPathCalculator.calcLeastCostPath(NetworkUtils.getNearestLink(((NetworkImpl) scenario.getSimplerNetwork(mode)), startLink.getCoord()).getFromNode(), NetworkUtils.getNearestLink(((NetworkImpl) scenario.getSimplerNetwork(mode)), endLink.getCoord()).getToNode(), time, null, null);
			if(path.travelTime<minTime) {
				minTime = path.travelTime;
				bestPath = path.links;
				bestMode = mode;
			}
		}
		Set<String> mode = new HashSet<String>();
		mode.add(bestMode);
		leastCostPathCalculator.setModeRestriction(mode);
		NetworkRoute networkRoute = getFullNetworkRoute(bestPath, startLinkId, endLinkId, time);
		Leg leg = new LegImpl(bestMode);
		leg.setRoute(networkRoute);
		return Arrays.asList(leg);
	}
	private NetworkRoute getFullNetworkRoute(List<Link> bestPath, Id startLinkId, Id endLinkId, double time) {
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(startLinkId, endLinkId);
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		Link startLink = scenario.getNetwork().getLinks().get(startLinkId);
		Link endLink = scenario.getNetwork().getLinks().get(endLinkId);
		Node prevNode = startLink.getToNode(), currNode = null;
		for(Link link:bestPath) {
			currNode = ((ComposedLink)link).getStartNode();
			Path internalNodePath = leastCostPathCalculator.calcLeastCostPath(prevNode, currNode, time, null, null);
			for(Link linkFull:internalNodePath.links)
				links.add(linkFull.getId());
			prevNode = ((ComposedLink)link).getEndNode();
			Path path = leastCostPathCalculator.calcLeastCostPath(currNode, prevNode, time, null, null);
			for(Link linkFull:path.links)
				links.add(linkFull.getId());
		}
		Path path = leastCostPathCalculator.calcLeastCostPath(prevNode, endLink.getFromNode(), time, null, null);
		for(Link linkFull:path.links)
			links.add(linkFull.getId());
		networkRoute.setLinkIds(startLinkId, links, endLinkId);
		return networkRoute;
	}
	@Override
	public double decideStartTime(double minimumStartTime, Id facilityId) {
		return minimumStartTime;
	}
	@Override
	public double decideEndTime(double startTime, double maximumEndTime, String typeOfActivity, Id facilityId) {
		OpeningTime startTimeOpeningTime = null;
		ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(facilityId);
		for(OpeningTime openingTime:facility.getActivityOptions().get(typeOfActivity).getOpeningTimes())
			if(openingTime.getStartTime()<=startTime && startTime<=openingTime.getEndTime())
				startTimeOpeningTime = openingTime;
		return Math.min(startTimeOpeningTime.getEndTime(), maximumEndTime);
	}

}

package playground.sergioo.passivePlanning.core.population.decisionMakers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.FastDijkstra;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.passivePlanning.core.network.ComposedLink;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.EndTimeDecisionMaker;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.FacilityDecisionMaker;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.ModeRouteDecisionMaker;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.StartTimeDecisionMaker;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.TypeOfActivityDecisionMaker;
import playground.sergioo.passivePlanning.core.scenario.ScenarioSimplerNetwork;

public class SocialDecisionMaker implements TypeOfActivityDecisionMaker, StartTimeDecisionMaker, EndTimeDecisionMaker, FacilityDecisionMaker, ModeRouteDecisionMaker {

	//Classes
	private class Period {
	
		//Attributes
		public final double startTime;
		public final double endTime;
		public final String name;
		
		//Constructors
		public Period(double startTime, double endTime) {
			this(startTime, endTime, null);
		}
		public Period(double startTime, double endTime, String name) {
			super();
			this.startTime = startTime;
			this.endTime = endTime;
			this.name = name;
		}
	
	}
	private class TravelMinCostSimplerNetwork implements TravelDisutility {
	
		//Attributes
		private String mode;
	
		//Methods
		public void setMode(String mode) {
			this.mode = mode;
		}
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			time = time%periodsTime;
			double sum = 0, tSum = 0;
			for(int i=0; i<periods.length; i++) {
				if(periods[i].startTime<=time && time<=periods[i].endTime)
					return knownTravelTimes.get(mode).get(link.getId())[i];
				double tTime = periods[i].endTime - periods[i].startTime;
				sum += knownTravelTimes.get(mode).get(link.getId())[i]*tTime;
				tSum += tTime;
			}
			return sum/tSum;
		}
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			double min = Double.MAX_VALUE;
			for(int i=0; i<periods.length; i++)
				if(knownTravelTimes.get(mode).get(link.getId())[i]<min)
					min = knownTravelTimes.get(mode).get(link.getId())[i];
			return min;
		}
	
	}
	private class TravelTimeSimplerNetwork implements TravelTime {
	
		//Attributes
		private String mode;
	
		//Methods
		public void setMode(String mode) {
			this.mode = mode;
		}
		@Override
		public double getLinkTravelTime(Link link, double time) {
			time = time%periodsTime;
			double sum = 0, tSum = 0;
			for(int i=0; i<periods.length; i++) {
				if(periods[i].startTime<=time && time<=periods[i].endTime)
					return knownTravelTimes.get(mode).get(link.getId())[i];
				double tTime = periods[i].endTime - periods[i].startTime;
				sum += knownTravelTimes.get(mode).get(link.getId())[i]*tTime;
				tSum += tTime;
			}
			return sum/tSum;
		}
	
	}

	//Attributes
	private final ScenarioSimplerNetwork scenario;
	private final Household household;
	private final Set<Id> knownPeople = new HashSet<Id>();
	private final Set<Id> knownPlaces = new HashSet<Id>();
	private final Period[] periods;
	private final double periodsTime;
	private final Map<String, Map<Id, Double[]>> knownTravelTimes = new HashMap<String, Map<Id, Double[]>>();
	private final TravelMinCostSimplerNetwork travelMinCostSimple =  new TravelMinCostSimplerNetwork();
	private final TravelTimeSimplerNetwork travelTimeSimple =  new TravelTimeSimplerNetwork();
	private final IntermodalLeastCostPathCalculator leastCostPathCalculator;
	private Link startLink;
	private double time;
	private String typeOfActivity;
	private ActivityFacility facility;
	private Leg leg;
	private double minimumStartTime = time;
	private double startTime;
	private double maximumEndTime = Double.POSITIVE_INFINITY;
	
	//Methods
	public SocialDecisionMaker(ScenarioSimplerNetwork scenario, Household household, IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this.scenario = scenario;
		if(household!=null)
			this.household = household;
		else
			this.household = null;
		this.leastCostPathCalculator = leastCostPathCalculator;
		for(String mode:scenario.getConfig().plansCalcRoute().getNetworkModes())
			knownTravelTimes.put(mode, new HashMap<Id, Double[]>());
		periods = new Period[6];
		periods[0] = new Period(0, 7*3600-1, "Early morning");
		periods[1] = new Period(7*3600, 10*3600-1, "Morning peak");
		periods[2] = new Period(10*3600, 13*3600-1, "Before lunch");
		periods[3] = new Period(13*3600, 18*3600-1, "After lunch");
		periods[4] = new Period(18*3600, 21*3600-1, "Evening peak");
		periods[5] = new Period(21*3600, 24*3600-1, "Evening peak");
		periodsTime = 24*3600;
	}
	public Household getHousehold() {
		return household;
	}
	public Set<Id> getKnownPeople() {
		return knownPeople;
	}
	public void addKnownPerson(Id id) {
		knownPeople.add(id);
	}
	public Set<Id> getKnownPlaces() {
		return knownPlaces;
	}
	public void addKnownPlace(Id id) {
		knownPlaces.add(id);
	}
	public void setKnownTravelTime(double time, String mode, Id linkId, double travelTime) {
		for(int i=0; i<periods.length; i++)
			if(periods[i].startTime<=time && time<=periods[i].endTime)
				knownTravelTimes.get(mode).get(linkId)[i] = travelTime;
	}
	@Override
	public double getTime() {
		return time;
	}
	@Override
	public void setTime(double time) {
		this.time = time;
	}
	@Override
	public String decideTypeOfActivity() {
		String typeOfActivity = null;
		//TODO
		this.typeOfActivity = typeOfActivity;
		return typeOfActivity;
	}
	@Override
	public void setTypeOfActivity(String typeOfActivity) {
		this.typeOfActivity = typeOfActivity;
	}
	@Override
	public ActivityFacility decideFacility() {
		ActivityFacility facility = null;
		//TODO
		this.facility = facility;
		return facility;
	}
	@Override
	public void setFacility(ActivityFacility facility) {
		this.facility = facility;
	}
	@Override
	public void setLeg(Leg leg) {
		this.leg = leg;
	}
	@Override
	public void setStartLink(Link startLink) {
		this.startLink = startLink;
	}
	@Override
	public void setEndLink(Link link) {
		((ActivityFacilityImpl)facility).setLinkId(link.getId());
	}
	@Override
	public Leg decideModeRoute() {
		Link endLink = scenario.getNetwork().getLinks().get(facility.getLinkId());
		String bestMode = null;
		List<Link> bestPath = null;
		double minTime = Double.MAX_VALUE;
		for(String mode:knownTravelTimes.keySet()) {
			travelMinCostSimple.setMode(mode);
			travelTimeSimple.setMode(mode);
			LeastCostPathCalculator leastCostPathCalculator = new FastDijkstra(scenario.getSimplerNetwork(mode), travelMinCostSimple, travelTimeSimple);
			Path path = leastCostPathCalculator.calcLeastCostPath(((NetworkImpl)scenario.getSimplerNetwork(mode)).getNearestLink(startLink.getCoord()).getFromNode(), ((NetworkImpl)scenario.getSimplerNetwork(mode)).getNearestLink(endLink.getCoord()).getToNode(), time, null, null);
			if(path.travelTime<minTime) {
				minTime = path.travelTime;
				bestPath = path.links;
				bestMode = mode;
			}
		}
		Set<String> mode = new HashSet<String>();
		mode.add(bestMode);
		leastCostPathCalculator.setModeRestriction(mode);
		NetworkRoute networkRoute = getFullNetworkRoute(bestPath);
		if(leg==null)
			leg = new LegImpl(bestMode);
		else
			leg.setMode(bestMode);
		leg.setRoute(networkRoute);
		return leg;
	}
	private NetworkRoute getFullNetworkRoute(List<Link> bestPath) {
		Link endLink = scenario.getNetwork().getLinks().get(facility.getLinkId());
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
		List<Id> links = new ArrayList<Id>();
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
		networkRoute.setLinkIds(startLink.getId(), links, endLink.getId());
		return networkRoute;
	}
	@Override
	public void setMinimumStartTime(double minimumStartTime) {
		this.minimumStartTime = minimumStartTime;
	}
	@Override
	public double decideStartTime() {
		double startTime = minimumStartTime;
		this.startTime = startTime;
		return startTime;
	}
	@Override
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	@Override
	public void setMaximumEndTime(double maximumEndTime) {
		this.maximumEndTime = maximumEndTime;
	}
	@Override
	public double decideEndTime() {
		OpeningTime startTimeOpeningTime = null;
		for(OpeningTime openingTime:facility.getActivityOptions().get(typeOfActivity).getOpeningTimes(DayType.wkday))
			if(openingTime.getStartTime()<=startTime && startTime<=openingTime.getEndTime())
				startTimeOpeningTime = openingTime;
		return Math.min(startTimeOpeningTime.getEndTime(), maximumEndTime);
	}

}

package playground.balac.twowaycarsharingredisigned.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ActivityDurationUtils;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacility;
import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacilityImpl;


public class TwoWayCSPersonDriverAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent{

	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private static int expectedLinkWarnCount = 0;
	
	final Person person;

	private MobsimVehicle vehicle;

	Id<Link> cachedNextLinkId = null;

	// This agent never seriously calls the simulation back! (That's good.)
	// It is only held to get to the EventManager and to the Scenario, and, 
	// in a special case, to the AgentCounter (still necessary?)  michaz 01-2012
	private final Netsim simulation;

	private double activityEndTime = Time.UNDEFINED_TIME;

	private Id<Link> currentLinkId = null;

	int currentPlanElementIndex = 0;

	private final Plan plan;

	private transient Id<Link> cachedDestinationLinkId;

	private Leg currentLeg;

	private List<Id<Link>> cachedRouteLinkIds = null;

	int currentLinkIdIndex;

	private MobsimAgent.State state = MobsimAgent.State.ABORT;
	
	private Scenario scenario;	
	
	private MatsimServices controler;
	
	private Link startLink;
	

	private TwoWayCSVehicleLocation twvehiclesLocation;	
	
	ArrayList<ActivityFacility> carsharingVehicleLocations;
	
	ArrayList<ActivityFacility> startFacilities;
	
	private String twVehId;
	
	HashMap<Link, Link> map = new HashMap<Link, Link>();
	
	double beelineFactor = 0.0;
	
	double walkSpeed = 0.0;
	// ============================================================================================================================
	// c'tor

	public TwoWayCSPersonDriverAgentImpl(final Person person, final Plan plan, final Netsim simulation, final Scenario scenario, final MatsimServices controler, TwoWayCSVehicleLocation twvehiclesLocation) {
		this.person = person;
		this.simulation = simulation;
		this.controler = controler;
		this.plan = plan;
		this.scenario = scenario;
		this.twvehiclesLocation = twvehiclesLocation;
		
		beelineFactor = Double.parseDouble(controler.getConfig().getModule("planscalcroute").getParams().get("beelineDistanceFactor"));
		walkSpeed = Double.parseDouble(controler.getConfig().getModule("planscalcroute").getParams().get("teleportedModeSpeed_walk"));
		
		carsharingVehicleLocations = new ArrayList<ActivityFacility>();
		map = new HashMap<Link, Link>();
		List<? extends PlanElement> planElements = this.plan.getPlanElements();
		if (planElements.size() > 0) {
			this.currentPlanElementIndex = 0;
			Activity firstAct = (Activity) planElements.get(0);				
			this.currentLinkId = firstAct.getLinkId();
			this.state = MobsimAgent.State.ACTIVITY ;
			calculateAndSetDepartureTime(firstAct);
		}
		throw new RuntimeException("Should this class still be in use?  I think there is a delegated version of this, isn't there?  "
				+ "This one here causes additional refactoring work. kai, feb'16" ) ;

	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endActivityAndComputeNextState(final double now) {
		Activity act = (Activity) this.getPlanElements().get(this.currentPlanElementIndex);
		this.simulation.getEventsManager().processEvent(
				new ActivityEndEvent(now, this.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
		advancePlan(now);
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endLegAndComputeNextState(final double now) {
		this.simulation.getEventsManager().processEvent(new PersonArrivalEvent(
						now, this.getPerson().getId(), this.getDestinationLinkId(), currentLeg.getMode()));
	
		
		if( (!(this.currentLinkId == null && this.cachedDestinationLinkId == null)) 
				&& !this.currentLinkId.equals(this.cachedDestinationLinkId)) {
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.cachedDestinationLinkId
					+ ", but arrived on link " + this.currentLinkId + ". Removing the agent from the simulation.");
			this.state = MobsimAgent.State.ABORT ;
		} else {
			// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
				if (currentLeg.getMode().equals("twowaycarsharing") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg) {
					
					twvehiclesLocation.addVehicle(scenario.getNetwork().getLinks().get(this.cachedDestinationLinkId), twVehId);
					twVehId = null;
				}
				advancePlan(now) ;
		}
	}

	@Override
	public final void setStateToAbort(final double now) {
		this.state = MobsimAgent.State.ABORT ;
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
		this.currentLinkId = linkId;
		double distance = ((Leg) getCurrentPlanElement()).getRoute().getDistance();
		this.simulation.getEventsManager().processEvent(new TeleportationArrivalEvent(this.simulation.getSimTimer().getTimeOfDay(), person.getId(), distance));
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		if (expectedLinkWarnCount < 10 && !newLinkId.equals(this.cachedNextLinkId)) {
			log.warn("Agent did not end up on expected link. Ok for within-day replanning agent, otherwise not.  Continuing " +
					"anyway ... This warning is suppressed after the first 10 warnings.") ;
			expectedLinkWarnCount++;
		}
		this.currentLinkId = newLinkId;
		this.currentLinkIdIndex++;
		this.cachedNextLinkId = null; //reset cached nextLink
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	@Override
	public Id<Link> chooseNextLinkId() {
		
		// Please, let's try, amidst all checking and caching, to have this method return the same thing
		// if it is called several times in a row. Otherwise, you get Heisenbugs.
		// I just fixed a situation where this method would give a warning about a bad route and return null
		// the first time it is called, and happily return a link id when called the second time.
		
		// michaz 2013-08
		
		if (this.cachedNextLinkId != null) {
			return this.cachedNextLinkId;
		}
		if (this.cachedRouteLinkIds == null) {
			if ( this.currentLeg.getRoute() instanceof NetworkRoute ) {
				this.cachedRouteLinkIds = ((NetworkRoute) this.currentLeg.getRoute()).getLinkIds();
			} else {
				// (seems that this can happen if an agent is a DriverAgent, but wants to start a pt leg. 
				// A situation where Marcel's ``wrapping approach'' may have an advantage.  On the other hand,
				// DriverAgent should be a NetworkAgent, i.e. including pedestrians, and then this function
				// should always be answerable.  kai, nov'11)
				return null ;
			}
		}

		if (this.currentLinkIdIndex >= this.cachedRouteLinkIds.size() ) {
			// we have no more information for the route, so the next link should be the destination link
			Link currentLink = this.simulation.getScenario().getNetwork().getLinks().get(this.currentLinkId);
			Link destinationLink = this.simulation.getScenario().getNetwork().getLinks().get(this.cachedDestinationLinkId);
			if (currentLink.getToNode().equals(destinationLink.getFromNode())) {
				this.cachedNextLinkId = destinationLink.getId();
				return this.cachedNextLinkId;
			}
			if (!(this.currentLinkId.equals(this.cachedDestinationLinkId))) {
				// there must be something wrong. Maybe the route is too short, or something else, we don't know...
				log.error("The vehicle with driver " + this.getPerson().getId() + ", currently on link " + this.currentLinkId.toString()
						+ ", is at the end of its route, but has not yet reached its destination link " + this.cachedDestinationLinkId.toString());
				// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
			}
			return null; // vehicle is at the end of its route
		}


		Id<Link> nextLinkId = this.cachedRouteLinkIds.get(this.currentLinkIdIndex);
		Link currentLink = this.simulation.getScenario().getNetwork().getLinks().get(this.currentLinkId);
		Link nextLink = this.simulation.getScenario().getNetwork().getLinks().get(nextLinkId);
		if (currentLink.getToNode().equals(nextLink.getFromNode())) {
			this.cachedNextLinkId = nextLinkId; //save time in later calls, if link is congested
			return this.cachedNextLinkId;
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentLinkIdIndex + " ]");
		// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
		return null;
	}


	// ============================================================================================================================
	// below there only (package-)private methods or setters/getters

	private void advancePlan(double now) {
		this.currentPlanElementIndex++;

		// check if plan has run dry:
		if ( this.currentPlanElementIndex >= this.getPlanElements().size() ) {
			log.error("plan of agent with id = " + this.getId() + " has run empty.  Setting agent state to ABORT\n" +
					"          (but continuing the mobsim).  This used to be an exception ...") ;
			this.state = MobsimAgent.State.ABORT ;
			return;
		}

		PlanElement pe = this.getCurrentPlanElement() ;
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			initializeActivity(act);
		} else if (pe instanceof Leg) {
			Leg leg = (Leg) pe;
			//start of added stuff		
								
			if (leg.getMode().equals( "walk_rb" )) {
				
				if (this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) + 1) instanceof Leg) {
				
					initializeTwoWayCarsharingStartWalkLeg(leg);
				}
				else if (this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) - 1) instanceof Leg) {
					
					initializeTwoWayCarsharingEndWalkLeg(leg);
					
				}
				else
					initializeLeg(leg);
				
			}
			else if (leg.getMode().equals("twowaycarsharing")) {
				if (this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) - 1) instanceof Activity &&
						this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) + 1) instanceof Activity)
					
					initializeTwoWayCSMidleCarLeg(startLink, now);
				
				else if (this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) - 1) instanceof Leg)
					
					initializeTwoWayCarsharingCarLeg(startLink, now);
				
				else if (this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) + 1) instanceof Leg)
					
					initializeTwoWayCarsharingEndCarLeg(startLink, now);
				else 
					log.error("this should neevr happen");
				
			}
			else //end of added stuff
				initializeLeg(leg);
		} else {
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}
	//added methods
	private void initializeTwoWayCarsharingStartWalkLeg(Leg leg) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();
		TwoWayCSStation station = findClosestAvailableTWCar(route.getStartLinkId());
		
		if (station == null) {
			this.state = MobsimAgent.State.ABORT ;
			return;
			
		}		
				
		startLink = station.getLink();
		twVehId = station.getIDs().get(0);
		twvehiclesLocation.removeVehicle(station, station.getIDs().get(0));
		
		map.put(scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()), startLink);
		
		LegImpl walkLeg = new LegImpl("walk_rb");
		
		GenericRouteImpl walkRoute = new GenericRouteImpl(route.getStartLinkId(), startLink.getId());
		final double dist = CoordUtils.calcEuclideanDistance(scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startLink.getCoord());
		final double estimatedNetworkDistance = dist * this.beelineFactor;

		final int travTime = (int) (estimatedNetworkDistance / this.walkSpeed );
		walkRoute.setTravelTime(travTime);
		walkRoute.setDistance(estimatedNetworkDistance);	
		
		walkLeg.setRoute(walkRoute);
		this.cachedDestinationLinkId = startLink.getId();
		walkLeg.setTravelTime(travTime);
		// set the route according to the next leg
		this.currentLeg = walkLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;		
		
		return;
		
	}
	
	private void initializeTwoWayCarsharingCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		
		//create route for the car part of the twowaycarsharing trip
		
		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();
		
		Provider<TripRouter> tripRouterFactory = controler.getTripRouterProvider();
		
		TripRouter tripRouter = tripRouterFactory.get();
		
		Network network = scenario.getNetwork();

		Coord coordStart = new Coord(l.getCoord().getX(), l.getCoord().getY());
		
		TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("1000000000", TwoWayCSFacility.class), coordStart, l.getId());

		Coord coordEnd = new Coord(network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord().getX(), network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord().getY());

		TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("1000000001", TwoWayCSFacility.class), coordEnd, leg.getRoute().getEndLinkId());
		
		for(PlanElement pe1: tripRouter.calcRoute("car", startFacility, endFacility, now, person)) {
	    	
			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
	    			travelTime += ((Leg) pe1).getTravelTime();
			}
		}
		
		LegImpl carLeg = new LegImpl("twowaycarsharing");
		
		carLeg.setTravelTime( travelTime );
		NetworkRoute route = ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, l.getId(), leg.getRoute().getEndLinkId());
		route.setLinkIds( l.getId(), ids, leg.getRoute().getEndLinkId());
		route.setTravelTime( travelTime);
		carLeg.setRoute(route);
		this.cachedDestinationLinkId = route.getEndLinkId();

			// set the route according to the next leg
		this.currentLeg = carLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;			
			
		return;		
			
	}	
	
	private void initializeTwoWayCSMidleCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		
		//create route for the car part of the twowaycarsharing trip
		
		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();
		
		Provider<TripRouter> tripRouterFactory = controler.getTripRouterProvider();
		
		TripRouter tripRouter = tripRouterFactory.get();
		
		Network network = scenario.getNetwork();
		
		
		TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("1000000000", TwoWayCSFacility.class), network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord(), leg.getRoute().getStartLinkId());
		
		TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("1000000001", TwoWayCSFacility.class), network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord(), leg.getRoute().getEndLinkId());
		
		for(PlanElement pe1: tripRouter.calcRoute("car", startFacility, endFacility, now, person)) {
	    	
			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
	    			travelTime += ((Leg) pe1).getTravelTime();
			}
			}
		
		LegImpl carLeg = new LegImpl("twowaycarsharing");
		
		carLeg.setTravelTime( travelTime );
		NetworkRoute route = ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
		route.setLinkIds( leg.getRoute().getStartLinkId(), ids, leg.getRoute().getEndLinkId());
		route.setTravelTime( travelTime);
		carLeg.setRoute(route);
		this.cachedDestinationLinkId = route.getEndLinkId();

			// set the route according to the next leg
		this.currentLeg = carLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;		
			
		return;
	}
	
	private void initializeTwoWayCarsharingEndCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		
		//create route for the car part of the twowaycarsharing trip
		
		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();
		
		Provider<TripRouter> tripRouterFactory = controler.getTripRouterProvider();
		
		TripRouter tripRouter = tripRouterFactory.get();
		
		Network network = scenario.getNetwork();
		
		Link link = map.get(network.getLinks().get(leg.getRoute().getEndLinkId()));
		
		TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("1000000000", TwoWayCSFacility.class), network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord() , leg.getRoute().getStartLinkId());
		
		TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("1000000001", TwoWayCSFacility.class), link.getCoord(), link.getId());
		
		for(PlanElement pe1: tripRouter.calcRoute("car", startFacility, endFacility, now, person)) {
	    	
			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
	    			travelTime += ((Leg) pe1).getTravelTime();
			}
			}
		
		LegImpl carLeg = new LegImpl("twowaycarsharing");
		
		carLeg.setTravelTime( travelTime );
		NetworkRoute route = ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, leg.getRoute().getStartLinkId(), link.getId());
		route.setLinkIds( leg.getRoute().getStartLinkId(), ids, link.getId());
		route.setTravelTime( travelTime);
		carLeg.setRoute(route);
		this.cachedDestinationLinkId = link.getId();

			// set the route according to the next leg
		this.currentLeg = carLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;		
			
		return;
	}
	
	private void initializeTwoWayCarsharingEndWalkLeg(Leg leg) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();		
		
		LegImpl walkLeg = new LegImpl("walk_rb");
		
		Link link = map.get(scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
		map.remove(scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
		GenericRouteImpl walkRoute = new GenericRouteImpl(link.getId(), route.getEndLinkId());
		final double dist = CoordUtils.calcEuclideanDistance(link.getCoord(), scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord());
		final double estimatedNetworkDistance = dist * this.beelineFactor;

		final int travTime = (int) (estimatedNetworkDistance / this.walkSpeed );
		walkRoute.setTravelTime(travTime);
		walkRoute.setDistance(estimatedNetworkDistance);		
		
		walkLeg.setRoute(walkRoute);
		this.cachedDestinationLinkId = route.getEndLinkId();
		walkLeg.setTravelTime(travTime);
		// set the route according to the next leg
		this.currentLeg = walkLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;		
		
		return;
		
	}

	private TwoWayCSStation findClosestAvailableTWCar(Id<Link> linkId) {
		
		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		//if no cars within certain radius return null
		Link link = scenario.getNetwork().getLinks().get(linkId);
		
		Collection<TwoWayCSStation> location = twvehiclesLocation.getQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), Double.parseDouble(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing")));
		if (location.isEmpty()) return null;
		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing"));
		TwoWayCSStation closest = null;
		for(TwoWayCSStation station: location) {
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), station.getLink().getCoord());
			}			
			
		}
					
		return closest;
				
	}
	
	//the end of added methods	
	
	private void initializeLeg(Leg leg) {
		this.state = MobsimAgent.State.LEG ;			
		Route route = leg.getRoute();
		if (route == null) {
			log.error("The agent " + this.getPerson().getId() + " has no route in its leg.  Setting agent state to ABORT " +
					"(but continuing the mobsim).");
			if ( noRouteWrnCnt < 1 ) {
				log.info( "(Route is needed inside Leg even if you want teleportation since Route carries the start/endLinkId info.)") ;
				noRouteWrnCnt++ ;
			}
			this.state = MobsimAgent.State.ABORT ;
			return;
		} else {
			this.cachedDestinationLinkId = route.getEndLinkId();
			
			this.currentLeg = leg;
			this.cachedRouteLinkIds = null;
			this.currentLinkIdIndex = 0;
			this.cachedNextLinkId = null;
			return;
		}
	}
	
	private void initializeActivity(Activity act) {
		this.state = MobsimAgent.State.ACTIVITY ;

		double now = this.getMobsim().getSimTimer().getTimeOfDay() ;
		this.simulation.getEventsManager().processEvent(
				new ActivityStartEvent(now, this.getId(), this.currentLinkId, act.getFacilityId(), act.getType()));
		/* schedule a departure if either duration or endtime is set of the activity.
		 * Otherwise, the agent will just stay at this activity for ever...
		 */
		calculateAndSetDepartureTime(act);
	}

	/**
	 * Some data of the currently simulated Leg is cached to speed up
	 * the simulation. If the Leg changes (for example the Route or
	 * the Destination Link), those cached data has to be reseted.
	 *</p>
	 * If the Leg has not changed, calling this method should have no effect
	 * on the Results of the Simulation!
	 */
	void resetCaches() {
		
		// moving this method not to WithinDay for the time being since it seems to make some sense to keep this where the internal are
		// known best.  kai, oct'10
		// Compromise: package-private here; making it public in the Withinday class.  kai, nov'10

		this.cachedNextLinkId = null;
		this.cachedRouteLinkIds = null;
		this.cachedDestinationLinkId = null;

		/*
		 * The Leg may have been exchanged in the Person's Plan, so
		 * we update the Reference to the currentLeg Object.
		 */
		PlanElement currentPlanElement = this.getPlanElements().get(this.currentPlanElementIndex);
		if (currentPlanElement instanceof Leg) {
			this.currentLeg  = ((Leg) currentPlanElement);
			this.cachedRouteLinkIds = null;

			Route route = currentLeg.getRoute();
			if (route == null) {
				log.error("The agent " + this.getId() + " has no route in its leg. Removing the agent from the simulation." );
				//			"          (But as far as I can tell, this will not truly remove the agent???  kai, nov'11)");
				//			this.simulation.getAgentCounter().decLiving();
				//			this.simulation.getAgentCounter().incLost();
				this.state = MobsimAgent.State.ABORT ;
				return;
			}
			this.cachedDestinationLinkId = route.getEndLinkId();
		} else {			
			// If an activity is performed, update its current activity.
			this.calculateAndSetDepartureTime((Activity) this.getCurrentPlanElement());
		}
	}

	/**
	 * If this method is called to update a changed ActivityEndTime please
	 * ensure, that the ActivityEndsList in the {@link QSim} is also updated.
	 */
	void calculateAndSetDepartureTime(Activity act) {
		double now = this.getMobsim().getSimTimer().getTimeOfDay() ;
		PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation =
				(this.simulation.getScenario().getConfig().plans().getActivityDurationInterpretation());
		double departure = ActivityDurationUtils.calculateDepartureTime(act, now, activityDurationInterpretation);

		if ( this.currentPlanElementIndex == this.getPlanElements().size()-1 ) {
			if ( finalActHasDpTimeWrnCnt < 1 && departure!=Double.POSITIVE_INFINITY ) {
				log.error( "last activity of person driver agent id " + this.person.getId() + " has end time < infty; setting it to infty") ;
				log.error( Gbl.ONLYONCE ) ;
				finalActHasDpTimeWrnCnt++ ;
			}
			departure = Double.POSITIVE_INFINITY ;
		}

		this.activityEndTime = departure ;
	}

	private static int finalActHasDpTimeWrnCnt = 0 ;


	private static int noRouteWrnCnt = 0 ;

	/**
	 * Convenience method delegating to person's selected plan
	 * @return list of {@link Activity}s and {@link Leg}s of this agent's plan
	 */
	private final List<PlanElement> getPlanElements() {
		return this.getCurrentPlan().getPlanElements();
	}

	public final Netsim getMobsim(){
		return this.simulation;
	}

	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.getPlanElements().get(this.currentPlanElementIndex);
	}

	@Override
	public final PlanElement getNextPlanElement() {
		if ( this.currentPlanElementIndex < this.getPlanElements().size() ) {
			return this.getPlanElements().get( this.currentPlanElementIndex+1 ) ;
		} else {
			return null ;
		}
	}

	@Override
	public final void setVehicle(final MobsimVehicle veh) {
		this.vehicle = veh;
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return this.vehicle;
	}

	@Override
	public final double getActivityEndTime() {
		// yyyyyy I don't think there is any guarantee that this entry is correct after an activity end re-scheduling.  kai, oct'10
		return this.activityEndTime;
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		// note: the method is really only defined for DriverAgent!  kai, oct'10
		return this.currentLinkId;
	}

	@Override
	public final Double getExpectedTravelTime() {
		
		return (this.currentLeg).getTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {

        return (this.currentLeg).getRoute().getDistance();
    }

    @Override
	public final String getMode() {
		if( this.currentPlanElementIndex >= this.plan.getPlanElements().size() ) {
			// just having run out of plan elements it not an argument for not being able to answer the "mode?" question.
			// this is in most cases called in "abort".  kai, mar'12

			return null ;
		}
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return ((Leg) currentPlanElement).getMode() ;
	}

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.
		if (((Leg)currentPlanElement).getMode().equals("twowaycarsharing")){
			
			return Id.create("TW_"+ (twVehId), Vehicle.class);	
		
		}
		else if (route.getVehicleId() != null) {
			return route.getVehicleId();
		} else {
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
		}
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.cachedDestinationLinkId;
	}

	@Override
	public final Person getPerson() {
		return this.person;
	}

	@Override
	public final Id<Person> getId() {
		return this.person.getId();
	}

	

	@Override
	public MobsimAgent.State getState() {
		return state;
	}

	@Override
	public Plan getCurrentPlan() {
		// TODO Auto-generated method stub
		return this.plan;
	}
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
		// kai, nov'14
		if ( this.chooseNextLinkId()==null ) {
			return true ;
		} else {
			return false ;
		}
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public PlanElement getPreviousPlanElement() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}

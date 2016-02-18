package playground.balac.allcsmodestest.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
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
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playground.balac.allcsmodestest.events.CarsharingLegFinishedEvent;
import playground.balac.allcsmodestest.events.EndRentalEvent;
import playground.balac.allcsmodestest.events.NoParkingSpaceEvent;
import playground.balac.allcsmodestest.events.NoVehicleCarSharingEvent;
import playground.balac.allcsmodestest.events.StartRentalEvent;
import playground.balac.freefloating.qsim.FreeFloatingStation;
import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingStation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingVehicleLocation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;
import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacility;
import playground.balac.twowaycarsharingredisigned.scenario.TwoWayCSFacilityImpl;


/**
 * Current version includes:
 * -- two-way carsharing with reservation of the vehicle at the end of the activity preceding the rental.
 * -- one-way carsharing with each station having a parking capacity with the reservation system as the one with two-way
 * -- free-floating carsharing with parking at the link of the next activity following free-floating trip, reservation system as the one with two-way cs.
 * 
 * @author balac
 */

 
public class PersonDriverAgentOnlyMembersImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, PTPassengerAgent{

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
	
	private Link startLinkFF;
	
	private Link endLinkOW;
	
	private Link startLinkTW;
	
	private Link startLinkOW;	
	
	private OneWayCarsharingRDWithParkingStation startStationOW;
	
	private OneWayCarsharingRDWithParkingStation endStationOW;
	
	private TwoWayCSVehicleLocation twvehiclesLocation;	
	private OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocation;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;	
	
	HashMap<Link, Link> mapTW = new HashMap<Link, Link>();
	HashMap<Link, Link> mapOW = new HashMap<Link, Link>();
	
	private String ffVehId;
	private String owVehId;
	private String twVehId;
	
	double beelineFactor = 0.0;
	
	double walkSpeed = 0.0;

	private TripRouter tripRouter;
	// ============================================================================================================================
	// c'tor

	public PersonDriverAgentOnlyMembersImpl(final Person person, final Plan plan, 
			final Netsim simulation, final Scenario scenario,
			FreeFloatingVehiclesLocation ffvehiclesLocation, 
			OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocation, 
			TwoWayCSVehicleLocation twvehiclesLocation, TripRouter tripRouter) {
		this.person = person;
		this.simulation = simulation;
		this.plan = plan;
		this.scenario = scenario;
				
		this.ffvehiclesLocation = ffvehiclesLocation;
		this.owvehiclesLocation = owvehiclesLocation;
		this.twvehiclesLocation = twvehiclesLocation;
		this.tripRouter = tripRouter;
		
		beelineFactor = ((PlansCalcRouteConfigGroup)scenario.getConfig().getModule("planscalcroute")).getBeelineDistanceFactors().get("walk");
		walkSpeed = (((PlansCalcRouteConfigGroup)scenario.getConfig().getModule("planscalcroute")).getTeleportedModeSpeeds().get("walk"));
		//carsharingVehicleLocations = new ArrayList<ActivityFacility>();
		mapTW = new HashMap<Link, Link>();
		mapOW = new HashMap<Link, Link>();
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
	
		if (currentLeg.getMode().equals("twowaycarsharing"))
		this.simulation.getEventsManager().processEvent(
				new CarsharingLegFinishedEvent(now, this.getPerson().getId(), Id.create("TW_"+(this.twVehId), Vehicle.class), currentLeg));
		
		else if (currentLeg.getMode().equals("freefloating"))
			this.simulation.getEventsManager().processEvent(
					new CarsharingLegFinishedEvent(now, this.getPerson().getId(), Id.create("FF_"+(this.ffVehId), Vehicle.class), currentLeg));
		else if (currentLeg.getMode().equals("onewaycarsharing"))
			this.simulation.getEventsManager().processEvent(
					new CarsharingLegFinishedEvent(now, this.getPerson().getId(), Id.create("OW_"+(this.owVehId), Vehicle.class), currentLeg));
		
		if( (!(this.currentLinkId == null && this.cachedDestinationLinkId == null))) 
				if( !this.currentLinkId.equals(this.cachedDestinationLinkId)) {
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.cachedDestinationLinkId
					+ ", but arrived on link " + this.currentLinkId + ". Removing the agent from the simulation.");
			this.state = MobsimAgent.State.ABORT ;
		} else {
			// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
			
			 if (currentLeg.getMode().equals("twowaycarsharing") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg) {
				 this.simulation.getEventsManager().processEvent(
							new EndRentalEvent(now, this.cachedDestinationLinkId, this.getPerson().getId(), Id.create("TW_"+(this.twVehId), Vehicle.class)));
				 parkCSVehicle(this.currentLeg, this.plan);
			 }
			 else if (currentLeg.getMode().equals("walk_rb") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg)
				 this.simulation.getEventsManager().processEvent(
							new StartRentalEvent(now, this.cachedDestinationLinkId, this.getPerson().getId(), Id.create("TW_"+(this.twVehId), Vehicle.class)));
			 else if (currentLeg.getMode().equals("freefloating")) {// && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg) {
				
				 this.simulation.getEventsManager().processEvent(
							new EndRentalEvent(now, this.cachedDestinationLinkId, this.getPerson().getId(), Id.create("FF_"+(this.ffVehId), Vehicle.class)));
				 parkCSVehicle(this.currentLeg, this.plan);
			 }
			 
			 else if (currentLeg.getMode().equals("walk_ff") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg)
				 this.simulation.getEventsManager().processEvent(
							new StartRentalEvent(now, this.cachedDestinationLinkId, this.getPerson().getId(), Id.create("FF_"+(this.ffVehId), Vehicle.class)));
			 
			 else if (currentLeg.getMode().equals("onewaycarsharing") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg) {
				 this.simulation.getEventsManager().processEvent(
						new EndRentalEvent(now, this.cachedDestinationLinkId, this.getPerson().getId(), Id.create("OW_"+(this.owVehId), Vehicle.class)));
				 parkCSVehicle(this.currentLeg, this.plan);
			 }
		 
		 else if (currentLeg.getMode().equals("walk_ow_sb") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg)
			 this.simulation.getEventsManager().processEvent(
						new StartRentalEvent(now, this.cachedDestinationLinkId, this.getPerson().getId(), Id.create("OW_"+(this.owVehId), Vehicle.class)));
			 
							
			advancePlan(now) ;
		}
	}	
	
	private void parkCSVehicle( Leg currentLeg, Plan plan) {
		if (currentLeg.getMode().equals("onewaycarsharing")) {
			
			owvehiclesLocation.addVehicle(endStationOW, owVehId);
			owVehId = null;
		}
		else if (currentLeg.getMode().equals("twowaycarsharing") && plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg) {
			
			twvehiclesLocation.addVehicle(scenario.getNetwork().getLinks().get(this.cachedDestinationLinkId), twVehId);
			twVehId = null;
		}
		else if (this.currentLeg.getMode().equals("freefloating")) {
			
			ffvehiclesLocation.addVehicle(scenario.getNetwork().getLinks().get(this.cachedDestinationLinkId), ffVehId);
			ffVehId = null;
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
			if (currentLink == destinationLink && this.currentLinkIdIndex > this.cachedRouteLinkIds.size()) {
				// this can happen if the last link in a route is a loop link. Don't ask, it can happen in special transit simulation cases... mrieser/jan2014
				return null;
			}
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
			
			String mode = leg.getMode();
			
			PlanElement previousPlanElement = this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) - 1);
			PlanElement nextPlanElement = this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) + 1);
				
			if (mode.equals( "walk_rb" )) {
				
				if (nextPlanElement instanceof Leg) {
				
					initializeTwoWayCarsharingStartWalkLeg(leg, now);
				}
				else if (previousPlanElement instanceof Leg) {
					
					initializeTwoWayCarsharingEndWalkLeg(leg, now);
					
				}
				
								
			}
			else if (mode.equals("twowaycarsharing")) {
				if (previousPlanElement instanceof Activity &&
						nextPlanElement instanceof Activity)
					
					initializeTwoWayCSMidleCarLeg(startLinkTW, now);
				
				else if (previousPlanElement instanceof Leg && !(nextPlanElement instanceof Leg))
					
					initializeTwoWayCarsharingCarLeg(startLinkTW, now);
				
				else if (previousPlanElement instanceof Leg && (nextPlanElement instanceof Leg))
					
					initializeTwoWayCarsharingEmptyCarLeg(startLinkTW, now);
				
				else if (nextPlanElement instanceof Leg)
					
					initializeTwoWayCarsharingEndCarLeg(startLinkTW, now);
				else 
					log.error("this should never happen");
				
			}
			else if (mode.equals( "walk_ff" )) {				
				
					initializeFreeFLoatingWalkLeg(leg, now);
			
			}
			else if (mode.equals( "freefloating" )) {
				initializeFreeFLoatingCarLeg(startLinkFF, now);
				
			} 
			else if (mode.equals( "walk_ow_sb" )) {
				
				if (nextPlanElement instanceof Leg) {
				
					initializeOneWayCarsharingStartWalkLeg(leg, now);
				}
				else if (previousPlanElement instanceof Leg) {
					
					initializeOneWayCarsharingEndWalkLeg(leg, now);
					
				}				
				
			}
			else if (mode.equals( "onewaycarsharing" )) {
				initializeOneWayCarsharingCarLeg(startLinkOW, now);
				
			}
			else //end of added stuff
				
				
				initializeLeg(leg);
		} else {
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}
	
	//added methods
	
	private void initializeCSWalkLeg(String mode, double now, Link startLink, Link destinationLink) {
		LegImpl walkLeg = new LegImpl(mode);
		
		GenericRouteImpl walkRoute = new GenericRouteImpl(startLink.getId(), destinationLink.getId());
		final double dist = CoordUtils.calcEuclideanDistance(startLink.getCoord(), destinationLink.getCoord());
		final double estimatedNetworkDistance = dist * beelineFactor;

		final int travTime = (int) (estimatedNetworkDistance / walkSpeed);
		walkRoute.setTravelTime(travTime);
		walkRoute.setDistance(estimatedNetworkDistance);	
		
		walkLeg.setRoute(walkRoute);
		this.cachedDestinationLinkId = destinationLink.getId();
		
		walkLeg.setDepartureTime(now);
		walkLeg.setTravelTime(travTime);
		walkLeg.setArrivalTime(now + travTime);
		// set the route according to the next leg
		this.currentLeg = walkLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;
		
	}
	
	private void initializeCSVehicleLeg (String mode, double now, Link startLink, Link destinationLink) {
		double travelTime = 0.0;

		Coord coordStart = new Coord(startLink.getCoord().getX(), startLink.getCoord().getY());
		
		TwoWayCSFacilityImpl startFacility = new TwoWayCSFacilityImpl(Id.create("1000000000", TwoWayCSFacility.class), coordStart, startLink.getId());

		Coord coordEnd = new Coord(destinationLink.getCoord().getX(), destinationLink.getCoord().getY());

		TwoWayCSFacilityImpl endFacility = new TwoWayCSFacilityImpl(Id.create("1000000001", TwoWayCSFacility.class), coordEnd, destinationLink.getId());
		double distance = 0.0;
		for(PlanElement pe1: this.tripRouter.calcRoute("car", startFacility, endFacility, now, person)) {
	    	
			if (pe1 instanceof Leg) {
    			travelTime += ((Leg) pe1).getTravelTime();
	    		distance += ((Leg) pe1).getRoute().getDistance();	
			}
		}
		
		LegImpl carLeg = new LegImpl(mode);
		
		carLeg.setTravelTime( travelTime );
		
		GenericRouteImpl route = new GenericRouteImpl(startLink.getId(), destinationLink.getId());

		route.setDistance(distance);
		route.setTravelTime( travelTime);
		
		route.setStartLinkId(startLink.getId());
		route.setEndLinkId(destinationLink.getId());
		if (mode.equals("twowaycarsharing"))
			route.setRouteDescription("TW_" + (twVehId));
		else if (mode.equals("onewaycarsharing"))
			route.setRouteDescription("OW_" + (owVehId));
		else if (mode.equals("freefloating"))
			route.setRouteDescription("FF_" + (ffVehId));


		carLeg.setRoute(route);
		this.cachedDestinationLinkId = route.getEndLinkId();

			// set the route according to the next leg
		this.currentLeg = carLeg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;	
		
		
		
	}
	
	private void initializeTwoWayCarsharingStartWalkLeg(Leg leg, double now) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();
				
		TwoWayCSStation station = findClosestAvailableTWCar(route.getStartLinkId());
		
		if (station == null) {
			this.state = MobsimAgent.State.ABORT;
			this.simulation.getEventsManager().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "rt"));
			return;
			
		}		
				
		startLinkTW = station.getLink();
		twVehId = station.getIDs().get(0);
		twvehiclesLocation.removeVehicle(station, station.getIDs().get(0));
		
		mapTW.put(scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()), startLinkTW);
		initializeCSWalkLeg("walk_rb", now, scenario.getNetwork().getLinks().get(route.getStartLinkId()), startLinkTW);
					
	}
	
	private void initializeTwoWayCarsharingCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		
		//create route for the car part of the twowaycarsharing trip
		initializeCSVehicleLeg("twowaycarsharing", now, l, scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
		
			
	}	
	private void initializeTwoWayCarsharingEmptyCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
				
		initializeCSVehicleLeg("twowaycarsharing", now, l, l);		
			
	}
	private void initializeTwoWayCSMidleCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		Network network = scenario.getNetwork();
		
		//create route for the car part of the twowaycarsharing trip
		initializeCSVehicleLeg("twowaycarsharing", now, network.getLinks().get(leg.getRoute().getStartLinkId()), network.getLinks().get(leg.getRoute().getEndLinkId()));
		
	}
	
	private void initializeTwoWayCarsharingEndCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		Network network = scenario.getNetwork();
		Link link = mapTW.get(network.getLinks().get(leg.getRoute().getEndLinkId()));
		
		//create route for the car part of the twowaycarsharing trip
		initializeCSVehicleLeg("twowaycarsharing", now, network.getLinks().get(leg.getRoute().getStartLinkId()), link);
		
	}
	
	private void initializeTwoWayCarsharingEndWalkLeg(Leg leg, double now) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();		
		
		Link link = mapTW.get(scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
		mapTW.remove(scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
		initializeCSWalkLeg("walk_rb", now, link, scenario.getNetwork().getLinks().get(route.getEndLinkId()));				
				
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
	
	private void initializeFreeFLoatingWalkLeg(Leg leg, double now) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();
		FreeFloatingStation location = findClosestAvailableCar(route.getStartLinkId());
		
		if (location == null) {
			this.state = MobsimAgent.State.ABORT ;
			this.simulation.getEventsManager().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ff"));

			return;
			
		}
		ffVehId = location.getIDs().get(0);
		ffvehiclesLocation.removeVehicle(location.getLink(), ffVehId);
		startLinkFF = location.getLink();
		initializeCSWalkLeg("walk_ff", now, scenario.getNetwork().getLinks().get(route.getStartLinkId()), startLinkFF);
				
	}
	
	private void initializeFreeFLoatingCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		
		//create route for the car part of the freefloating trip
		initializeCSVehicleLeg("freefloating", now, l, scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
				
			
	}
	

	private FreeFloatingStation findClosestAvailableCar(Id<Link> linkId) {		
		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		Link link = scenario.getNetwork().getLinks().get(linkId);
		
		FreeFloatingStation location = ffvehiclesLocation.getQuadTree().getClosest(link.getCoord().getX(), link.getCoord().getY());
				
		return location;
	}
	
	
	private void initializeOneWayCarsharingStartWalkLeg(Leg leg, double now) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();
		OneWayCarsharingRDWithParkingStation station = findClosestAvailableOWCar(route.getStartLinkId());
		
		if (station == null) {
			this.state = MobsimAgent.State.ABORT ;
			this.simulation.getEventsManager().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ow"));

			return;
			
		}
		startStationOW = station;
		owVehId = station.getIDs().get(0);
		owvehiclesLocation.removeVehicle(station, owVehId);
		startLinkOW = station.getLink();
		
		initializeCSWalkLeg("walk_ow_sb", now, scenario.getNetwork().getLinks().get(route.getStartLinkId()), startLinkOW);
		
		
	}
	
	private void initializeOneWayCarsharingCarLeg(Link l, double now) {
		this.state = MobsimAgent.State.LEG;
		
		PlanElement pe = this.getCurrentPlanElement() ;
		
		Leg leg =  (Leg) pe;
		Network network = scenario.getNetwork();
		endStationOW = findClosestAvailableParkingSpace(network.getLinks().get(leg.getRoute().getEndLinkId()));
		
		if (endStationOW == null) {
			
			this.state = MobsimAgent.State.ABORT ;
			this.simulation.getEventsManager().processEvent(new NoParkingSpaceEvent(now, leg.getRoute().getEndLinkId(), "ow"));

			return;
		}
		else {
			startStationOW.freeParkingSpot();
			endStationOW.reserveParkingSpot();

			Link destinationLink = endStationOW.getLink();
			//create route for the car part of the onewaycarsharing trip
			this.endLinkOW = destinationLink;
			initializeCSVehicleLeg("onewaycarsharing", now, l, destinationLink);
		}
			
	}
	private void initializeOneWayCarsharingEndWalkLeg(Leg leg, double now) {
		
		this.state = MobsimAgent.State.LEG;
		Route route = leg.getRoute();		

		initializeCSWalkLeg("walk_ow_sb", now, endLinkOW, scenario.getNetwork().getLinks().get(route.getEndLinkId()));
				
	}

	private OneWayCarsharingRDWithParkingStation findClosestAvailableOWCar(Id<Link> linkId) {
		
		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		//if no cars within certain radius return null
		Link link = scenario.getNetwork().getLinks().get(linkId);
		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("OneWayCarsharing").getParams().get("searchDistanceOneWayCarsharing"));

		Collection<OneWayCarsharingRDWithParkingStation> location = owvehiclesLocation.getQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		OneWayCarsharingRDWithParkingStation closest = null;
		for(OneWayCarsharingRDWithParkingStation station: location) {
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), station.getLink().getCoord());
			}			
			
		}			
		//owvehiclesLocation.removeVehicle(closest.getLink());
		return closest;
		
		
	}
	
	private OneWayCarsharingRDWithParkingStation findClosestAvailableParkingSpace(Link link) {
		
		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		//if no cars within certain radius return null
		
		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("OneWayCarsharing").getParams().get("searchDistanceOneWayCarsharing"));

		Collection<OneWayCarsharingRDWithParkingStation> location = owvehiclesLocation.getQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		OneWayCarsharingRDWithParkingStation closest = null;
		for(OneWayCarsharingRDWithParkingStation station: location) {
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfAvailableParkingSpaces() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), station.getLink().getCoord());
			}			
			
		}		
			
		//owvehiclesLocation.removeVehicle(closest.getLink());
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
		return  (currentLeg.getTravelTime() );
		
	}

    @Override
    public Double getExpectedTravelDistance() {
        return (currentLeg.getRoute().getDistance() );
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
		
		if (((Leg)currentPlanElement).getMode().equals("freefloating")){
			
			return Id.create("FF_"+ (ffVehId), Vehicle.class);	
		
		}
		else if (((Leg)currentPlanElement).getMode().equals("onewaycarsharing")){
			
			return Id.create("OW_"+ (owVehId), Vehicle.class);	
		
		}
		else if (((Leg)currentPlanElement).getMode().equals("twowaycarsharing")){
			if (twVehId == null) {
				
				log.info("Twowaycarsahring vehicle ID is null for person with id " + this.getId() +" , returning person id as vehicle id and continuing! ");
				return Id.create(this.getId(), Vehicle.class);
			}
				
			return Id.create("TW_"+ (twVehId), Vehicle.class);	
		
		}
		else if (route.getVehicleId() != null) 
				return route.getVehicleId();
		 
		else
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
		
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

	/**
	 * The Plan this agent is executing. Please assume this to be immutable. 
	 * Modifying a Plan which comes out of this method is a programming error.
	 * This will eventually be replaced by a read-only interface.
	 * 
	 */
	@Override
	public final Plan getCurrentPlan() {
		return plan;
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		if (line.getId().equals(route.getLineId())) {
			return containsId(stopsToCome, route.getEgressStopId());
		} else {
			return false;
		}
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}

	@Override
	public double getWeight() {
		return 1.0;
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		Leg leg = getCurrentLeg();
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ leg.getRoute().getRouteDescription());
			return null;
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			Id<TransitStopFacility> accessStopId = route.getAccessStopId();
			return accessStopId;
		}
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		return route.getEgressStopId();
	}
	
	protected Leg getCurrentLeg() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		return (Leg) currentPlanElement;
	}

	protected boolean containsId(List<TransitRouteStop> stopsToCome,
			Id<TransitStopFacility> egressStopId) {
		for (TransitRouteStop stop : stopsToCome) {
			if (egressStopId.equals(stop.getStopFacility().getId())) {
				return true;
			}
		}
		return false;
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

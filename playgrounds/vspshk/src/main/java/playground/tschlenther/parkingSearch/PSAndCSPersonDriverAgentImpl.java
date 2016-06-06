package playground.tschlenther.parkingSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.events.NoParkingSpaceEvent;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.facility.DummyFacility;
import org.matsim.contrib.carsharing.qsim.CarSharingVehicles;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.utils.EditRoutes;

/**
 * @author balac, tschlenther
 */
public class PSAndCSPersonDriverAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, PTPassengerAgent {

	private static final Logger log = Logger.getLogger(PSAndCSPersonDriverAgentImpl.class);

	private static final double MAXIMUM_WALKING_TIME = 660;
	private ParkingMode searchMode = ParkingMode.DRIVING;
	
	private CarSharingVehicles carSharingVehicles;
	private String ffVehId;

	double beelineFactor = 0.0;
	double walkSpeed = 0.0;

	private TripRouter tripRouter;

	private final BasicPlanAgentImpl basicAgentDelegate ;
	private final TransitAgentImpl transitAgentDelegate ;
	private final PlanBasedDriverAgentImpl driverAgentDelegate ;
	private LeastCostPathCalculator pathCalculator;

	public PSAndCSPersonDriverAgentImpl(final Plan plan, final Netsim simulation, 
			CarSharingVehicles carSharingVehicles, TripRouter tripRouter, LeastCostPathCalculator pathCalculator) {
		this.pathCalculator = pathCalculator;
		Scenario scenario = simulation.getScenario() ;

		this.basicAgentDelegate = new BasicPlanAgentImpl( plan, scenario, simulation.getEventsManager(), simulation.getSimTimer() ) ;
		this.transitAgentDelegate = new TransitAgentImpl( this.basicAgentDelegate ) ;
		this.driverAgentDelegate = new PlanBasedDriverAgentImpl( this.basicAgentDelegate ) ;

		this.basicAgentDelegate.getModifiablePlan() ; // this lets the agent make a full copy of the plan, which can then be modified

		this.carSharingVehicles = carSharingVehicles;

		this.tripRouter = tripRouter;

		beelineFactor = scenario.getConfig().plansCalcRoute().getBeelineDistanceFactors().get("walk");
		walkSpeed = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get("walk");
		//carsharingVehicleLocations = new ArrayList<ActivityFacility>();

		if ( scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) {
			throw new RuntimeException( "does not work with a TripRouter that inserts access/egress walk") ;
		}
		if ( scenario.getConfig().qsim().getNumberOfThreads() != 1 ) {
			throw new RuntimeException("does not work with multiple qsim threads (will use same instance of router)") ; 
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endActivityAndComputeNextState(final double now) {

		if (this.basicAgentDelegate.getNextPlanElement() instanceof Leg && 
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("freefloating")) {

			insertFreeFloatingTripWhenEndingActivity(now);			
		}
		if (!this.getState().equals(State.ABORT))
			this.basicAgentDelegate.endActivityAndComputeNextState(now);

	}

	private void insertFreeFloatingTripWhenEndingActivity(double now) {
		// (_next_ plan element is (presumably) a leg)
		
		List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();
		int indexOfInsertion = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;

		final List<PlanElement> trip = new ArrayList<PlanElement>();
		
		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		RouteFactoryImpl routeFactory = ((PopulationFactoryImpl)pf).getRouteFactory() ;

		// === walk leg: ===

		NetworkRoute route = (NetworkRoute) ((Leg)this.basicAgentDelegate.getNextPlanElement()).getRoute();
		final Link currentLink = scenario.getNetwork().getLinks().get(route.getStartLinkId());
		final Link destinationLink = scenario.getNetwork().getLinks().get(route.getEndLinkId());

		FreeFloatingStation station = findClosestAvailableCar(currentLink);
		if (station == null) {
			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, currentLink.getId(), "ff"));
			return;
		}
		ffVehId = station.getIDs().get(0);
		
		final Link stationLink = scenario.getNetwork().getLinks().get( station.getLinkId() ) ;
		this.carSharingVehicles.getFreeFLoatingVehicles().removeVehicle(stationLink, ffVehId); 
		
		Route routeToCar = routeFactory.createRoute( Route.class, currentLink.getId(), stationLink.getId() ) ; 
		final double dist = CoordUtils.calcEuclideanDistance(currentLink.getCoord(), stationLink.getCoord()) * beelineFactor;
		routeToCar.setTravelTime( (dist / walkSpeed));
		routeToCar.setDistance(dist);	

		final Leg leg = pf.createLeg( "walk_ff" );
		leg.setRoute(routeToCar);

		trip.add( leg );

		// === car leg: ===

		Vehicle vehicle = null ;
		Path path = this.pathCalculator.calcLeastCostPath(stationLink.getToNode(), destinationLink.getFromNode(), now, this.basicAgentDelegate.getPerson(), vehicle ) ;
		
		NetworkRoute carRoute = routeFactory.createRoute(NetworkRoute.class, stationLink.getId(), destinationLink.getId() );
		carRoute.setLinkIds(stationLink.getId(), NetworkUtils.getLinkIds( path.links), destinationLink.getId());
		carRoute.setTravelTime( path.travelTime );
		carRoute.setVehicleId( Id.create("FF_" + (ffVehId), Vehicle.class) ) ;
		// (yyyy this should be the same physical vehicle as the one that is removed from the link --> should NOT change the ID! kai, feb'16)

		Leg carLeg = pf.createLeg("freefloating");
		carLeg.setTravelTime( path.travelTime );
		carLeg.setRoute(carRoute);

		trip.add(carLeg);
		
		// === insert trip: ===

		planElements.remove(this.basicAgentDelegate.getNextPlanElement());
		planElements.addAll(indexOfInsertion, trip);
	}
	
	
	private void insertWalkingLegWhenParkingElsewhere(Id<Link> tripDestination){
				List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();
				int indexOfInsertion = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;

				final List<PlanElement> trip = new ArrayList<PlanElement>();
				
				Scenario scenario = this.basicAgentDelegate.getScenario() ;
				PopulationFactory pf = scenario.getPopulation().getFactory() ;
				RouteFactoryImpl routeFactory = ((PopulationFactoryImpl)pf).getRouteFactory() ;

				// === walk leg: ===

				final Id<Link> currentLink = this.getCurrentLinkId();

				Route routeToActivity = routeFactory.createRoute( Route.class, currentLink, tripDestination) ; 
				final double dist = CoordUtils.calcEuclideanDistance(scenario.getNetwork().getLinks().get(currentLink).getCoord(), scenario.getNetwork().getLinks().get(tripDestination).getCoord()) * beelineFactor;
				routeToActivity.setTravelTime( (dist / walkSpeed));
				routeToActivity.setDistance(dist);	

				final Leg leg = pf.createLeg( "walk_parking" );
				leg.setRoute(routeToActivity);

				trip.add( leg );

				// === insert walking leg: ===

				planElements.addAll(indexOfInsertion, trip);
	}

	@Override
	public final void endLegAndComputeNextState(final double now) {
		parkCSVehicle( );			
		this.basicAgentDelegate.endLegAndComputeNextState(now);
	}	


	private void parkCSVehicle() {
		String currentLegMode = ((Leg) this.basicAgentDelegate.getCurrentPlanElement() ).getMode() ;
		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		if (currentLegMode.equals("freefloating")) {
			this.carSharingVehicles.getFreeFLoatingVehicles().addVehicle(scenario.getNetwork().getLinks().get(this.getDestinationLinkId()), ffVehId);
			ffVehId = null;
		}
	}

	@Deprecated // use method below directly (do lookup in calling method)
	private FreeFloatingStation findClosestAvailableCar(Id<Link> linkId) {		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		Link link = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(linkId);

		return findClosestAvailableCar(link);
	}

	private FreeFloatingStation findClosestAvailableCar(Link link) {
		FreeFloatingStation location = this.carSharingVehicles.getFreeFLoatingVehicles().getQuadTree().getClosest(link.getCoord().getX(), link.getCoord().getY());
		return location;
	}

	void resetCaches() {
		WithinDayAgentUtils.resetCaches(this.basicAgentDelegate);
	}

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.
		if (route.getVehicleId() != null) 
			return route.getVehicleId();
		else
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
	}

	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.basicAgentDelegate.getCurrentPlanElement() ;
	}

	@Override
	public final PlanElement getNextPlanElement() {
		return this.basicAgentDelegate.getNextPlanElement() ;
	}

	@Override
	public final void setVehicle(final MobsimVehicle veh) {
		this.basicAgentDelegate.setVehicle(veh) ;
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return this.basicAgentDelegate.getVehicle() ;
	}

	@Override
	public final double getActivityEndTime() {
		return this.basicAgentDelegate.getActivityEndTime() ;
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		return this.driverAgentDelegate.getCurrentLinkId() ;
	}

	@Override
	public final Double getExpectedTravelTime() {
		return this.basicAgentDelegate.getExpectedTravelTime() ;
	}

	@Override
	public Double getExpectedTravelDistance() {
		return this.basicAgentDelegate.getExpectedTravelDistance() ;
	}

	@Override
	public final String getMode() {
		return this.basicAgentDelegate.getMode() ;
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.basicAgentDelegate.getDestinationLinkId() ;
	}

	@Override
	public final Person getPerson() {
		return this.basicAgentDelegate.getPerson() ;
	}

	@Override
	public final Id<Person> getId() {
		return this.basicAgentDelegate.getId() ;
	}

	@Override
	public final Plan getCurrentPlan() {
		return this.basicAgentDelegate.getCurrentPlan() ;
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		return this.transitAgentDelegate.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle) ;
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		return this.transitAgentDelegate.getExitAtStop(stop) ;
	}

	@Override
	public double getWeight() {
		return this.transitAgentDelegate.getWeight() ;
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		return this.transitAgentDelegate.getDesiredAccessStopId() ;
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		return this.transitAgentDelegate.getDesiredAccessStopId() ;
	}


	@Override
	public MobsimAgent.State getState() {
		return this.basicAgentDelegate.getState() ;
	}
	@Override
	public final void setStateToAbort(final double now) {
		this.basicAgentDelegate.setStateToAbort(now);
	}

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
		this.basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		this.driverAgentDelegate.notifyMoveOverNode(newLinkId);
	}

	/**
	 * when not searching for parking space, method is delegated
	 * when in searching mode, it is assumed that agents know the surrounding area: they choose the one link out of
	 * the outlinks of currentlink that has the closest toNode to the toNode of the destination link.
	 * in networks with high percentage of bidirectional links, this may not be very constructive.
	 * if the next link is the destination link, the agent is put in search mode.
	 */
	@Override
	public Id<Link> chooseNextLinkId() {
		Id<Link> destination = this.getDestinationLinkId();
		if ( !this.searchMode.equals(ParkingMode.SEARCHING)) {
			Id<Link> nextId =  driverAgentDelegate.chooseNextLinkId();
			if(nextId.equals(destination)){
				this.searchMode = ParkingMode.SEARCHING;
			}
			return nextId;
		} else {
		Link currentLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get( this.getCurrentLinkId() ) ;
		Node currentToNode = currentLink.getToNode() ;
		Link destinationLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(destination) ;
			
		// pick randomly out of outlinks of toNode
		
//		int idx = MatsimRandom.getRandom().nextInt( toNode.getOutLinks().size() ) ;
//			for ( Link outLink : toNode.getOutLinks().values() ) {
//				if ( idx==0 ) {
//					return outLink.getId() ;
//				}
//				idx-- ;
//			}
//		throw new RuntimeException("should not happen" ) ;
		
		// choose the link with the closest toNode to the destinationLink's toNode
		
		Link LinkWithClosestToNode = null;
		double closestDist = Double.MAX_VALUE;
		double distCache = closestDist;
		for(Link next: currentToNode.getOutLinks().values()){
			 distCache = NetworkUtils.getEuclideanDistance(next.getToNode().getCoord(), destinationLink.getToNode().getCoord()); 
			if(distCache < closestDist){
				closestDist = distCache;
				LinkWithClosestToNode = next;
			}
			if(distCache == closestDist){
				if(Math.random() < 0.5) LinkWithClosestToNode = next;
			}
		}
		return LinkWithClosestToNode.getId();
		
		}
	}
	
	/**
	 * as long as the agent is not searching for parking space, this method gets delegated.
	 * it is assumed that agents have a maximum walking time to their activity and know the walking times in
	 * the surrounding area of their destination link.
	 * if the destination link's toNode is reachable of the current link's toNode within this time, the agent looks for a parking lot.
	 * at the moment, availabilty of parking spaces is modeled by a randomly drawn number.
	 * if the agent does not park on the destination link, a walk leg must be inserted into the plan.
	 */
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.searchMode.equals(ParkingMode.DRIVING) ) {
			return driverAgentDelegate.isWantingToArriveOnCurrentLink();
		} else {
			Link destination = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(this.getDestinationLinkId());
			Link currentLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(this.getCurrentLinkId());
//			if(destination.equals(currentLinkId)){
//				parkSearchLinkCounter  ++;
//				if(parkSearchLinkCounter > 5 ){
//					this.parkSearchLinkCounter = 0;
//					this.searchMode = ParkingMode.DRIVING;
//					log.warn("agent " + this.getId() + " arrived disenchantedly after passing activity for the 5th time :D");
//					return true;
			
			double dist = NetworkUtils.getEuclideanDistance(currentLink.getToNode().getCoord(), destination.getToNode().getCoord()) * beelineFactor;
			if( dist/walkSpeed < MAXIMUM_WALKING_TIME ){
				double rnd = Math.random();
				if( rnd <= 0.33){
					Id<Link> tripDestination = this.getDestinationLinkId();
					if(!currentLink.getId().equals(tripDestination)){
						log.info("agent " + this.getId() + " did not park on destination link, walking leg got inserted");
						Leg freefloatingLeg = (Leg) this.getCurrentPlanElement();
						freefloatingLeg.getRoute().setEndLinkId(getCurrentLinkId());
						insertWalkingLegWhenParkingElsewhere(tripDestination);
					}
					return true;
				}
			}
		}
		return false ;
	}

	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.basicAgentDelegate.getCurrentFacility();
	}

	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.basicAgentDelegate.getDestinationFacility();
	}

	public final PlanElement getPreviousPlanElement() {
		return this.basicAgentDelegate.getPreviousPlanElement();
	}

}


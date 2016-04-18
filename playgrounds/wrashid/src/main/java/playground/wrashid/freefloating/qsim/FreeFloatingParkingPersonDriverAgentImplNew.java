package playground.wrashid.freefloating.qsim;


import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingLinkInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.NetworkRoutingInclAccessEgressModule;
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
 * Current version includes:<ul>
 * <li> two-way carsharing with reservation of the vehicle at the end of the activity preceding the rental.
 * <li> one-way carsharing with each station having a parking capacity with the reservation system as the one with two-way
 * <li> free-floating carsharing with parking at the link of the next activity following free-floating trip, reservation system as the one with two-way cs.
 * <li> end of the free-floating rental is always on the link of the next activity, therefore no egress walk leg
 * </ul>
 * @author balac
 */
public class FreeFloatingParkingPersonDriverAgentImplNew implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, PTPassengerAgent {
	/**
	 * 2016-02-17 It seems that this whole class could be simplified a lot by observing the following:<ul>
	 * <li> avoid copy and paste.  There is a lot of code repetition
	 * <li> define more meaningful local variables.  This can be done by "extract local variable" re-factorings in eclipse.
	 * <li> consider using the {@link EditRoutes} infrastructure.  It does similar splicing.
	 * </ul> kai, feb'16
	 * <p/>
	 * 2016-02-18 After trying around with this, coming up with the following intuitions:<ul>
	 * <li> Code can be made easier to read, see {@link #insertFreeFloatingTripWhenEndingActivity()}.  The main device is to
	 * replace repeated chained variable lookups by local variables.  This can be done automatically: re-factor --> "extract local variable".
	 * <li> The "behavioral" {@link TripRouter} (which may try to insert additional access/egress walk legs) <i> can indeed </i> be replaced by
	 * the "computer science" {@link LeastCostPathCalculator} with the same results.  
	 * Again see see {@link #insertFreeFloatingTripWhenEndingActivity()}.
	 * <li> {@link EditRoutes} could probably be used to re-route the car leg. 
	 * <li> It should be possible to extract the agent behavior into something analog to {@link NetworkRoutingInclAccessEgressModule}.
	 * </ul> kai, feb'16
	 */

	private static final Logger log = Logger.getLogger(FreeFloatingParkingPersonDriverAgentImplNew.class);

	private ParkingModuleWithFreeFloatingCarSharing parkingModule;

	private Id vehID;

	double beelineFactor = 0.0;

	double walkSpeed = 0.0;

	private final BasicPlanAgentImpl basicAgentDelegate ;
	private final TransitAgentImpl transitAgentDelegate ;
	private final PlanBasedDriverAgentImpl driverAgentDelegate ;

	private LeastCostPathCalculator pathCalculator;

	private ParkingLinkInfo parkingSpotStart;
	private ParkingLinkInfo parkingSpotEnd;

	public FreeFloatingParkingPersonDriverAgentImplNew(final Plan plan, final Netsim simulation, 
			ParkingModuleWithFreeFloatingCarSharing parkingModule, TripRouter tripRouter, LeastCostPathCalculator pathCalculator) {
		this.pathCalculator = pathCalculator;
		Scenario scenario = simulation.getScenario() ;

		this.basicAgentDelegate = new BasicPlanAgentImpl( plan, scenario, simulation.getEventsManager(), simulation.getSimTimer() ) ;
		this.transitAgentDelegate = new TransitAgentImpl( this.basicAgentDelegate ) ;
		this.driverAgentDelegate = new PlanBasedDriverAgentImpl( this.basicAgentDelegate ) ;
		this.basicAgentDelegate.getModifiablePlan() ;
		this.parkingModule = parkingModule;

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
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("walk_ff")) {

			initializeFreeFloatingStartWalkLeg(now);			
		}		

		if (!this.getState().equals(State.ABORT))
			this.basicAgentDelegate.endActivityAndComputeNextState(now);

	}

	private void initializeFreeFloatingStartWalkLeg(double now) {
		Leg leg = (Leg)this.basicAgentDelegate.getNextPlanElement();
		Route route = leg.getRoute();
		Scenario scenario = this.basicAgentDelegate.getScenario();
		Person person = this.basicAgentDelegate.getPerson();
		parkingSpotStart = this.parkingModule.getNextFreeFloatingVehicle(
				scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), 
				person.getId(), now);		
		if (parkingSpotStart == null || parkingSpotStart.getLinkId() == null) {
			log.warn("Agent with id: " + this.getId().toString() + " was aborted because the freefloating vehicle was not avaialble or the vehicle id was not set up.");
			this.setStateToAbort(now);
			return;
			
		}
		vehID = parkingSpotStart.getVehicleId();
		Link endLink = scenario.getNetwork().getLinks().get(parkingSpotStart.getLinkId());
		
		GenericRouteImpl walkRoute = new GenericRouteImpl(route.getStartLinkId(), endLink.getId());
		final double dist = CoordUtils.calcEuclideanDistance(scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), endLink.getCoord());
		final double estimatedNetworkDistance = dist * this.beelineFactor;

		final int travTime = (int) (estimatedNetworkDistance / this.walkSpeed );
		walkRoute.setTravelTime(travTime);
		walkRoute.setDistance(estimatedNetworkDistance);	
		
		leg.setRoute(walkRoute);
		leg.setDepartureTime(now);
		leg.setTravelTime(travTime);
		
		return;		
	}
	
	private void initializeFreeFLoatingCarLeg( double now) {
		Scenario scenario = this.basicAgentDelegate.getScenario();

		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		RouteFactoryImpl routeFactory = ((PopulationFactoryImpl)pf).getRouteFactory() ;
		
		Leg leg = (Leg)this.basicAgentDelegate.getNextPlanElement();
		this.parkingModule.makeFFVehicleUnavailable(this.vehID, parkingSpotStart.getParking(), now, this.getId());
		Vehicle vehicle = null ;
		Network network = scenario.getNetwork();
		Link startLink = network.getLinks().get(this.parkingSpotStart.getLinkId());
		Link destinationLink = network.getLinks().get(leg.getRoute().getEndLinkId());
		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), destinationLink.getFromNode(), now, this.basicAgentDelegate.getPerson(), vehicle ) ;
		
		NetworkRoute carRoute = routeFactory.createRoute(NetworkRoute.class, startLink.getId(), destinationLink.getId() );
		carRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds( path.links), destinationLink.getId());
		carRoute.setTravelTime( path.travelTime );
		carRoute.setVehicleId( Id.create("FF_" + (this.vehID), Vehicle.class) ) ;
		// (yyyy this should be the same physical vehicle as the one that is removed from the link --> should NOT change the ID! kai, feb'16)

		//Leg carLeg = pf.createLeg("freefloating");
		leg.setTravelTime( path.travelTime );
		leg.setRoute(carRoute);		
			
		return;			
			
	}
	
	private void initializeFreeFLoatingParkingCarLeg(double now) {
		
		Scenario scenario = this.basicAgentDelegate.getScenario();
		Person person = this.basicAgentDelegate.getPerson();
		Leg leg = (Leg)this.basicAgentDelegate.getNextPlanElement();
		//create route for the car part of the freefloating trip
		parkingSpotEnd = parkingModule.parkFreeFloatingVehicle(Id.create(vehID.toString(), Vehicle.class),
				scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getCoord(),
				person.getId(), now);
		
		

		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		RouteFactoryImpl routeFactory = ((PopulationFactoryImpl)pf).getRouteFactory() ;
		
		Vehicle vehicle = null ;
		Network network = scenario.getNetwork();
		Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
		Link destinationLink = network.getLinks().get(parkingSpotEnd.getLinkId());
		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), destinationLink.getFromNode(), now, this.basicAgentDelegate.getPerson(), vehicle ) ;
		
		NetworkRoute carRoute = routeFactory.createRoute(NetworkRoute.class, startLink.getId(), destinationLink.getId() );
		carRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds( path.links), destinationLink.getId());
		carRoute.setTravelTime( path.travelTime );
		carRoute.setVehicleId( Id.create("FF_" + (this.vehID), Vehicle.class) ) ;
		// (yyyy this should be the same physical vehicle as the one that is removed from the link --> should NOT change the ID! kai, feb'16)

		leg.setTravelTime( path.travelTime );
		leg.setRoute(carRoute);		
			
			
		return;
			
			
	}
	
	private void initializeFreeFloatingEndWalkLeg(double now) {
		
		this.parkingModule.makeFFVehicleAvailable(Id.create((vehID.toString()), Vehicle.class), parkingSpotEnd.getParking());
		
		Leg leg = (Leg)this.basicAgentDelegate.getNextPlanElement();
		Route route = leg.getRoute();
		Scenario scenario = this.basicAgentDelegate.getScenario();
		Network network = scenario.getNetwork();
		Link startLink = network.getLinks().get(parkingSpotEnd.getLinkId());
		Link destinationLink = network.getLinks().get(route.getEndLinkId());
		GenericRouteImpl walkRoute = new GenericRouteImpl(startLink.getId(), destinationLink.getId());
		final double dist = CoordUtils.calcEuclideanDistance(startLink.getCoord(), destinationLink.getCoord());
		final double estimatedNetworkDistance = dist * this.beelineFactor;

		final int travTime = (int) (estimatedNetworkDistance / this.walkSpeed );
		walkRoute.setTravelTime(travTime);
		walkRoute.setDistance(estimatedNetworkDistance);	
		
		leg.setRoute(walkRoute);
		leg.setDepartureTime(now);
		leg.setTravelTime(travTime);
		
		return;	
	}
	
	

	

	@Override
	public final void endLegAndComputeNextState(final double now) {
		PlanElement pe = this.basicAgentDelegate.getNextPlanElement();
		if (pe instanceof Leg) {
			
			if (((Leg) pe).getMode().equals("freefloating"))
				this.initializeFreeFLoatingCarLeg(now);
			else if (((Leg) pe).getMode().equals("freefloatingparking"))
				this.initializeFreeFLoatingParkingCarLeg(now);
			else if (((Leg) pe).getMode().equals("walk_ff"))
				this.initializeFreeFloatingEndWalkLeg(now);
			
		}				

		this.basicAgentDelegate.endLegAndComputeNextState(now);

	}	

	
	//the end of added methods	

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

	// ####################################################################
	// only pure delegate methods below this line

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
	public boolean isWantingToArriveOnCurrentLink() {
		return this.driverAgentDelegate.isWantingToArriveOnCurrentLink() ;
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

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.driverAgentDelegate.chooseNextLinkId() ;
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

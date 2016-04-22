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
 * Current version includes:<ul>
 * <li> two-way carsharing with reservation of the vehicle at the end of the activity preceding the rental.
 * <li> one-way carsharing with each station having a parking capacity with the reservation system as the one with two-way
 * <li> free-floating carsharing with parking at the link of the next activity following free-floating trip, reservation system as the one with two-way cs.
 * <li> end of the free-floating rental is always on the link of the next activity, therefore no egress walk leg
 * </ul>
 * @author balac
 */
public class ParkSearchAndCarsharingPersonDriverAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, PTPassengerAgent {
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



	private static final Logger log = Logger.getLogger(ParkSearchAndCarsharingPersonDriverAgentImpl.class);

	private OneWayCarsharingStation startStationOW;
	private OneWayCarsharingStation endStationOW;

	private ArrayList<TwoWayCarsharingStation> pickupStations = new ArrayList<TwoWayCarsharingStation>();
	private ArrayList<String> twcsVehicleIDs = new ArrayList<String>();
	private Map<Id<Link>, Id<Vehicle>> vehicleIdLocation = new HashMap<Id<Link>, Id<Vehicle>>();

	private CarSharingVehicles carSharingVehicles;


	private String ffVehId;
	private String owVehId;
	private String twVehId;

	double beelineFactor = 0.0;

	double walkSpeed = 0.0;

	private TripRouter tripRouter;

	private final BasicPlanAgentImpl basicAgentDelegate ;
	private final TransitAgentImpl transitAgentDelegate ;
	private final PlanBasedDriverAgentImpl driverAgentDelegate ;

	private LeastCostPathCalculator pathCalculator;

//	private boolean searchMode = false;
	private ParkingMode searchMode = ParkingMode.DRIVING;

	private int parkSearchLinkCounter = 0;


	public ParkSearchAndCarsharingPersonDriverAgentImpl(final Plan plan, final Netsim simulation, 
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
		else if (this.basicAgentDelegate.getNextPlanElement() instanceof Leg && 
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("onewaycarsharing")) {

			insertOneWayCarsharingTripWhenEndingActivity(now);
		}

		else if (this.basicAgentDelegate.getNextPlanElement() instanceof Leg && 
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("twowaycarsharing")) {

			insertRoundTripCarsharingTripWhenEndingActivity(now);
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

	private void insertOneWayCarsharingTripWhenEndingActivity(double now) {

		List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();
		int indexOfInsertion = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;

		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ((Leg)this.basicAgentDelegate.getNextPlanElement()).getRoute();
		OneWayCarsharingStation station = findClosestAvailableOWCar(route.getStartLinkId());

		if (station == null) {
			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ow"));

			return;

		}

		GenericRouteImpl routeStart = new GenericRouteImpl(((Activity)this.basicAgentDelegate.getCurrentPlanElement()).getLinkId(),
				station.getLinkId());

		startStationOW = station;
		owVehId = station.getIDs().get(0);
		this.carSharingVehicles.getOneWayVehicles().removeVehicle(station, owVehId);
		routeStart.setTravelTime( ((CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startStationOW.getCoord()) * beelineFactor) / walkSpeed));
		
		routeStart.setDistance(CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startStationOW.getCoord()) * beelineFactor);	

		final Leg legWalkStart = new LegImpl( "walk_ow_sb" );
		legWalkStart.setRoute(routeStart);

		final List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( legWalkStart );

		//adding vehicle part of the onewaycarsharing trip

		endStationOW = findClosestAvailableParkingSpace(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()));

		if (endStationOW == null) {

			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoParkingSpaceEvent(now, route.getEndLinkId(), "ow"));

			return;
		}


		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();

		DummyFacility dummyStartFacility = new DummyFacility(new Coord(startStationOW.getCoord().getX(), 
				startStationOW.getCoord().getY()), startStationOW.getLinkId());

		DummyFacility dummyEndFacility = new DummyFacility(new Coord(endStationOW.getCoord().getX(),
				endStationOW.getCoord().getY() ), endStationOW.getLinkId());

		for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {
			// yyyy the following is memorizing only the last PlanElement that the router generates.  Not sure why this is necessary.
			// In practice, the router seems to generate exactly one plan element and so there is no problem, but the code remains obscure.
			// kai, may'15

			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
				travelTime += ((Leg) pe1).getTravelTime();
			}
		}

		Leg carLeg = new LegImpl("onewaycarsharing");

		carLeg.setTravelTime( travelTime );

		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, startStationOW.getLinkId(), endStationOW.getLinkId());

		routeCar.setLinkIds( startStationOW.getLinkId(), ids, endStationOW.getLinkId());
		routeCar.setTravelTime( travelTime);

		Id<Vehicle> vehId = null ;

		vehId = Id.create("OW_" + (owVehId), Vehicle.class);

		routeCar.setVehicleId( vehId ) ;
		carLeg.setRoute(routeCar);
		trip.add(carLeg);

		//adding eggress walk leg

		final Leg legWalkEnd = new LegImpl( "walk_ow_sb" );
		GenericRouteImpl routeEnd = new GenericRouteImpl(endStationOW.getLinkId(),
				route.getEndLinkId());

	    routeEnd.setTravelTime( ((CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(endStationOW.getLinkId()).getCoord(), 
	    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor) / walkSpeed));
		
	    routeEnd.setDistance(CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(endStationOW.getLinkId()).getCoord(),
				this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor);	

		legWalkEnd.setRoute(routeEnd);
		trip.add( legWalkEnd );

		planElements.remove(this.basicAgentDelegate.getNextPlanElement());
		planElements.addAll(indexOfInsertion, trip);


	}

	private void insertRoundTripCarsharingTripWhenEndingActivity(double now) {

		//TODO: throw exception if the next planElement is not a leg		
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ((Leg)this.basicAgentDelegate.getNextPlanElement()).getRoute();

		List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();

		int indexOfInsertion = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;

		final List<PlanElement> trip = new ArrayList<PlanElement>();
		if (hasCSVehicleAtLink(route.getStartLinkId())) {
			//log.info("person has available tw car :" + basicAgentDelegate.getPerson().getId());

			if (willUseTheVehicleLater(route.getEndLinkId())) {

				//log.info("person will use the car later:" + basicAgentDelegate.getPerson().getId());

				double travelTime = 0.0;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();

				DummyFacility dummyStartFacility = new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId());

				DummyFacility dummyEndFacility = new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

				for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {
					// yyyy the following is memorizing only the last PlanElement that the router generates.  Not sure why this is necessary.
					// In practice, the router seems to generate exactly one plan element and so there is no problem, but the code remains obscure.
					// kai, may'15

					if (pe1 instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
						travelTime += ((Leg) pe1).getTravelTime();
					}
				}

				Leg carLeg = new LegImpl("twowaycarsharing");

				carLeg.setTravelTime( travelTime );

				Scenario scenario = this.basicAgentDelegate.getScenario() ;
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

				routeCar.setLinkIds( this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(), ids, 
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
				routeCar.setTravelTime( travelTime);

				Id<Vehicle> vehId = this.vehicleIdLocation.get(route.getStartLinkId());

				routeCar.setVehicleId( vehId ) ;
				carLeg.setRoute(routeCar);
				trip.add(carLeg);

			}

			else {
				//log.info("person will not use the car later:" + basicAgentDelegate.getPerson().getId());

				double travelTime = 0.0;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();

				DummyFacility dummyStartFacility = new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord().getX(), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId());
				//TODO: get the station where the car was picked up and create the new route
				DummyFacility dummyEndFacility = new DummyFacility(new Coord(this.pickupStations.get(this.pickupStations.size() - 1).getCoord().getX(),
						this.pickupStations.get(this.pickupStations.size() - 1).getCoord().getY()), this.pickupStations.get(this.pickupStations.size() - 1).getLinkId());

				for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {
					// yyyy the following is memorizing only the last PlanElement that the router generates.  Not sure why this is necessary.
					// In practice, the router seems to generate exactly one plan element and so there is no problem, but the code remains obscure.
					// kai, may'15

					if (pe1 instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
						travelTime += ((Leg) pe1).getTravelTime();
					}
				}

				Leg carLeg = new LegImpl("twowaycarsharing");

				carLeg.setTravelTime( travelTime );

				Scenario scenario = this.basicAgentDelegate.getScenario() ;
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(),
						this.pickupStations.get(this.pickupStations.size() - 1).getLinkId());

				routeCar.setLinkIds( this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(), ids, 
						this.pickupStations.get(this.pickupStations.size() - 1).getLinkId());
				routeCar.setTravelTime( travelTime);

				Id<Vehicle> vehId = this.vehicleIdLocation.get(route.getStartLinkId());

				routeCar.setVehicleId( vehId ) ;
				carLeg.setRoute(routeCar);
				trip.add(carLeg);

				final Leg legWalkEnd = new LegImpl( "walk_rb" );
				//TODO: get start link from the station
				GenericRouteImpl routeEnd = new GenericRouteImpl(null,
						route.getEndLinkId());

			    routeEnd.setTravelTime( ((CoordUtils.calcEuclideanDistance(this.pickupStations.get(this.pickupStations.size() - 1).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor) / walkSpeed));
				
			    routeEnd.setDistance(CoordUtils.calcEuclideanDistance(this.pickupStations.get(this.pickupStations.size() - 1).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor);	

				legWalkEnd.setRoute(routeEnd);
				trip.add( legWalkEnd );

			}

		}

		else {

			if (willUseTheVehicleLater(route.getEndLinkId())) {

				//log.info("Person with an id: "   + basicAgentDelegate.getPerson().getId()
				//		+ " does not have a tw car.");

				final Leg legWalkEnd = new LegImpl( "walk_rb" );

				TwoWayCarsharingStation pickUpStation = this.findClosestAvailableTWCar(route.getStartLinkId());

				if (pickUpStation == null) {
					this.setStateToAbort(now);
					this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "tw"));
					return;

				}
				this.pickupStations.add(pickUpStation);
				this.twcsVehicleIDs.add(pickUpStation.getIDs().get(0));
				GenericRouteImpl routeEnd = new GenericRouteImpl(route.getStartLinkId(),
						pickUpStation.getLinkId());
	
			    routeEnd.setTravelTime( ((CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord()) * beelineFactor) / walkSpeed));
				
			    routeEnd.setDistance(CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord()) * beelineFactor);	
	
				legWalkEnd.setRoute(routeEnd);
				trip.add( legWalkEnd );

				//vehicle leg of the round-trip carsharing trip

				double travelTime = 0.0;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();

				DummyFacility dummyStartFacility =new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getId());
				//TODO: get the station where the car was picked up and create the new route
				DummyFacility dummyEndFacility = new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

				for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {
					// yyyy the following is memorizing only the last PlanElement that the router generates.  Not sure why this is necessary.
					// In practice, the router seems to generate exactly one plan element and so there is no problem, but the code remains obscure.
					// kai, may'15

					if (pe1 instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
						travelTime += ((Leg) pe1).getTravelTime();
					}
				}

				Leg carLeg = new LegImpl("twowaycarsharing");

				carLeg.setTravelTime( travelTime );

				Scenario scenario = this.basicAgentDelegate.getScenario() ;
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, pickUpStation.getLinkId(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

				routeCar.setLinkIds( pickUpStation.getLinkId(), ids, 
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
				routeCar.setTravelTime( travelTime);

				//log.info("Rented twowaycarsharing car has an id: " + pickUpStation.getIDs().get(0));
				Id<Vehicle> vehId = Id.create("TW_" + (pickUpStation.getIDs().get(0)), Vehicle.class);
				this.carSharingVehicles.getTwoWayVehicles().removeVehicle(pickUpStation, pickUpStation.getIDs().get(0));

				routeCar.setVehicleId( vehId ) ;
				carLeg.setRoute(routeCar);
				trip.add(carLeg);
			}

			else {

				//log.info("Two way carsharing trip is assigned to a leg between the same locations!");

				final Leg legWalkStart = new LegImpl( "walk_rb" );

				TwoWayCarsharingStation pickUpStation = this.findClosestAvailableTWCar(route.getStartLinkId());

				if (pickUpStation == null) {
					this.setStateToAbort(now);
					this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "tw"));
					return;

				}
				this.pickupStations.add(pickUpStation);
				this.twcsVehicleIDs.add(pickUpStation.getIDs().get(0));
				GenericRouteImpl routeWalkStart = new GenericRouteImpl(route.getStartLinkId(),
						pickUpStation.getLinkId());
	
				routeWalkStart.setTravelTime( ((CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord()) * beelineFactor) / walkSpeed));
				
				routeWalkStart.setDistance(CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord()) * beelineFactor);	
	
			    legWalkStart.setRoute(routeWalkStart);
				trip.add( legWalkStart );

				//vehicle leg of the round-trip carsharing trip

				double travelTime = 0.0;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();

				DummyFacility dummyStartFacility =new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getId());
				//TODO: get the station where the car was picked up and create the new route
				DummyFacility dummyEndFacility = new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

				for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {
					// yyyy the following is memorizing only the last PlanElement that the router generates.  Not sure why this is necessary.
					// In practice, the router seems to generate exactly one plan element and so there is no problem, but the code remains obscure.
					// kai, may'15

					if (pe1 instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
						travelTime += ((Leg) pe1).getTravelTime();
					}
				}

				Leg carLeg = new LegImpl("twowaycarsharing");

				carLeg.setTravelTime( travelTime );

				Scenario scenario = this.basicAgentDelegate.getScenario() ;
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getRouteFactory().createRoute(NetworkRoute.class, pickUpStation.getLinkId(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

				routeCar.setLinkIds( pickUpStation.getLinkId(), ids, 
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
				routeCar.setTravelTime( travelTime);

				//log.info("Rented twowaycarsharing car has an id: " + pickUpStation.getIDs().get(0));
				Id<Vehicle> vehId = Id.create("TW_" + (pickUpStation.getIDs().get(0)), Vehicle.class);
				this.carSharingVehicles.getTwoWayVehicles().removeVehicle(pickUpStation, pickUpStation.getIDs().get(0));

				routeCar.setVehicleId( vehId ) ;
				carLeg.setRoute(routeCar);
				trip.add(carLeg);

				final Leg legWalkEnd = new LegImpl( "walk_rb" );

				GenericRouteImpl routeWalkEnd = new GenericRouteImpl(pickUpStation.getLinkId(),
						route.getEndLinkId());
	
				routeWalkEnd.setTravelTime( ((CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord()) * beelineFactor) / walkSpeed));
				
				routeWalkEnd.setDistance(CoordUtils.calcEuclideanDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLinkId()).getCoord()) * beelineFactor);	
	
				legWalkEnd.setRoute(routeWalkEnd);
				trip.add( legWalkEnd );



			}


		}

		planElements.remove(this.basicAgentDelegate.getNextPlanElement());
		planElements.addAll(indexOfInsertion, trip);

	}

	private boolean willUseTheVehicleLater(Id<Link> linkId) {
		boolean willUseVehicle = false;

		List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();

		int index = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;

		for (int i = index; i < planElements.size(); i++) {

			if (planElements.get(i) instanceof Leg) {

				if (((Leg)planElements.get(i)).getMode().equals("twowaycarsharing")) {

					if (((Leg)planElements.get(i)).getRoute().getStartLinkId().toString().equals(linkId.toString())) {

						willUseVehicle = true;
					}
				}

			}
		}

		return willUseVehicle;
	}

	private boolean hasCSVehicleAtLink(Id<Link> linkId) {
		boolean hasVehicle = false;

		if (this.vehicleIdLocation.containsKey(linkId))
			hasVehicle = true;

		return hasVehicle;
	}

	@Override
	public final void endLegAndComputeNextState(final double now) {

		parkCSVehicle( );			

		this.basicAgentDelegate.endLegAndComputeNextState(now);

	}	


	private void parkCSVehicle() {
		Leg currentLeg = (Leg) this.basicAgentDelegate.getCurrentPlanElement() ;
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		if (currentLeg.getMode().equals("onewaycarsharing")) {

			this.carSharingVehicles.getOneWayVehicles().addVehicle(endStationOW, owVehId);
			owVehId = null;
		}
		else if (currentLeg.getMode().equals("twowaycarsharing") 

				&& this.basicAgentDelegate.getNextPlanElement() instanceof Leg
				) {

			if (((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("walk_rb")) {
				this.vehicleIdLocation.remove(currentLeg.getRoute().getStartLinkId());
				this.carSharingVehicles.getTwoWayVehicles().addVehicle(this.pickupStations.get(this.pickupStations.size() - 1), 
						twcsVehicleIDs.get(twcsVehicleIDs.size() - 1));
				this.pickupStations.remove(this.pickupStations.size() - 1);

				this.twcsVehicleIDs.remove(twcsVehicleIDs.size() - 1);
			}
		}
		else if (currentLeg.getMode().equals("twowaycarsharing")) {
			this.vehicleIdLocation.remove(currentLeg.getRoute().getStartLinkId());

			this.vehicleIdLocation.put(currentLeg.getRoute().getEndLinkId(), ((LinkNetworkRouteImpl)currentLeg.getRoute()).getVehicleId());
		}
		else if (currentLeg.getMode().equals("freefloating")) {

			this.carSharingVehicles.getFreeFLoatingVehicles().addVehicle(scenario.getNetwork().getLinks().get(this.getDestinationLinkId()), ffVehId);
			ffVehId = null;
		}


	}


	//added methods


	private TwoWayCarsharingStation findClosestAvailableTWCar(Id<Link> linkId) {
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		//if no cars within certain radius return null
		Link link = scenario.getNetwork().getLinks().get(linkId);

		Collection<TwoWayCarsharingStation> location = this.carSharingVehicles.getTwoWayVehicles().getQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), Double.parseDouble(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing")));
		if (location.isEmpty()) return null;
		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing"));
		TwoWayCarsharingStation closest = null;
		for(TwoWayCarsharingStation station: location) {
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), station.getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), station.getCoord());
			}			

		}

		return closest;

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

	private OneWayCarsharingStation findClosestAvailableOWCar(Id<Link> linkId) {
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		//find the closest available car and reserve it (make it unavailable)
		//if no cars within certain radius return null
		Link link = scenario.getNetwork().getLinks().get(linkId);
		final OneWayCarsharingConfigGroup oneWayCSConfig = (OneWayCarsharingConfigGroup) scenario.getConfig().getModule("OneWayCarsharing");
		double distanceSearch = oneWayCSConfig.getsearchDistance() ;

		Collection<OneWayCarsharingStation> location = this.carSharingVehicles.getOneWayVehicles().getQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		OneWayCarsharingStation closest = null;
		for(OneWayCarsharingStation station: location) {
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), station.getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), station.getCoord());
			}			

		}			

		return closest;


	}

	private OneWayCarsharingStation findClosestAvailableParkingSpace(Link link) {
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		//find the closest available parking space and reserve it (make it unavailable)
		//if there are no parking spots within search radius, return null

		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("OneWayCarsharing").getParams().get("searchDistanceOneWayCarsharing"));

		Collection<OneWayCarsharingStation> location = this.carSharingVehicles.getOneWayVehicles().getQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		OneWayCarsharingStation closest = null;
		for(OneWayCarsharingStation station: location) {
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), station.getCoord()) < distanceSearch && station.getNumberOfAvailableParkingSpaces() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcEuclideanDistance(link.getCoord(), station.getCoord());
			}			

		}		

		return closest;

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
		if ( !this.searchMode.equals(ParkingMode.SEARCHING)) {
			Id<Link> destination = this.getDestinationLinkId();
			Id<Link> nextId =  driverAgentDelegate.chooseNextLinkId();
			if(nextId.equals(destination)){
				log.warn("set agent " + this.getId() + " on searching mode after wanting to drive on link " + nextId);
				this.searchMode = ParkingMode.SEARCHING;
				return chooseNextLinkId();
		}
		return nextId;
	} else {

		Link currentLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get( this.getCurrentLinkId() ) ;
		Node toNode = currentLink.getToNode() ;
		int idx = MatsimRandom.getRandom().nextInt( toNode.getOutLinks().size() ) ;
		for ( Link outLink : toNode.getOutLinks().values() ) {
			if ( idx==0 ) {
				return outLink.getId() ;
			}
			idx-- ;
		}
		throw new RuntimeException("should not happen" ) ;
	}
	}
	
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		if ( this.searchMode.equals(ParkingMode.DRIVING) ) {
			return driverAgentDelegate.isWantingToArriveOnCurrentLink();
		} else {
			Id<Link> destination = this.getDestinationLinkId();
			Id<Link> currentLinkId = this.getCurrentLinkId();
			if(destination.equals(currentLinkId)){
				parkSearchLinkCounter  ++;
				if(parkSearchLinkCounter > 5 ){
					this.parkSearchLinkCounter = 0;
					this.searchMode = ParkingMode.DRIVING;
					log.warn("agent " + this.getId() + " arrived disenchantedly after passing activity for the 5th time :D");
					return true;
				}
			}
			return false ;
		}
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

enum ParkingMode{
	SEARCHING,
	DRIVING,
}


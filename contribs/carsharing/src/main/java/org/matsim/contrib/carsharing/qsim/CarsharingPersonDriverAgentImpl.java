package org.matsim.contrib.carsharing.qsim;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.events.NoParkingSpaceEvent;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.facility.DummyFacility;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
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
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;



/**
 * Current version includes:
 * -- two-way carsharing with reservation of the vehicle at the end of the activity preceding the rental.
 * -- one-way carsharing with each station having a parking capacity with the reservation system as the one with two-way
 * -- free-floating carsharing with parking at the link of the next activity following free-floating trip, reservation system as the one with two-way cs.
 * -- end of the free-floating rental is always on the link of the next activity, therefore no egress walk leg
 * @author balac
 */


public class CarsharingPersonDriverAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, PTPassengerAgent {

	private static final Logger log = Logger.getLogger(CarsharingPersonDriverAgentImpl.class);

	private Link startLinkFF;

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


	public CarsharingPersonDriverAgentImpl(final Plan plan, final Netsim simulation, 
			CarSharingVehicles carSharingVehicles, TripRouter tripRouter) {
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
		
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	

	@Override
	public final void endActivityAndComputeNextState(final double now) {
		
		
		if (this.basicAgentDelegate.getNextPlanElement() instanceof Leg && 
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("freefloating")) {
			
			insertFreeFloatingTrip(now);			
		}
		else if (this.basicAgentDelegate.getNextPlanElement() instanceof Leg && 
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("onewaycarsharing")) {
			
			insertOneWayCarsharingTrip(now);
		}
		
		else if (this.basicAgentDelegate.getNextPlanElement() instanceof Leg && 
				((Leg)this.basicAgentDelegate.getNextPlanElement()).getMode().equals("twowaycarsharing")) {
			
			insertRoundTripCarsharingTrip(now);
		}
		
		if (!this.getState().equals(State.ABORT))
			this.basicAgentDelegate.endActivityAndComputeNextState(now);

	}

	private void insertFreeFloatingTrip(double now) {
		List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();
		
		int indexOfInsertion = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		
		final Leg leg = new LegImpl( "walk_ff" );
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ((Leg)this.basicAgentDelegate.getNextPlanElement()).getRoute();

		
		FreeFloatingStation location = findClosestAvailableCar(route.getStartLinkId());

		if (location == null) {
			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ff"));
			return;
		}
		ffVehId = location.getIDs().get(0);
		this.carSharingVehicles.getFreeFLoatingVehicles().removeVehicle(location.getLink(), ffVehId);
		startLinkFF = location.getLink();

		GenericRouteImpl routeStart = new GenericRouteImpl(((Activity)this.basicAgentDelegate.getCurrentPlanElement()).getLinkId(),
				startLinkFF.getId());
		routeStart.setTravelTime( ((CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startLinkFF.getCoord()) * beelineFactor) / walkSpeed));
		
		routeStart.setDistance(CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startLinkFF.getCoord()) * beelineFactor);	

		
		leg.setRoute(routeStart);
		trip.add( leg );
		
		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();

		DummyFacility dummyStartFacility = new DummyFacility(new Coord(startLinkFF.getCoord().getX(), 
				startLinkFF.getCoord().getY()), startLinkFF.getId());

		DummyFacility dummyEndFacility = new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getX(), 
				this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord().getY()),
				this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

		for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {
			// yyyy the following is memorizing only the last PlanElement that the router generates.  Not sure why this is necessary.
			// In practice, the router seems to generate exactly one plan element and so there is no problem, but the code remains obscure.
			// kai, may'15
			
			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
				travelTime += ((Leg) pe1).getTravelTime();
			}
		}

		Leg carLeg = new LegImpl("freefloating");

		carLeg.setTravelTime( travelTime );

		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute(NetworkRoute.class, startLinkFF.getId(),
				 this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());

		routeCar.setLinkIds(startLinkFF.getId(), ids, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
		routeCar.setTravelTime( travelTime);

		Id<Vehicle> vehId = null ;
		
		vehId = Id.create("FF_" + (ffVehId), Vehicle.class);
		
		routeCar.setVehicleId( vehId ) ;
		carLeg.setRoute(routeCar);
		trip.add(carLeg);
			
		planElements.remove(this.basicAgentDelegate.getNextPlanElement());
		planElements.addAll(indexOfInsertion, trip);
		
	}

	private void insertOneWayCarsharingTrip(double now) {
		
		List<PlanElement> planElements = this.basicAgentDelegate.getCurrentPlan().getPlanElements();
		int indexOfInsertion = planElements.indexOf(this.basicAgentDelegate.getCurrentPlanElement()) + 1;
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		
		final Leg legWalkStart = new LegImpl( "walk_ow_sb" );
		
		
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ((Leg)this.basicAgentDelegate.getNextPlanElement()).getRoute();
		OneWayCarsharingStation station = findClosestAvailableOWCar(route.getStartLinkId());

		if (station == null) {
			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ow"));

			return;

		}
		
		GenericRouteImpl routeStart = new GenericRouteImpl(((Activity)this.basicAgentDelegate.getCurrentPlanElement()).getLinkId(),
				station.getLink().getId());
		
		startStationOW = station;
		owVehId = station.getIDs().get(0);
		this.carSharingVehicles.getOneWayVehicles().removeVehicle(station, owVehId);
		routeStart.setTravelTime( ((CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startStationOW.getLink().getCoord()) * beelineFactor) / walkSpeed));
		
		routeStart.setDistance(CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), startStationOW.getLink().getCoord()) * beelineFactor);	

		legWalkStart.setRoute(routeStart);
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

		DummyFacility dummyStartFacility = new DummyFacility(new Coord(startStationOW.getLink().getCoord().getX(), 
				startStationOW.getLink().getCoord().getY()), startStationOW.getLink().getId());

		DummyFacility dummyEndFacility = new DummyFacility(new Coord(endStationOW.getLink().getCoord().getX(),
				endStationOW.getLink().getCoord().getY() ), endStationOW.getLink().getId());

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
		LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute(NetworkRoute.class, startStationOW.getLink().getId(), endStationOW.getLink().getId());

		routeCar.setLinkIds( startStationOW.getLink().getId(), ids, endStationOW.getLink().getId());
		routeCar.setTravelTime( travelTime);

		Id<Vehicle> vehId = null ;
		
		vehId = Id.create("OW_" + (owVehId), Vehicle.class);
		
		routeCar.setVehicleId( vehId ) ;
		carLeg.setRoute(routeCar);
		trip.add(carLeg);
	
		//adding eggress walk leg
		
		final Leg legWalkEnd = new LegImpl( "walk_ow_sb" );
		GenericRouteImpl routeEnd = new GenericRouteImpl(endStationOW.getLink().getId(),
				route.getEndLinkId());

	    routeEnd.setTravelTime( ((CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(endStationOW.getLink().getId()).getCoord(), 
	    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor) / walkSpeed));
		
	    routeEnd.setDistance(CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(endStationOW.getLink().getId()).getCoord(),
				this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor);	

		legWalkEnd.setRoute(routeEnd);
		trip.add( legWalkEnd );
		
		planElements.remove(this.basicAgentDelegate.getNextPlanElement());
		planElements.addAll(indexOfInsertion, trip);
		
		
	}
	
	private void insertRoundTripCarsharingTrip(double now) {
		
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
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute(NetworkRoute.class, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(),
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
				DummyFacility dummyEndFacility = new DummyFacility(new Coord(this.pickupStations.get(this.pickupStations.size() - 1).getLink().getCoord().getX(),
						this.pickupStations.get(this.pickupStations.size() - 1).getLink().getCoord().getY()), this.pickupStations.get(this.pickupStations.size() - 1).getLink().getId());

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
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute(NetworkRoute.class, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(),
						this.pickupStations.get(this.pickupStations.size() - 1).getLink().getId());

				routeCar.setLinkIds( this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getId(), ids, 
						this.pickupStations.get(this.pickupStations.size() - 1).getLink().getId());
				routeCar.setTravelTime( travelTime);

				Id<Vehicle> vehId = this.vehicleIdLocation.get(route.getStartLinkId());
				
				routeCar.setVehicleId( vehId ) ;
				carLeg.setRoute(routeCar);
				trip.add(carLeg);
				
				final Leg legWalkEnd = new LegImpl( "walk_rb" );
				//TODO: get start link from the station
				GenericRouteImpl routeEnd = new GenericRouteImpl(null,
						route.getEndLinkId());

			    routeEnd.setTravelTime( ((CoordUtils.calcDistance(this.pickupStations.get(this.pickupStations.size() - 1).getLink().getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord()) * beelineFactor) / walkSpeed));
				
			    routeEnd.setDistance(CoordUtils.calcDistance(this.pickupStations.get(this.pickupStations.size() - 1).getLink().getCoord(),
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
						pickUpStation.getLink().getId());
	
			    routeEnd.setTravelTime( ((CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord()) * beelineFactor) / walkSpeed));
				
			    routeEnd.setDistance(CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord()) * beelineFactor);	
	
				legWalkEnd.setRoute(routeEnd);
				trip.add( legWalkEnd );
				
				//vehicle leg of the round-trip carsharing trip
				
				double travelTime = 0.0;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();
	
				DummyFacility dummyStartFacility =new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getId());
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
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute(NetworkRoute.class, pickUpStation.getLink().getId(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
	
				routeCar.setLinkIds( pickUpStation.getLink().getId(), ids, 
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
						pickUpStation.getLink().getId());
	
				routeWalkStart.setTravelTime( ((CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord()) * beelineFactor) / walkSpeed));
				
				routeWalkStart.setDistance(CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord()) * beelineFactor);	
	
			    legWalkStart.setRoute(routeWalkStart);
				trip.add( legWalkStart );
				
				//vehicle leg of the round-trip carsharing trip
				
				double travelTime = 0.0;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();
	
				DummyFacility dummyStartFacility =new DummyFacility(new Coord(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord().getX(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord().getY()), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getId());
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
				LinkNetworkRouteImpl routeCar = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute(NetworkRoute.class, pickUpStation.getLink().getId(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
	
				routeCar.setLinkIds( pickUpStation.getLink().getId(), ids, 
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getId());
				routeCar.setTravelTime( travelTime);
	
				//log.info("Rented twowaycarsharing car has an id: " + pickUpStation.getIDs().get(0));
				Id<Vehicle> vehId = Id.create("TW_" + (pickUpStation.getIDs().get(0)), Vehicle.class);
				this.carSharingVehicles.getTwoWayVehicles().removeVehicle(pickUpStation, pickUpStation.getIDs().get(0));
	
				routeCar.setVehicleId( vehId ) ;
				carLeg.setRoute(routeCar);
				trip.add(carLeg);
				
				final Leg legWalkEnd = new LegImpl( "walk_rb" );
				
				GenericRouteImpl routeWalkEnd = new GenericRouteImpl(pickUpStation.getLink().getId(),
						route.getEndLinkId());
	
				routeWalkEnd.setTravelTime( ((CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord(), 
			    		this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord()) * beelineFactor) / walkSpeed));
				
				routeWalkEnd.setDistance(CoordUtils.calcDistance(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()).getCoord(),
						this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(pickUpStation.getLink().getId()).getCoord()) * beelineFactor);	
	
				legWalkEnd.setRoute(routeWalkEnd);
				trip.add( legWalkEnd );
				
				
				
			}
			
			
		}
		
		planElements.remove(this.basicAgentDelegate.getNextPlanElement());
		planElements.addAll(indexOfInsertion, trip);
		
	}
	
	private boolean willUseTheVehicleLater(Id<Link> linkId) {
		// TODO Auto-generated method stub
		
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
			if (CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord());
			}			

		}

		return closest;

	}	

	private FreeFloatingStation findClosestAvailableCar(Id<Link> linkId) {		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		Link link = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(linkId);

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
			if (CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord());
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
			if (CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfAvailableParkingSpaces() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord());
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



}

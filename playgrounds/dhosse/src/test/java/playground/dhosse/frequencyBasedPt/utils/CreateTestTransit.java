package playground.dhosse.frequencyBasedPt.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CreateTestTransit {
	
	private static int vehicleCounter = 0;
	private static final String vehicle = "BUS_";

	public static TransitSchedule createTestSchedule(Network network, int nRoutes){
		
		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();

		TransitStopFacility stop1 = factory.createTransitStopFacility(Id.create("stop_1", TransitStopFacility.class), new Coord((double) 150, (double) 150), false);
		stop1.setLinkId(Id.create(1, Link.class));
		TransitStopFacility stop3 = factory.createTransitStopFacility(Id.create("stop_3", TransitStopFacility.class), new Coord((double) 300, (double) 150), false);
		stop3.setLinkId(Id.create(3, Link.class));
		TransitStopFacility stop5 = factory.createTransitStopFacility(Id.create("stop_5", TransitStopFacility.class), new Coord((double) 450, (double) 150), false);
		stop5.setLinkId(Id.create(5, Link.class));
		TransitStopFacility stop7 = factory.createTransitStopFacility(Id.create("stop_7", TransitStopFacility.class), new Coord((double) 300, (double) 150), false);
		stop7.setLinkId(Id.create(7, Link.class));

		TransitStopFacility stop2 = factory.createTransitStopFacility(Id.create("stop_2", TransitStopFacility.class), new Coord((double) 0, (double) 150), false);
		stop2.setLinkId(Id.create(2, Link.class));
		TransitStopFacility stop4 = factory.createTransitStopFacility(Id.create("stop_4", TransitStopFacility.class), new Coord((double) 150, (double) 150), false);
		stop4.setLinkId(Id.create(4, Link.class));
		TransitStopFacility stop6 = factory.createTransitStopFacility(Id.create("stop_6", TransitStopFacility.class), new Coord((double) 300, (double) 150), false);
		stop6.setLinkId(Id.create(6, Link.class));
		TransitStopFacility stop8 = factory.createTransitStopFacility(Id.create("stop_8", TransitStopFacility.class), new Coord((double) 450, (double) 150), false);
		stop8.setLinkId(Id.create(8, Link.class));
		
		TransitSchedule schedule = factory.createTransitSchedule();
		
		schedule.addStopFacility(stop1);
		schedule.addStopFacility(stop2);
		schedule.addStopFacility(stop3);
		schedule.addStopFacility(stop4);
		schedule.addStopFacility(stop5);
		schedule.addStopFacility(stop6);
		schedule.addStopFacility(stop7);
		schedule.addStopFacility(stop8);
		
		TransitLine transitLine = factory.createTransitLine(Id.create(1, TransitLine.class));
		
		//hinrichtung
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		routeLinkIds.add(Id.create(1, Link.class));
		routeLinkIds.add(Id.create(3, Link.class));
		routeLinkIds.add(Id.create(5, Link.class));
		routeLinkIds.add(Id.create(7, Link.class));
		NetworkRoute route1 = RouteUtils.createNetworkRoute(routeLinkIds, network);
		
		//rückrichtung
//		routeLinkIds = new ArrayList<Id>();
//		routeLinkIds.add(Id.create(8));
//		routeLinkIds.add(Id.create(6));
//		routeLinkIds.add(Id.create(4));
//		routeLinkIds.add(Id.create(2));
//		NetworkRoute route2 = RouteUtils.createNetworkRoute(routeLinkIds, network);
		
		//hinrichtung
		List<TransitRouteStop> transitRouteStops1 = new ArrayList<TransitRouteStop>();
		TransitRouteStop stop_1 = factory.createTransitRouteStop(stop1, 0., 0.);
		stop_1.setAwaitDepartureTime(true);
		transitRouteStops1.add(stop_1);
		TransitRouteStop stop_3 = factory.createTransitRouteStop(stop3, 10., 15.);
		stop_1.setAwaitDepartureTime(true);
		transitRouteStops1.add(stop_3);
		TransitRouteStop stop_5 = factory.createTransitRouteStop(stop5, 25., 30.);
		stop_1.setAwaitDepartureTime(true);
		transitRouteStops1.add(stop_5);
		TransitRouteStop stop_7 = factory.createTransitRouteStop(stop7, 40., 40.);
		stop_1.setAwaitDepartureTime(true);
		transitRouteStops1.add(stop_7);
		
		//rückrichtung
//		List<TransitRouteStop> transitRouteStops2 = new ArrayList<TransitRouteStop>();
//		TransitRouteStop stop_8 = factory.createTransitRouteStop(stop8, 0., 0.);
//		stop_8.setAwaitDepartureTime(true);
//		transitRouteStops2.add(stop_8);
//		TransitRouteStop stop_6 = factory.createTransitRouteStop(stop6, 10., 15.);
//		stop_6.setAwaitDepartureTime(true);
//		transitRouteStops2.add(stop_6);
//		TransitRouteStop stop_4 = factory.createTransitRouteStop(stop4, 25., 30.);
//		stop_4.setAwaitDepartureTime(true);
//		transitRouteStops2.add(stop_4);
//		TransitRouteStop stop_2 = factory.createTransitRouteStop(stop2, 40., 40.);
//		stop_2.setAwaitDepartureTime(true);
//		transitRouteStops2.add(stop_2);
		
		
		//hinrichtung
		for(int i = 0; i < nRoutes; i++){
			
			int departureCounter = 0;
			
			TransitRoute transitRoute = factory.createTransitRoute(Id.create("route_h_"+i, TransitRoute.class), route1, transitRouteStops1, TransportMode.pt);
			
			Departure dep1 = factory.createDeparture(Id.create(departureCounter, Departure.class), i*3600);
			dep1.setVehicleId(Id.create(vehicle + vehicleCounter, Vehicle.class));
//			Departure dep2 = factory.createDeparture(Id.create("dep_6"), 6*3600);
//			dep1.setVehicleId(Id.create(vehicle + vehicleCounter+1));
//			Departure dep3 = factory.createDeparture(Id.create("dep_6,5"), 6*3600+30*60);
//			dep1.setVehicleId(Id.create(vehicle + vehicleCounter+2));
			
			transitRoute.addDeparture(dep1);
//			transitRoute.addDeparture(dep2);
//			transitRoute.addDeparture(dep3);
			
			vehicleCounter++;
			
			transitLine.addRoute(transitRoute);
			
		}
		
		schedule.addTransitLine(transitLine);
		
		//rückrichtung
//		for(int i = 0; i < nRoutes; i++){
//					
//			TransitRoute transitRoute = factory.createTransitRoute(Id.create("route_r_"+i), route2, transitRouteStops2, TransportMode.pt);
//					
//			Departure dep1 = factory.createDeparture(Id.create("dep_17,5"), 17*3600+30*60);
//			dep1.setVehicleId(Id.create(vehicle + vehicleCounter));
//			Departure dep2 = factory.createDeparture(Id.create("dep_18"), 18*3600);
//			dep1.setVehicleId(Id.create(vehicle + vehicleCounter+1));
//			Departure dep3 = factory.createDeparture(Id.create("dep_18,5"), 18*3600+30*60);
//			dep1.setVehicleId(Id.create(vehicle + vehicleCounter+2));
//					
//			transitRoute.addDeparture(dep1);
//			transitRoute.addDeparture(dep2);
//			transitRoute.addDeparture(dep3);
//			
//			vehicleCounter += 3;
//			
//			transitLine.addRoute(transitRoute);
//				
//		}
		
		return schedule;
		
	}
	
	public static Vehicles createTestTransitVehicles(){
		
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		
		VehicleType typeBus = new VehicleTypeImpl(Id.create("bus", VehicleType.class));
		typeBus.setLength(12.);
		typeBus.setWidth(2.5);
		typeBus.setMaximumVelocity(80/3.6);
		
		VehicleCapacityImpl capacityBus = new VehicleCapacityImpl();
		capacityBus.setSeats(33);
		capacityBus.setStandingRoom(58);
		
		typeBus.setCapacity(capacityBus);
		typeBus.setAccessTime(4.);
		typeBus.setEgressTime(2.);
		
		vehicles.addVehicleType( typeBus);
		
		for(int i = 0; i < vehicleCounter; i++){
			Vehicle bus = new VehicleImpl(Id.create(vehicle + i, Vehicle.class),typeBus);
			vehicles.addVehicle( bus);
		}
		
		return vehicles;
		
	}
	
}

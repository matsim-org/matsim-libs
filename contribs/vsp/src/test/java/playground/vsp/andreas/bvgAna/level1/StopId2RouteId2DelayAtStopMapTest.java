package playground.vsp.andreas.bvgAna.level1;

import junit.framework.Assert;
import playground.vsp.andreas.bvgAna.level1.StopId2RouteId2DelayAtStopMap;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class StopId2RouteId2DelayAtStopMapTest {
	
	@Test
	public void testStopId2RouteId2DelayAtStopMap() {
	    
//	    assign Ids to routes, vehicles and agents to be used in Test
		
			Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
	    Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
	    Id<Person> driverId1 = Id.create(4, Person.class);
	    Id<Person> driverId2 = Id.create(5, Person.class);
	    Id<Departure> departureId1 = Id.create(6, Departure.class);
	    Id<Departure> departureId2 = Id.create(7, Departure.class);
	    Id<TransitRoute> transitRouteId1 = Id.create(14, TransitRoute.class);
	    Id<TransitRoute> transitRouteId2 = Id.create(2, TransitRoute.class);
	    Id<TransitLine> transitLineId1 = Id.create(4, TransitLine.class);
	    Id<TransitLine> transitLineId2 = Id.create(6, TransitLine.class);
	    Id<TransitStopFacility> facilityId1 = Id.create(8, TransitStopFacility.class);
	    Id<TransitStopFacility> facilityId2 = Id.create(9, TransitStopFacility.class);
	    
//	    create events
	    
	    VehicleDepartsAtFacilityEvent event1 = new VehicleDepartsAtFacilityEvent(2., vehId1, facilityId1, 0.5);
	    VehicleDepartsAtFacilityEvent event2 = new VehicleDepartsAtFacilityEvent(2.7, vehId2, facilityId2, 0.7);
	    TransitDriverStartsEvent event3 = new TransitDriverStartsEvent(2.8, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
	    TransitDriverStartsEvent event4 = new TransitDriverStartsEvent(2.3, driverId2, vehId2, transitLineId2, transitRouteId2, departureId2);
	    
	    StopId2RouteId2DelayAtStopMap test = new StopId2RouteId2DelayAtStopMap();
	    
	    test.handleEvent(event3);
	    test.handleEvent(event4);
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	   	   
	    
	    // to be completed
	    
	    /**
	     * @TODO complete tests, first tests working now
	     */
	    
//	    System.out.println(test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).toString());
//	    
//	    System.out.println(test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getLineId());
	    
	    Assert.assertEquals(transitLineId1, test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getLineId());
	    
	    Assert.assertEquals(transitRouteId1, test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getRouteId());
	    
	    Assert.assertEquals(1, test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).get(transitRouteId1).getRealizedDepartures().size());
	    
	    
	    
	    
//	    String test1 = "{"+transitRouteId1+"=Stop: "+event1.getFacilityId()+", Line: "+transitLineId1+", Route: "+transitRouteId1+", # planned Departures: 1, # realized Departures: 1}";
//	    System.out.println(test1);
//	    
//	    System.out.println(test.getStopId2RouteId2DelayAtStopMap().get(event2.getFacilityId()).toString());
	    

	    
	    
	    
	    
	}



}

package playground.vsp.andreas.bvgAna.level1;

import junit.framework.Assert;
import playground.vsp.andreas.bvgAna.level1.StopId2LineId2Pulk;

import org.junit.Ignore;
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

public class StopId2LineId2PulkTest {

	@Test @Ignore
	public void testStopId2LineId2Pulk() {

//	    assign Ids to routes, vehicles and agents to be used in Test
	    
	    Id<Person> driverId1 = Id.create(1, Person.class);
	    Id<Vehicle> vehicleId1 = Id.create(2, Vehicle.class);
	    Id<Person> driverId2 = Id.create(3, Person.class);
	    Id<Vehicle> vehicleId2 = Id.create(4, Vehicle.class);
	    Id<TransitLine> transitLineId1 = Id.create(4, TransitLine.class);
	    Id<TransitRoute> transitRouteId1 = Id.create(5, TransitRoute.class);
	    Id<Departure> departureId1 = Id.create(6, Departure.class);
	    Id<Departure> departureId2 = Id.create(8, Departure.class);
	    Id<TransitStopFacility> facilityId1 = Id.create(7, TransitStopFacility.class);


//	    create events
	    
	    TransitDriverStartsEvent event1 = new TransitDriverStartsEvent(1.0, driverId1, vehicleId1, transitLineId1, transitRouteId1, departureId1);
	    VehicleDepartsAtFacilityEvent event2 = new VehicleDepartsAtFacilityEvent(1.0, vehicleId1, facilityId1, 0.5);
	    
	    TransitDriverStartsEvent event3 = new TransitDriverStartsEvent(1.1, driverId2, vehicleId2, transitLineId1, transitRouteId1, departureId2);
	    VehicleDepartsAtFacilityEvent event4 = new VehicleDepartsAtFacilityEvent(1.1, vehicleId2, facilityId1, 0.5);
	    
	    StopId2LineId2Pulk test = new StopId2LineId2Pulk();
	    
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	    test.handleEvent(event3);
	    test.handleEvent(event4);
	    
//	    gibt nichts zurück
	    
	    System.out.println(test.getStopId2LineId2PulkDataList().toString());
	    
//	    to be implemented
	    
	    /** @TODO complete tests, the current version does not work
	     *  
	     */

	    Assert.assertEquals(event2, test.getStopId2LineId2PulkDataList().get(event2.getFacilityId()).get(event2.getVehicleId()));
	    
	    System.out.println(test.getStopId2LineId2PulkDataList().get(event2.getFacilityId()).get(event2.getVehicleId()));
	    System.out.println(event2.getVehicleId());
	    
	    
	    
		
	}

}

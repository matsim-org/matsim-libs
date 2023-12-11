package playground.vsp.andreas.bvgAna.level1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playground.vsp.andreas.bvgAna.level1.VehId2DelayAtStopMapData;

public class VehId2DelayAtStopMapDataTest {

	@Test
	void testVehId2DelayAtStopMapData() {
		
        
//        assign Ids to routes, vehicles and agents to be used in Test
        

        Id<Vehicle> vehId1 = Id.create(4, Vehicle.class);
        Id<Person> driverId1 = Id.create(0, Person.class);
        Id<TransitStopFacility> facilId1 = Id.create(9, TransitStopFacility.class);
        Id<TransitLine> transitLineId1 = Id.create(14, TransitLine.class);
        Id<TransitRoute> transitRouteId1 = Id.create(12, TransitRoute.class);
        Id<Departure> departureId1 = Id.create(13, Departure.class);
		
        TransitDriverStartsEvent event1 = new TransitDriverStartsEvent(2.4*3600, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
        VehicleDepartsAtFacilityEvent event2 = new VehicleDepartsAtFacilityEvent(2.3*3600, vehId1, facilId1, 1.);
        
        VehId2DelayAtStopMapData mapData = new VehId2DelayAtStopMapData(event1);
        
        mapData.addVehicleDepartsAtFacilityEvent(event2);
        
//        testing
        
        Assertions.assertNotNull(mapData);
        
        
        
		
	}


}

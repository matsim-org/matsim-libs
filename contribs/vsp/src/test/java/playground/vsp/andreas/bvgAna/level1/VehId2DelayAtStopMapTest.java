package playground.vsp.andreas.bvgAna.level1;

import java.util.LinkedList;
import java.util.TreeMap;

import junit.framework.Assert;
import playground.vsp.andreas.bvgAna.level1.VehId2DelayAtStopMap;
import playground.vsp.andreas.bvgAna.level1.VehId2DelayAtStopMapData;

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

public class VehId2DelayAtStopMapTest {

	@Test
	public void testVehId2DelayAtStopMap() {
		
		TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> data = new TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>>();

		//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
        Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
        Id<Person> driverId1 = Id.create(4, Person.class);
        Id<Person> driverId2 = Id.create(5, Person.class);
        Id<Person> driverId3 = Id.create(6, Person.class);
        Id<Person> driverId4 = Id.create(7, Person.class);
        Id<TransitStopFacility> facilId1 = Id.create(8, TransitStopFacility.class);
        Id<TransitStopFacility> facilId2 = Id.create(9, TransitStopFacility.class);        
        Id<TransitStopFacility> facilId3 = Id.create(10, TransitStopFacility.class);
        Id<TransitStopFacility> facilId4 = Id.create(11, TransitStopFacility.class);
        Id<TransitLine> transitLineId1 = Id.create(13, TransitLine.class);
        Id<TransitLine> transitLineId2 = Id.create(1, TransitLine.class);
        Id<TransitLine> transitLineId3 = Id.create(3, TransitLine.class);
        Id<TransitLine> transitLineId4 = Id.create(5, TransitLine.class);
        Id<TransitRoute> transitRouteId1 = Id.create(14, TransitRoute.class);
        Id<TransitRoute> transitRouteId2 = Id.create(2, TransitRoute.class);
        Id<TransitRoute> transitRouteId3 = Id.create(4, TransitRoute.class);
        Id<TransitRoute> transitRouteId4 = Id.create(6, TransitRoute.class);
        Id<Departure> departureId1 = Id.create(1, Departure.class);
        Id<Departure> departureId2 = Id.create(2, Departure.class);
        Id<Departure> departureId3 = Id.create(3, Departure.class);
        Id<Departure> departureId4 = Id.create(0, Departure.class);

//        create events
        
        VehicleDepartsAtFacilityEvent event1 = new VehicleDepartsAtFacilityEvent(2.3*3600, vehId1, facilId1, 2.3);
        TransitDriverStartsEvent event2 = new TransitDriverStartsEvent(2.6*3600, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
        
        VehicleDepartsAtFacilityEvent event3 = new VehicleDepartsAtFacilityEvent(2.8*3600, vehId2, facilId2, 2.5);
        TransitDriverStartsEvent event4 = new TransitDriverStartsEvent(2.3*3600, driverId2, vehId2, transitLineId2, transitRouteId2, departureId2);
        
        VehicleDepartsAtFacilityEvent event5 = new VehicleDepartsAtFacilityEvent(2.1*3600, vehId1, facilId3, 2.4);
        TransitDriverStartsEvent event6 = new TransitDriverStartsEvent(2.2*3600, driverId3, vehId1, transitLineId3, transitRouteId3, departureId3);
        
        VehicleDepartsAtFacilityEvent event7 = new VehicleDepartsAtFacilityEvent(2.6*3600, vehId2, facilId4, 2.1);
        TransitDriverStartsEvent event8 = new TransitDriverStartsEvent(2.1*3600, driverId4, vehId2, transitLineId4, transitRouteId4, departureId4);
       
//		create instance of class to be tested
        
		VehId2DelayAtStopMap testMap = new VehId2DelayAtStopMap();
		
//		handle events
		
		testMap.handleEvent(event2);
		testMap.handleEvent(event1);
		testMap.handleEvent(event4);
		testMap.handleEvent(event3);
		testMap.handleEvent(event6);
		testMap.handleEvent(event5);
		testMap.handleEvent(event8);
		testMap.handleEvent(event7);
		
//        add events to local TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> for comparison
		
		data.put(event2.getVehicleId(), new LinkedList<VehId2DelayAtStopMapData>());
        data.get(event2.getVehicleId()).add(new VehId2DelayAtStopMapData(event2));
        data.get(event2.getVehicleId()).getLast().addVehicleDepartsAtFacilityEvent(event1);
        data.get(event6.getVehicleId()).add(new VehId2DelayAtStopMapData(event6));
        data.get(event6.getVehicleId()).getLast().addVehicleDepartsAtFacilityEvent(event5);
        
        data.put(event4.getVehicleId(), new LinkedList<VehId2DelayAtStopMapData>());
        data.get(event4.getVehicleId()).add(new VehId2DelayAtStopMapData(event4));
        data.get(event4.getVehicleId()).getLast().addVehicleDepartsAtFacilityEvent(event3);
        data.get(event8.getVehicleId()).add(new VehId2DelayAtStopMapData(event8));
        data.get(event8.getVehicleId()).getLast().addVehicleDepartsAtFacilityEvent(event7);  
		
//        testing if first and last entries match
        	
		Assert.assertEquals(data.get(vehId1).getFirst().toString(), testMap.getVehId2DelayAtStopMap().get(vehId1).getFirst().toString());
		Assert.assertEquals(data.get(vehId2).getFirst().toString(), testMap.getVehId2DelayAtStopMap().get(vehId2).getFirst().toString());
		
		Assert.assertEquals(data.get(vehId1).getLast().toString(), testMap.getVehId2DelayAtStopMap().get(vehId1).getLast().toString());
		Assert.assertEquals(data.get(vehId2).getLast().toString(), testMap.getVehId2DelayAtStopMap().get(vehId2).getLast().toString());
		
		
	}



}

package playground.andreas.bvgAna.level1;

import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;

public class VehId2DelayAtStopMapTest {

	@Test
	public void testVehId2DelayAtStopMap() {
		
		TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> data = new TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>>();
		
        Id[] ida= new Id[15];
    	Set<Id> idSet = new TreeSet<Id>();
        for (int ii=0; ii<15; ii++){
        	ida[ii] = new IdImpl(ii); 
            idSet.add(ida[ii]);
        }
        
//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id vehId1 = ida[1];
        Id vehId2 = ida[2];
        Id driverId1 = ida[4];
        Id driverId2 = ida[5];
        Id driverId3 = ida[6];
        Id driverId4 = ida[7];
        Id facilId1 = ida[8];
        Id facilId2 = ida[9];        
        Id facilId3 = ida[10];
        Id facilId4 = ida[11];
        Id transitLineId1 = ida[13];
        Id transitRouteId1 = ida[14];
        Id transitLineId2 = ida[1];
        Id transitRouteId2 = ida[2];
        Id transitLineId3 = ida[3];
        Id transitRouteId3 = ida[4];
        Id transitLineId4 = ida[5];
        Id transitRouteId4 = ida[6];
        Id departureId1 = ida[1];
        Id departureId2 = ida[2];
        Id departureId3 = ida[3];
        Id departureId4 = ida[0];

//        create events
        
        EventsFactoryImpl ef = new EventsFactoryImpl();
        
        VehicleDepartsAtFacilityEvent event1 = ef.createVehicleDepartsAtFacilityEvent(2.3*3600, vehId1, facilId1, 2.3);
        TransitDriverStartsEvent event2 = ef.createTransitDriverStartsEvent(2.6*3600, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
        
        VehicleDepartsAtFacilityEvent event3 = ef.createVehicleDepartsAtFacilityEvent(2.8*3600, vehId2, facilId2, 2.5);
        TransitDriverStartsEvent event4 = ef.createTransitDriverStartsEvent(2.3*3600, driverId2, vehId2, transitLineId2, transitRouteId2, departureId2);
        
        VehicleDepartsAtFacilityEvent event5 = ef.createVehicleDepartsAtFacilityEvent(2.1*3600, vehId1, facilId3, 2.4);
        TransitDriverStartsEvent event6 = ef.createTransitDriverStartsEvent(2.2*3600, driverId3, vehId1, transitLineId3, transitRouteId3, departureId3);
        
        VehicleDepartsAtFacilityEvent event7 = ef.createVehicleDepartsAtFacilityEvent(2.6*3600, vehId2, facilId4, 2.1);
        TransitDriverStartsEvent event8 = ef.createTransitDriverStartsEvent(2.1*3600, driverId4, vehId2, transitLineId4, transitRouteId4, departureId4);
       
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

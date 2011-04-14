package playground.andreas.bvgAna.level1;

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

public class StopId2RouteId2DelayAtStopMapTest {
	
	private TreeMap<Id, TransitDriverStartsEvent> vehTestMap = new TreeMap<Id, TransitDriverStartsEvent>();
	private TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>> stopDataMapTest = new TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>>();

	@Test
	public void testStopId2RouteId2DelayAtStopMap() {
	    
		Id[] ida= new Id[15];
		Set<Id> idSet = new TreeSet<Id>();
	    for (int ii=0; ii<15; ii++){
	    	ida[ii] = new IdImpl(ii); 
	        idSet.add(ida[ii]);
	    }
	    
//	    assign Ids to routes, vehicles and agents to be used in Test
		
		Id vehId1 = ida[1];
	    Id vehId2 = ida[2];
	    Id driverId1 = ida[4];
	    Id driverId2 = ida[5];
	    Id departureId1 = ida[6];
	    Id departureId2 = ida[7];
	    Id transitRouteId1 = ida[14];
	    Id transitRouteId2 = ida[2];
	    Id transitLineId1 = ida[4];
	    Id transitLineId2 = ida[6];
	    Id facilityId1 = ida[8];
	    Id facilityId2 = ida[9];
	    
//	    create events
	    
	    EventsFactoryImpl ef = new EventsFactoryImpl();
	    
	    VehicleDepartsAtFacilityEvent event1 = ef.createVehicleDepartsAtFacilityEvent(2., vehId1, facilityId1, 0.5);
	    VehicleDepartsAtFacilityEvent event2 = ef.createVehicleDepartsAtFacilityEvent(2.7, vehId2, facilityId2, 0.7);
	    TransitDriverStartsEvent event3 = ef.createTransitDriverStartsEvent(2.8, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
	    TransitDriverStartsEvent event4 = ef.createTransitDriverStartsEvent(2.3, driverId2, vehId2, transitLineId2, transitRouteId2, departureId2);
	    
	    StopId2RouteId2DelayAtStopMap test = new StopId2RouteId2DelayAtStopMap();
	    
	    test.handleEvent(event3);
//	    test.handleEvent(event4);
	    test.handleEvent(event1);
//	    test.handleEvent(event2);
	   	   
	    
	    vehTestMap.put(event3.getVehicleId(), event3);
	    vehTestMap.put(event4.getVehicleId(), event4);
	    
//	    System.out.println("EINS: "+test.getStopId2RouteId2DelayAtStopMap().toString());
//	    System.out.println("ZWEI: "+test.getStopId2RouteId2DelayAtStopMap().get(event1.getFacilityId()).firstEntry().toString());
	    
	    // to be completed
	        
	    System.out.println(vehTestMap.toString());
	    
	    
	    
	    
	    
	}



}

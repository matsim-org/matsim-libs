package playground.andreas.bvgAna.level1;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;

public class VehId2DelayAtStopMapTest {

	@Test
	public void testVehId2DelayAtStopMap() {
		
        Id[] ida= new Id[15];
    	Set<Id> idSet = new TreeSet<Id>();
        for (int ii=0; ii<15; ii++){
        	ida[ii] = new IdImpl(ii); 
            idSet.add(ida[ii]);
        }
        
//        assign Ids to routes, vehicles and agents to be used in Test
        

        Id vehId1 = ida[4];
        Id driverId1 = ida[0];
        Id facilId1 = ida[9];
        Id transitLineId1 = ida[14];
        Id transitRouteId1 = ida[12];
        Id departureId1 = ida[13];

        
        EventsFactoryImpl ef = new EventsFactoryImpl();
        
        VehicleDepartsAtFacilityEvent event1 = ef.createVehicleDepartsAtFacilityEvent(2.5*3600, vehId1, facilId1, 2.);
        TransitDriverStartsEvent event2 = ef.createTransitDriverStartsEvent(2.6*3600, driverId1, vehId1, transitLineId1, transitRouteId1, departureId1);
		
		VehId2DelayAtStopMap testMap = new VehId2DelayAtStopMap();
		
		testMap.handleEvent(event2);
		testMap.handleEvent(event1);
		
//		test setup completed, work in progress
		
		
		
		
	}



}

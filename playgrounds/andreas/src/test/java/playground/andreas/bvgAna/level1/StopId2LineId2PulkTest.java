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

public class StopId2LineId2PulkTest {

	@Test
	public void testStopId2LineId2Pulk() {


		Id[] ida= new Id[15];
		Set<Id> idSet = new TreeSet<Id>();
	    for (int ii=0; ii<15; ii++){
	    	ida[ii] = new IdImpl(ii); 
	        idSet.add(ida[ii]);
	    }
	    
//	    assign Ids to routes, vehicles and agents to be used in Test
	    
	    Id driverId1 = ida[1];
	    Id vehicleId1 = ida[2];
	    Id transitLineId1 = ida[4];
	    Id transitRouteId1 = ida[5];
	    Id departureId1 = ida[6];
	    Id facilityId1 = ida[7];


//	    create events
	    
	    EventsFactoryImpl ef = new EventsFactoryImpl();
	    
	    TransitDriverStartsEvent event1 = ef.createTransitDriverStartsEvent(1.0, driverId1, vehicleId1, transitLineId1, transitRouteId1, departureId1);
	    VehicleDepartsAtFacilityEvent event2 = ef.createVehicleDepartsAtFacilityEvent(1.0, vehicleId1, facilityId1, 0.5);
	    
	    StopId2LineId2Pulk test = new StopId2LineId2Pulk();
	    
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	    
	    System.out.println(test.getStopId2LineId2PulkDataList().toString());
		
	}

}

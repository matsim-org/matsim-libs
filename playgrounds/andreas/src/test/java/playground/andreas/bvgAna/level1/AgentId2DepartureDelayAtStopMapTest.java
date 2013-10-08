package playground.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.core.basic.v01.IdImpl;

public class AgentId2DepartureDelayAtStopMapTest {

	@Test
	public void testAgentId2DepartureDelayAtStopMap() {
		
        Id[] ida= new Id[15];
    	Set<Id> idSet = new TreeSet<Id>();
        for (int ii=0; ii<15; ii++){
        	ida[ii] = new IdImpl(ii); 
            idSet.add(ida[ii]);
        }
        
//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id vehId1 = ida[4];
        Id vehId2 = ida[1];
        Id persId1 = ida[0];
        Id persId2 = ida[5];
        Id linkId1 = ida[14];
        Id linkId3 = ida[13];

        
        AgentId2DepartureDelayAtStopMap handler = new AgentId2DepartureDelayAtStopMap(idSet);
        
//        create Events
        
        PersonDepartureEvent event3 = new PersonDepartureEvent(2.9*3600, persId1, linkId3, TransportMode.pt);
		handler.handleEvent(event3);
		PersonDepartureEvent event4 = new PersonDepartureEvent(2.1*3600, persId2, linkId1, TransportMode.pt);
		handler.handleEvent(event4);
        
        PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(2.9*3600, persId1, vehId1);
        handler.handleEvent(event1);
        PersonEntersVehicleEvent event2 = new PersonEntersVehicleEvent(2.1*3600, persId2, vehId2);
        handler.handleEvent(event2);
        
//        run tests
		
        Assert.assertTrue(handler.getStopId2DelayAtStopMap().containsKey(persId1));
		Assert.assertEquals(event3.getTime(), handler.getStopId2DelayAtStopMap().get(persId1).getAgentEntersVehicle().get(0));
		
		Assert.assertTrue(handler.getStopId2DelayAtStopMap().containsKey(persId2));
		Assert.assertEquals(event4.getTime(), handler.getStopId2DelayAtStopMap().get(persId2).getAgentEntersVehicle().get(0));
		
		
	}

}

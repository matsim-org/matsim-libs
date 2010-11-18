package playground.andreas.bvgAna.level1;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;

public class AgentId2EnterLeaveVehicleEventHandlerTest {

    /**
     * Test method for {@link playground.andreas.bvgAna.level1.AgentId2EnterLeaveVehicleEventHandler#AgentId2EnterLeaveVehicleEventHandler(java.util.Set)}.
     */
    @Test
    
    public void testAgentId2EnterLeaveVehicleEventHandler() {
    	       
        Id[] ida= new Id[10];
    	Set<Id> id = new TreeSet<Id>();
        for (int ii=0; ii<10; ii++){
        	ida[ii] = new IdImpl(ii); 
            id.add(ida[ii]);
        }
        
        Id routeId1 = ida[2];
        Id routeId2 = ida[3];
        Id routeId3 = ida[7]; 
        Id vehId1 = ida[4];
        Id vehId2 = ida[1];
        Id vehId3 = ida[6];
        Id persId1 = ida[0];
        Id persId2 =ida[5];
        
        EventsFactoryImpl ef = new EventsFactoryImpl();
        AgentId2EnterLeaveVehicleEventHandler handler = new AgentId2EnterLeaveVehicleEventHandler(id);
        
        
        PersonEntersVehicleEvent event1 = ef.createPersonEntersVehicleEvent(3.0*3600, persId1, vehId1, routeId1);
        handler.handleEvent(event1);
        Assert.assertNotNull(handler.getAgentId2EnterEventMap().get(persId1));
//        Assert.assertEquals(event1, handler.getAgentId2EnterEventMap().get(persId1));
        Assert.assertSame(event1, handler.getAgentId2EnterEventMap().get(persId1).get(0));

        
        
    }

}

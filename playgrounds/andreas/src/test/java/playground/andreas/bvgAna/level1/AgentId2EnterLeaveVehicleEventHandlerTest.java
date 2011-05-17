package playground.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
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
    	       
        Id[] ida= new Id[9];
    	Set<Id> idSet = new TreeSet<Id>();
        for (int ii=0; ii<9; ii++){
        	ida[ii] = new IdImpl(ii); 
            idSet.add(ida[ii]);
        }
        
//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id routeId1 = ida[2];
        Id routeId2 = ida[3];
        Id routeId3 = ida[7]; 
        Id vehId1 = ida[4];
        Id vehId2 = ida[1];
        Id vehId3 = ida[6];
        Id persId1 = ida[0];
        Id persId2 = ida[5];
        Id persId3 = ida[8];
        
        EventsFactoryImpl ef = new EventsFactoryImpl();
        AgentId2EnterLeaveVehicleEventHandler handler = new AgentId2EnterLeaveVehicleEventHandler(idSet);
        
//        create Events
        
        PersonEntersVehicleEvent event1 = ef.createPersonEntersVehicleEvent(3.0*3600, persId1, vehId1, routeId2);
        handler.handleEvent(event1);
        PersonEntersVehicleEvent event2 = ef.createPersonEntersVehicleEvent(3.7*3600, persId2, vehId3, routeId1);
        handler.handleEvent(event2);
        PersonEntersVehicleEvent event3 = ef.createPersonEntersVehicleEvent(3.2*3600, persId3, vehId2, routeId3);
        handler.handleEvent(event3);
        
        PersonLeavesVehicleEvent event4 = ef.createPersonLeavesVehicleEvent(4.0*3600, persId1, vehId2, routeId3);
        handler.handleEvent(event4);
        PersonLeavesVehicleEvent event5 = ef.createPersonLeavesVehicleEvent(4.7*3600, persId2, vehId3, routeId1);
        handler.handleEvent(event5);
        PersonLeavesVehicleEvent event6 = ef.createPersonLeavesVehicleEvent(4.2*3600, persId3, vehId1, routeId2);
        handler.handleEvent(event6);
        
//        run tests
        
        Assert.assertSame(event1, handler.getAgentId2EnterEventMap().get(persId1).get(0));
        Assert.assertSame(event2, handler.getAgentId2EnterEventMap().get(persId2).get(0));
        Assert.assertSame(event3, handler.getAgentId2EnterEventMap().get(persId3).get(0));
        Assert.assertSame(event4, handler.getAgentId2LeaveEventMap().get(persId1).get(0));
        Assert.assertSame(event5, handler.getAgentId2LeaveEventMap().get(persId2).get(0));
        Assert.assertSame(event6, handler.getAgentId2LeaveEventMap().get(persId3).get(0));
        
        
    }

}

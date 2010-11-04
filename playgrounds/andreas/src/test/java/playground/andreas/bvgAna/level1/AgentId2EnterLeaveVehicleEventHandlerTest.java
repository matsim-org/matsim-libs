package playground.andreas.bvgAna.level1;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;

public class AgentId2EnterLeaveVehicleEventHandlerTest {

    /**
     * Test method for {@link playground.andreas.bvgAna.level1.AgentId2EnterLeaveVehicleEventHandler#AgentId2EnterLeaveVehicleEventHandler(java.util.Set)}.
     */
    @Test
    public void testAgentId2EnterLeaveVehicleEventHandler() {
        
        Set<Id> id = null;
        for (int ii=0; ii<5; ii++){
            id.add(new IdImpl(ii));
        }
        Id[] ida = (Id[]) id.toArray();
        Id routeId = ida[2];
        Id vehId1 = ida[4];
        Id vehId2 = ida[1];
        
        EventsFactoryImpl ef = new EventsFactoryImpl();
        AgentId2EnterLeaveVehicleEventHandler handler = new AgentId2EnterLeaveVehicleEventHandler(id);
        
        handler.handleEvent(ef.createPersonEntersVehicleEvent(3.0*3600-15, ida[0], vehId1, routeId));
        Assert.assertNotNull("Testblablabla", handler.getAgentId2EnterEventMap());
        
        
    }

}

package playground.andreas.bvgAna.level1;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;

public class AgentId2PtTripTravelTimeMapDataTest {

	@Test
	public void testAgentId2PtTripTravelTimeMapData() {
		
		
	       Id[] ida= new Id[15];
	    	Set<Id> idSet = new TreeSet<Id>();
	        for (int ii=0; ii<15; ii++){
	        	ida[ii] = new IdImpl(ii); 
	            idSet.add(ida[ii]);
	        }
	        
//	        assign Ids to routes, vehicles and agents to be used in Test
	        
	        Id linkId1 = ida[1];
	        Id linkId2 = ida[2];
	        Id linkId3 = ida[3];
	        Id agentId1 = ida[4];
	        Id facilId1 = ida[5];
	        Id facilId2 = ida[6];    

		EventsFactoryImpl ef = new EventsFactoryImpl();
        ActivityEndEvent event = ef.createActivityEndEvent(1.2, agentId1, linkId1, facilId1, "w");	
        AgentDepartureEvent event3 = ef.createAgentDepartureEvent(1.2, agentId1, linkId2, "pt");        
        AgentArrivalEvent event4 = ef.createAgentArrivalEvent(1.9, agentId1, linkId3, "pt");
		
		AgentId2PtTripTravelTimeMapData test = new AgentId2PtTripTravelTimeMapData(event);
		
		test.handle(event3);
		test.handle(event4);
		
		System.out.println(test.getTotalTripTravelTime());
		
//		test to be implemented
		
		
	}

}

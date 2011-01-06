package playground.andreas.bvgAna.level1;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;

public class AgentId2DepartureDelayAtStopMapDataTest {
	
	
	private ArrayList<Double> agentDepartsPTInteraction = new ArrayList<Double>();
	private ArrayList<Double> agentEntersVehicle = new ArrayList<Double>();
	
	

	@Test
	public void testAgentId2DepartureDelayAtStopMapData() {
		
        Id[] ida= new Id[15];
    	Set<Id> idSet = new TreeSet<Id>();
        for (int ii=0; ii<15; ii++){
        	ida[ii] = new IdImpl(ii); 
            idSet.add(ida[ii]);
        }
        
//        assign Ids to routes, vehicles and agents to be used in Test, same as in AgentIdDepartureDelayAtStopMapTest
        
        Id routeId1 = ida[2];
        Id routeId2 = ida[3];
        Id routeId3 = ida[7]; 
        Id vehId1 = ida[4];
        Id vehId2 = ida[1];
        Id vehId3 = ida[6];
        Id persId1 = ida[0];
        Id facilId1 = ida[9];
        Id facilId2 = ida[11];
        Id linkId1 = ida[14];
        Id linkId2 = ida[12];
        Id linkId3 = ida[13];
        Id linkId4 = ida[10];
        
        EventsFactoryImpl ef = new EventsFactoryImpl();
        AgentId2DepartureDelayAtStopMapData data = new AgentId2DepartureDelayAtStopMapData(persId1);
        
//        create Events
        
        AgentDepartureEvent event3 = ef.createAgentDepartureEvent(2.9*3600, persId1, linkId3, TransportMode.pt);
//		AgentDepartureEvent event4 = ef.createAgentDepartureEvent(2.1*3600, persId1, linkId1, TransportMode.pt);     
        PersonEntersVehicleEvent event1 = ef.createPersonEntersVehicleEvent(2.9*3600, persId1, vehId1, routeId1);
//        PersonEntersVehicleEvent event2 = ef.createPersonEntersVehicleEvent(2.1*3600, persId1, vehId2, routeId2);
        
        data.addAgentDepartureEvent(event3);
//        data.addAgentDepartureEvent(event4);
        data.addPersonEntersVehicleEvent(event1);
//        data.addPersonEntersVehicleEvent(event2);
        
//        run tests for 1 event each, can be expanded later if needed
        
        Assert.assertEquals((Double)event3.getTime(), data.getAgentDepartsPTInteraction().get(0));
		Assert.assertEquals((Double)event1.getTime(), data.getAgentEntersVehicle().get(0));
		
		
	}



}

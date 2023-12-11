package playground.vsp.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.vsp.andreas.bvgAna.level1.AgentId2EnterLeaveVehicleEventHandler;

public class AgentId2EnterLeaveVehicleEventHandlerTest {

	/**
	* Test method for {@link playground.vsp.andreas.bvgAna.level1.AgentId2EnterLeaveVehicleEventHandler#AgentId2EnterLeaveVehicleEventHandler(java.util.Set)}.
	*/
	@Test
	void testAgentId2EnterLeaveVehicleEventHandler() {
    	       
    	Set<Id<Person>> idSet = new TreeSet<>();
    	for (int ii=0; ii<9; ii++){
    		idSet.add(Id.create(ii, Person.class));
    	}
        
//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id<Vehicle> vehId1 = Id.create(4, Vehicle.class);
        Id<Vehicle> vehId2 = Id.create(1, Vehicle.class);
        Id<Vehicle> vehId3 = Id.create(6, Vehicle.class);
        Id<Person> persId1 = Id.create(0, Person.class);
        Id<Person> persId2 = Id.create(5, Person.class);
        Id<Person> persId3 = Id.create(8, Person.class);
        
        AgentId2EnterLeaveVehicleEventHandler handler = new AgentId2EnterLeaveVehicleEventHandler(idSet);
        
//        create Events
        
        PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(3.0*3600, persId1, vehId1);
        handler.handleEvent(event1);
        PersonEntersVehicleEvent event2 = new PersonEntersVehicleEvent(3.7*3600, persId2, vehId3);
        handler.handleEvent(event2);
        PersonEntersVehicleEvent event3 = new PersonEntersVehicleEvent(3.2*3600, persId3, vehId2);
        handler.handleEvent(event3);
        
        PersonLeavesVehicleEvent event4 = new PersonLeavesVehicleEvent(4.0*3600, persId1, vehId2);
        handler.handleEvent(event4);
        PersonLeavesVehicleEvent event5 = new PersonLeavesVehicleEvent(4.7*3600, persId2, vehId3);
        handler.handleEvent(event5);
        PersonLeavesVehicleEvent event6 = new PersonLeavesVehicleEvent(4.2*3600, persId3, vehId1);
        handler.handleEvent(event6);
        
//        run tests
        
        Assertions.assertSame(event1, handler.getAgentId2EnterEventMap().get(persId1).get(0));
        Assertions.assertSame(event2, handler.getAgentId2EnterEventMap().get(persId2).get(0));
        Assertions.assertSame(event3, handler.getAgentId2EnterEventMap().get(persId3).get(0));
        Assertions.assertSame(event4, handler.getAgentId2LeaveEventMap().get(persId1).get(0));
        Assertions.assertSame(event5, handler.getAgentId2LeaveEventMap().get(persId2).get(0));
        Assertions.assertSame(event6, handler.getAgentId2LeaveEventMap().get(persId3).get(0));
        
        
    }

}

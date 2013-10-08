package playground.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.basic.v01.IdImpl;


public class PersonEnterLeaveVehicle2ActivityHandlerTest {
	
	public void testPersonEnterLeaveVehicle2ActivityHandler() {
		
	    Id[] ida= new Id[15];
		Set<Id> idSet = new TreeSet<Id>();
	    for (int ii=0; ii<15; ii++){
	    	ida[ii] = new IdImpl(ii); 
	        idSet.add(ida[ii]);
	    }
	    
//	    assign Ids to routes, vehicles and agents to be used in Test
		
	    Id linkId1 = ida[1];
	    Id personId1 = ida[2];
	    Id personId2 = ida[7];
	    Id agentId2 = ida[8];
	    Id vehicleId1 = ida[3];
	    Id agentId1 = ida[4];
	    Id facilityId1 = ida[6];   

		
//		steht aus
	    
	    EventsFactory ef = new EventsFactory(); 
	    
	    ActivityEndEvent event0 = ef.createActivityEndEvent(1.0, agentId1, linkId1, facilityId1, "w");
	    PersonEntersVehicleEvent event1 = ef.createPersonEntersVehicleEvent(1.0, personId1, vehicleId1);
	    PersonLeavesVehicleEvent event2 = ef.createPersonLeavesVehicleEvent(1.5, personId1, vehicleId1);
	    ActivityStartEvent event3 = ef.createActivityStartEvent(1.5, agentId1, linkId1, facilityId1, "h");
	    ActivityEndEvent event4 = ef.createActivityEndEvent(2.5, agentId2, linkId1, facilityId1, "h");
	    PersonEntersVehicleEvent event5 = ef.createPersonEntersVehicleEvent(2.5, personId2, vehicleId1);
	    PersonLeavesVehicleEvent event6 = ef.createPersonLeavesVehicleEvent(2.9, personId2, vehicleId1);
	    ActivityStartEvent event7 = ef.createActivityStartEvent(2.9, agentId2, linkId1, facilityId1, "w");

	    
	    PersonEnterLeaveVehicle2ActivityHandler test = new PersonEnterLeaveVehicle2ActivityHandler(idSet);
	    
	    test.handleEvent(event0);
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	    test.handleEvent(event3);
	    test.handleEvent(event4);
	    test.handleEvent(event5);
	    test.handleEvent(event6);
	    test.handleEvent(event7);
	    
	    Assert.assertEquals(event0, test.getPersonEntersVehicleEvent2ActivityEndEvent().get(event1));
	    
	    System.out.println(test.getPersonEntersVehicleEvent2ActivityEndEvent().toString());
	    
	    System.out.println(test.getPersonLeavesVehicleEvent2ActivityStartEvent().toString());
	    
	    
		
	}
	

	
	
}

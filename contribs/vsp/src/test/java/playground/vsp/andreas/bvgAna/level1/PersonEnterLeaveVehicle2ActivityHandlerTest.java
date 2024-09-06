package playground.vsp.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;


public class PersonEnterLeaveVehicle2ActivityHandlerTest {

	public void testPersonEnterLeaveVehicle2ActivityHandler() {
		Set<Id<Person>> idSet = new TreeSet<>();
  	for (int ii=0; ii<15; ii++){
  		idSet.add(Id.create(ii, Person.class));
  	}
//	    assign Ids to routes, vehicles and agents to be used in Test

	    Id<Link> linkId1 = Id.create(1, Link.class);
	    Id<Person> personId1 = Id.create(2, Person.class);
	    Id<Person> personId2 = Id.create(7, Person.class);
	    Id<Person> agentId1 = Id.create(4, Person.class);
	    Id<Person> agentId2 = Id.create(8, Person.class);
	    Id<Vehicle> vehicleId1 = Id.create(3, Vehicle.class);
	    Id<ActivityFacility> facilityId1 = Id.create(6, ActivityFacility.class);


//		steht aus

	    ActivityEndEvent event0 = new ActivityEndEvent(1.0, agentId1, linkId1, facilityId1, "w");
	    PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(1.0, personId1, vehicleId1);
	    PersonLeavesVehicleEvent event2 = new PersonLeavesVehicleEvent(1.5, personId1, vehicleId1);
	    ActivityStartEvent event3 = new ActivityStartEvent(1.5, agentId1, linkId1, facilityId1, "h");
	    ActivityEndEvent event4 = new ActivityEndEvent(2.5, agentId2, linkId1, facilityId1, "h");
	    PersonEntersVehicleEvent event5 = new PersonEntersVehicleEvent(2.5, personId2, vehicleId1);
	    PersonLeavesVehicleEvent event6 = new PersonLeavesVehicleEvent(2.9, personId2, vehicleId1);
	    ActivityStartEvent event7 = new ActivityStartEvent(2.9, agentId2, linkId1, facilityId1, "w");


	    PersonEnterLeaveVehicle2ActivityHandler test = new PersonEnterLeaveVehicle2ActivityHandler(idSet);

	    test.handleEvent(event0);
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	    test.handleEvent(event3);
	    test.handleEvent(event4);
	    test.handleEvent(event5);
	    test.handleEvent(event6);
	    test.handleEvent(event7);

	    Assertions.assertEquals(event0, test.getPersonEntersVehicleEvent2ActivityEndEvent().get(event1));

	    System.out.println(test.getPersonEntersVehicleEvent2ActivityEndEvent().toString());

	    System.out.println(test.getPersonLeavesVehicleEvent2ActivityStartEvent().toString());



	}




}

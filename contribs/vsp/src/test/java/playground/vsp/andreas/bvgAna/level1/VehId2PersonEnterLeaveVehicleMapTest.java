package playground.vsp.andreas.bvgAna.level1;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class VehId2PersonEnterLeaveVehicleMapTest {

	TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> enter = new TreeMap<Id, ArrayList<PersonEntersVehicleEvent>>();
	TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> leave = new TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>>();

	@Test
	void testVehId2PersonEnterLeaveVehicleMap() {

//	    assign Ids to routes, vehicles and agents to be used in Test

	    Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
	    Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
	    Id<Person> persId1 = Id.create(4, Person.class);
	    Id<Person> persId2 = Id.create(5, Person.class);
	    Id<Person> persId3 = Id.create(6, Person.class);
	    Id<Person> persId4 = Id.create(7, Person.class);

//	    create events

	    PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(2., persId1, vehId1);
	    PersonEntersVehicleEvent event2 = new PersonEntersVehicleEvent(2.1, persId2, vehId1);
	    PersonLeavesVehicleEvent event3 = new PersonLeavesVehicleEvent(2.2, persId3, vehId2);
	    PersonLeavesVehicleEvent event4 = new PersonLeavesVehicleEvent(2.3, persId4, vehId2);

//	    create instance of class to be tested

	    VehId2PersonEnterLeaveVehicleMap test = new VehId2PersonEnterLeaveVehicleMap();

//	    handle events

	    test.handleEvent(event1);
	    test.handleEvent(event2);
	    test.handleEvent(event3);
	    test.handleEvent(event4);

//	    add events to local TreeMaps for comparison

	    enter.put(event1.getVehicleId(), new ArrayList<PersonEntersVehicleEvent>());
	    enter.get(event1.getVehicleId()).add(event1);
	    enter.get(event2.getVehicleId()).add(event2);

	    leave.put(event3.getVehicleId(), new ArrayList<PersonLeavesVehicleEvent>());
	    leave.get(event3.getVehicleId()).add(event3);
	    leave.get(event4.getVehicleId()).add(event4);

//	    test

//	    Assert.assertEquals(enter.get(vehId1).get(0), test.getVehId2PersonEnterEventMap().get(vehId1).get(0));
	    Assertions.assertEquals(event1.getTime(), test.getVehId2PersonEnterEventMap().get(vehId1).get(0).getTime(), 0.);

//	    Assert.assertEquals(enter.get(vehId1).get(1), test.getVehId2PersonEnterEventMap().get(vehId1).get(1));
	    Assertions.assertEquals(event2.getTime(), test.getVehId2PersonEnterEventMap().get(vehId1).get(1).getTime(), 0.);


//	    Assert.assertEquals(leave.get(vehId2).get(0), test.getVehId2PersonLeaveEventMap().get(vehId2).get(0));
	    Assertions.assertEquals(event3.getTime(), test.getVehId2PersonLeaveEventMap().get(vehId2).get(0).getTime(), 0.);

//	    Assert.assertEquals(leave.get(vehId2).get(1), test.getVehId2PersonLeaveEventMap().get(vehId2).get(1));
	    Assertions.assertEquals(event4.getTime(), test.getVehId2PersonLeaveEventMap().get(vehId2).get(1).getTime(), 0.);



	}

}

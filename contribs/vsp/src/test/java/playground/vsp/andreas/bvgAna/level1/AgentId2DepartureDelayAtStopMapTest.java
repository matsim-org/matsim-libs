package playground.vsp.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class AgentId2DepartureDelayAtStopMapTest {

	@Test
	void testAgentId2DepartureDelayAtStopMap() {

		Set<Id<Person>> idSet = new TreeSet<>();
		for (int ii=0; ii<15; ii++){
			idSet.add(Id.create(ii, Person.class));
		}

//        assign Ids to routes, vehicles and agents to be used in Test

        Id<Vehicle> vehId1 = Id.create(4, Vehicle.class);
        Id<Vehicle> vehId2 = Id.create(1, Vehicle.class);
        Id<Person> persId1 = Id.create(0, Person.class);
        Id<Person> persId2 = Id.create(5, Person.class);
        Id<Link> linkId1 = Id.create(14, Link.class);
        Id<Link> linkId3 = Id.create(13, Link.class);


        AgentId2DepartureDelayAtStopMap handler = new AgentId2DepartureDelayAtStopMap(idSet);

//        create Events

        PersonDepartureEvent event3 = new PersonDepartureEvent(2.9*3600, persId1, linkId3, TransportMode.pt, TransportMode.pt);
		handler.handleEvent(event3);
		PersonDepartureEvent event4 = new PersonDepartureEvent(2.1*3600, persId2, linkId1, TransportMode.pt, TransportMode.pt);
		handler.handleEvent(event4);

        PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(2.9*3600, persId1, vehId1);
        handler.handleEvent(event1);
        PersonEntersVehicleEvent event2 = new PersonEntersVehicleEvent(2.1*3600, persId2, vehId2);
        handler.handleEvent(event2);

//        run tests

        Assertions.assertTrue(handler.getStopId2DelayAtStopMap().containsKey(persId1));
		Assertions.assertEquals(event3.getTime(), handler.getStopId2DelayAtStopMap().get(persId1).getAgentEntersVehicle().get(0), 0);

		Assertions.assertTrue(handler.getStopId2DelayAtStopMap().containsKey(persId2));
		Assertions.assertEquals(event4.getTime(), handler.getStopId2DelayAtStopMap().get(persId2).getAgentEntersVehicle().get(0), 0);


	}

}

package playground.vsp.andreas.bvgAna.level1;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

/**
 * @author fuerbas
 *
 */
public class AgentId2PtTripTravelTimeMapTest {

	private TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>> agentId2PtTripTravelTimeMap = new TreeMap<Id, ArrayList<AgentId2PtTripTravelTimeMapData>>();
	private TreeMap<Id, AgentId2PtTripTravelTimeMapData> tempList = new TreeMap<Id, AgentId2PtTripTravelTimeMapData>();

	/**
	 * Test method for {@link playground.vsp.andreas.bvgAna.level1.AgentId2PtTripTravelTimeMap#AgentId2PtTripTravelTimeMap(java.util.Set)}.
	 */
	@Test
	void testAgentId2PtTripTravelTimeMap() {


		Set<Id<Person>> idSet = new TreeSet<>();
  	for (int ii=0; ii<15; ii++){
  		idSet.add(Id.create(ii, Person.class));
  	}

//	        assign Ids to routes, vehicles and agents to be used in Test

	        Id<Link> linkId1 = Id.create(1, Link.class);
	        Id<Link> linkId2 = Id.create(2, Link.class);
	        Id<Link> linkId3 = Id.create(3, Link.class);
	        Id<Person> agentId1 = Id.create(4, Person.class);
	        Id<ActivityFacility> facilId1 = Id.create(5, ActivityFacility.class);
	        Id<ActivityFacility> facilId2 = Id.create(6, ActivityFacility.class);


	        AgentId2PtTripTravelTimeMap test = new AgentId2PtTripTravelTimeMap(idSet);

	        ActivityStartEvent event1 = new ActivityStartEvent(1.0, agentId1, linkId1, facilId1, "w", new Coord( 234., 5.67 ));
	        ActivityEndEvent event2 = new ActivityEndEvent(1.2, agentId1, linkId1, facilId1, "w", new Coord( 234., 5.67 ));

	        PersonDepartureEvent event3 = new PersonDepartureEvent(1.2, agentId1, linkId2, "pt", "pt");
	        PersonArrivalEvent event4 = new PersonArrivalEvent(1.9, agentId1, linkId3, "pt");

	        ActivityStartEvent event5 = new ActivityStartEvent(1.9, agentId1, linkId3, facilId2, "h", new Coord( 123., 45.67 ));	//home mit anderen werten
	        ActivityEndEvent event6 = new ActivityEndEvent(2.7, agentId1, linkId3, facilId2, "h", new Coord( 123., 45.67 ));

	        test.handleEvent(event1);
	        test.handleEvent(event2);
	        test.handleEvent(event3);
	        test.handleEvent(event4);
	        test.handleEvent(event5);
	        test.handleEvent(event6);

//	        first tests, this works

	        Assertions.assertEquals(event4.getTime()-event3.getTime(), test.getAgentId2PtTripTravelTimeMap().get(agentId1).get(0).getTotalTripTravelTime(), 0.);


	}



}

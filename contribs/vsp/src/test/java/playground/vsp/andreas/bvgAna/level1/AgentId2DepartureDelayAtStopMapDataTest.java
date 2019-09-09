package playground.vsp.andreas.bvgAna.level1;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.vsp.andreas.bvgAna.level1.AgentId2DepartureDelayAtStopMapData;

public class AgentId2DepartureDelayAtStopMapDataTest {
	
		

	@Test
	public void testAgentId2DepartureDelayAtStopMapData() {
		
//        assign Ids to routes, vehicles and agents to be used in Test
        
        Id<Vehicle> vehId1 = Id.create(4, Vehicle.class);
        Id<Person> persId1 = Id.create(0, Person.class);
        Id<Link> linkId3 = Id.create(13, Link.class);

        AgentId2DepartureDelayAtStopMapData data = new AgentId2DepartureDelayAtStopMapData(persId1);
        
//        create Events
        
        PersonDepartureEvent event3 = new PersonDepartureEvent(2.9*3600, persId1, linkId3, TransportMode.pt);
        PersonEntersVehicleEvent event1 = new PersonEntersVehicleEvent(2.9*3600, persId1, vehId1);
        
        data.addAgentDepartureEvent(event3);
        data.addPersonEntersVehicleEvent(event1);
        
//        run tests for 1 event each, can be expanded later if needed
        
        Assert.assertEquals((Double)event3.getTime(), data.getAgentDepartsPTInteraction().get(0));
		Assert.assertEquals((Double)event1.getTime(), data.getAgentEntersVehicle().get(0));
		
		
		
	}



}

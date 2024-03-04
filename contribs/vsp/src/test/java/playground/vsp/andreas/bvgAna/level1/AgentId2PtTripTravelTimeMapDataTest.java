/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.andreas.bvgAna.level1;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

import playground.vsp.andreas.bvgAna.level1.AgentId2PtTripTravelTimeMapData;

public class AgentId2PtTripTravelTimeMapDataTest {

	@Test
	void testAgentId2PtTripTravelTimeMapData() {
		
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

        ActivityEndEvent event = new ActivityEndEvent(1.2*3600, agentId1, linkId1, facilId1, "w", new Coord( 234., 5.67 ));
        PersonDepartureEvent event3 = new PersonDepartureEvent(1.2*3600, agentId1, linkId2, "pt", "pt");        
        PersonArrivalEvent event4 = new PersonArrivalEvent(1.9*3600, agentId1, linkId3, "pt");
        PersonDepartureEvent event5 = new PersonDepartureEvent(2.1*3600, agentId1, linkId3, "pt", "pt");        
        PersonArrivalEvent event6 = new PersonArrivalEvent(2.5*3600, agentId1, linkId2, "pt");
		
		AgentId2PtTripTravelTimeMapData test = new AgentId2PtTripTravelTimeMapData(event);
		
		test.handle(event3);
		test.handle(event4);
		test.handle(event5);
		test.handle(event6);
				
//		test, this works
		
		System.out.println("Number of Transfers should be: 1 and are: "+test.getNumberOfTransfers());	
		System.out.println("Total travel time should be: "+(event6.getTime()-event5.getTime()+event4.getTime()-event3.getTime())+" and is: "+test.getTotalTripTravelTime()); 
				
		Assertions.assertEquals(event6.getTime()-event5.getTime()+event4.getTime()-event3.getTime(), test.getTotalTripTravelTime(), 0.);
		
		Assertions.assertEquals(1, test.getNumberOfTransfers());
		

		
	}

}

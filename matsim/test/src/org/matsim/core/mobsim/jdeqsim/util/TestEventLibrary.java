package org.matsim.core.mobsim.jdeqsim.util;

import java.util.LinkedList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.testcases.MatsimTestCase;

public class TestEventLibrary extends MatsimTestCase {
	
	public void testGetTravelTime(){
		LinkedList<PersonEvent> events=new LinkedList<PersonEvent>();
		events.add(new AgentDepartureEventImpl(20, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new AgentArrivalEventImpl(30, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new AgentDepartureEventImpl(90, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		events.add(new AgentArrivalEventImpl(110, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		
		assertEquals(20.0, EventLibrary.getTravelTime(events,1), EPSILON);
	}
	
	public void testGetAverageTravelTime(){
		LinkedList<PersonEvent> events=new LinkedList<PersonEvent>();
		events.add(new AgentDepartureEventImpl(20, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new AgentArrivalEventImpl(30, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new AgentDepartureEventImpl(90, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		events.add(new AgentArrivalEventImpl(110, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		
		assertEquals(30.0, EventLibrary.getSumTravelTime(events), EPSILON);
	}
	
}

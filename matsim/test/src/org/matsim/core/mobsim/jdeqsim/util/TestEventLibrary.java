package org.matsim.core.mobsim.jdeqsim.util;

import java.util.LinkedList;

import org.matsim.api.basic.v01.events.BasicPersonEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.testcases.MatsimTestCase;

public class TestEventLibrary extends MatsimTestCase {
	
	public void testGetTravelTime(){
		LinkedList<BasicPersonEvent> events=new LinkedList<BasicPersonEvent>();
		events.add(new AgentDepartureEvent(20, new IdImpl("2"), new IdImpl("0")));
		events.add(new AgentArrivalEvent(30, new IdImpl("2"), new IdImpl("0")));
		events.add(new AgentDepartureEvent(90, new IdImpl("1"), new IdImpl("0")));
		events.add(new AgentArrivalEvent(110, new IdImpl("1"), new IdImpl("0")));
		
		assertEquals(20.0, EventLibrary.getTravelTime(events,1), EPSILON);
	}
	
	public void testGetAverageTravelTime(){
		LinkedList<BasicPersonEvent> events=new LinkedList<BasicPersonEvent>();
		events.add(new AgentDepartureEvent(20, new IdImpl("2"), new IdImpl("0")));
		events.add(new AgentArrivalEvent(30, new IdImpl("2"), new IdImpl("0")));
		events.add(new AgentDepartureEvent(90, new IdImpl("1"), new IdImpl("0")));
		events.add(new AgentArrivalEvent(110, new IdImpl("1"), new IdImpl("0")));
		
		assertEquals(30.0, EventLibrary.getSumTravelTime(events), EPSILON);
	}
	
}

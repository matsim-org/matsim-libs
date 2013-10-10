package org.matsim.core.mobsim.jdeqsim.util;

import java.util.LinkedList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class TestEventLibrary extends MatsimTestCase {
	
	public void testGetTravelTime(){
		LinkedList<Event> events=new LinkedList<Event>();
		events.add(new PersonDepartureEvent(20, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new PersonArrivalEvent(30, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new PersonDepartureEvent(90, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		events.add(new PersonArrivalEvent(110, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		
		assertEquals(20.0, EventLibrary.getTravelTime(events,1), EPSILON);
	}
	
	public void testGetAverageTravelTime(){
		LinkedList<Event> events=new LinkedList<Event>();
		events.add(new PersonDepartureEvent(20, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new PersonArrivalEvent(30, new IdImpl("2"), new IdImpl("0"), TransportMode.car));
		events.add(new PersonDepartureEvent(90, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		events.add(new PersonArrivalEvent(110, new IdImpl("1"), new IdImpl("0"), TransportMode.car));
		
		assertEquals(30.0, EventLibrary.getSumTravelTime(events), EPSILON);
	}
	
}

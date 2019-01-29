package org.matsim.core.mobsim.jdeqsim.util;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestCase;

public class TestEventLibrary extends MatsimTestCase {
	
	public void testGetTravelTime(){
		LinkedList<Event> events=new LinkedList<Event>();
		events.add(new PersonDepartureEvent(20, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonArrivalEvent(30, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonDepartureEvent(90, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonArrivalEvent(110, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car));
		
		assertEquals(20.0, EventLibrary.getTravelTime(events,1), EPSILON);
	}
	
	public void testGetAverageTravelTime(){
		LinkedList<Event> events=new LinkedList<Event>();
		events.add(new PersonDepartureEvent(20, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonArrivalEvent(30, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonDepartureEvent(90, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonArrivalEvent(110, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car));
		
		assertEquals(30.0, EventLibrary.getSumTravelTime(events), EPSILON);
	}
	
}

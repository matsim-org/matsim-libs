
/* *********************************************************************** *
 * project: org.matsim.*
 * TestEventLibrary.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.jdeqsim.util;

import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestUtils;

	public class TestEventLibrary {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	 @Test
	 void testGetTravelTime(){
		LinkedList<Event> events=new LinkedList<Event>();
		events.add(new PersonDepartureEvent(20, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car, TransportMode.car));
		events.add(new PersonArrivalEvent(30, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonDepartureEvent(90, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car, TransportMode.car));
		events.add(new PersonArrivalEvent(110, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car));

		assertEquals(20.0, EventLibrary.getTravelTime(events,1), MatsimTestUtils.EPSILON);
	}

	 @Test
	 void testGetAverageTravelTime(){
		LinkedList<Event> events=new LinkedList<Event>();
		events.add(new PersonDepartureEvent(20, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car, TransportMode.car));
		events.add(new PersonArrivalEvent(30, Id.create("2", Person.class), Id.create("0", Link.class), TransportMode.car));
		events.add(new PersonDepartureEvent(90, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car, TransportMode.car));
		events.add(new PersonArrivalEvent(110, Id.create("1", Person.class), Id.create("0", Link.class), TransportMode.car));

		assertEquals(30.0, EventLibrary.getSumTravelTime(events), MatsimTestUtils.EPSILON);
	}

}

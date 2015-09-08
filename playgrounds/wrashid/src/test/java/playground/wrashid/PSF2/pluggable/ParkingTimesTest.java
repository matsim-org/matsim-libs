/* *********************************************************************** *
 * project: org.matsim.*
 * EventsTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.pluggable;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;

public class ParkingTimesTest extends MatsimTestCase {

	public void testBasic(){
		String eventsFile=getPackageInputDirectory() + "0.events.xml";
		ParkingTimesPlugin parkingTimesPlugin = getParkintTimes(eventsFile);
		
		assertEquals(2, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).size());
		assertEquals(22500, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(0).getArrivalTime(),1.0);
		assertEquals(35700, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(0).getDepartureTime(),1.0);
		assertEquals(38040, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(1).getArrivalTime(),1.0);
		assertEquals(21600, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(1).getDepartureTime(),1.0);
	}
	
	public void testAgent2HasNoCarLegs(){
		String eventsFile=getPackageInputDirectory() +"agent2HasNoCarLeg.events.xml";
		ParkingTimesPlugin parkingTimesPlugin = getParkintTimes(eventsFile);
		
		assertEquals(0, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(2, Person.class)).size());
	}
	
	public void testAgent2UsesCarNotAsModeForFirstLeg(){
		String eventsFile=getPackageInputDirectory() +"agent2UsesCarNotAsModeForFirstLeg.events.xml";
		ParkingTimesPlugin parkingTimesPlugin = getParkintTimes(eventsFile);
		
		assertEquals(1, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(2, Person.class)).size());
		assertEquals(38040, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(2, Person.class)).get(0).getArrivalTime(),1.0);
		assertEquals(35700, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(2, Person.class)).get(0).getDepartureTime(),1.0);
	}
	
	private ParkingTimesPlugin getParkintTimes(String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		events.addHandler(parkingTimesPlugin);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		return parkingTimesPlugin;
	}
	
	public void testWithParkingLocationsFilterHome(){
		String eventsFile=getPackageInputDirectory() +"0.events.xml";
		EventsManager events = EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		LinkedList<String> actTypesFilter=new LinkedList<String>();
		actTypesFilter.add("h");
		parkingTimesPlugin.setActTypesFilter(actTypesFilter);
		
		events.addHandler(parkingTimesPlugin);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		assertEquals(1, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).size());
		assertEquals("h", parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).getFirst().getActTypeOfFirstActDuringParking());
		assertEquals(38040, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(0).getArrivalTime(),1.0);
		assertEquals(21600, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(0).getDepartureTime(),1.0);
	}
	
	
	public void testWithParkingLocationsFilterWork(){
		String eventsFile=getPackageInputDirectory() +"0.events.xml";
		EventsManager events = EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		LinkedList<String> actTypesFilter=new LinkedList<String>();
		actTypesFilter.add("w");
		parkingTimesPlugin.setActTypesFilter(actTypesFilter);
		
		events.addHandler(parkingTimesPlugin);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		assertEquals(1, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).size());
		assertEquals("w", parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).getFirst().getActTypeOfFirstActDuringParking());
		assertEquals(22500, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(0).getArrivalTime(),1.0);
		assertEquals(35700, parkingTimesPlugin.getParkingTimeIntervals().get(Id.create(1, Person.class)).get(0).getDepartureTime(),1.0);
	}
	
}

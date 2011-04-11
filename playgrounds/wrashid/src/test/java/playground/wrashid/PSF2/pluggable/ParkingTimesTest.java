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

import org.junit.Ignore;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;

public class ParkingTimesTest extends MatsimTestCase {

	public void testBasic(){
		String eventsFile=getPackageInputDirectory() + "0.events.txt.gz";
		ParkingTimesPlugin parkingTimesPlugin = getParkintTimes(eventsFile);
		
		assertEquals(2, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).size());
		assertEquals(22500, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(0).getArrivalTime(),1.0);
		assertEquals(35700, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(0).getDepartureTime(),1.0);
		assertEquals(38040, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(1).getArrivalTime(),1.0);
		assertEquals(21600, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(1).getDepartureTime(),1.0);
	}
	
	public void testAgent2HasNoCarLegs(){
		String eventsFile=getPackageInputDirectory() +"agent2HasNoCarLeg.events.txt";
		ParkingTimesPlugin parkingTimesPlugin = getParkintTimes(eventsFile);
		
		assertEquals(0, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(2)).size());
	}
	
	public void testAgent2UsesCarNotAsModeForFirstLeg(){
		String eventsFile=getPackageInputDirectory() +"agent2UsesCarNotAsModeForFirstLeg.events.txt";
		ParkingTimesPlugin parkingTimesPlugin = getParkintTimes(eventsFile);
		
		assertEquals(1, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(2)).size());
		assertEquals(38040, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(2)).get(0).getArrivalTime(),1.0);
		assertEquals(35700, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(2)).get(0).getDepartureTime(),1.0);
	}
	
	private ParkingTimesPlugin getParkintTimes(String eventsFile) {
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		events.addHandler(parkingTimesPlugin);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		return parkingTimesPlugin;
	}
	
	public void testWithParkingLocationsFilterHome(){
		String eventsFile=getPackageInputDirectory() +"0.events.txt.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		LinkedList<String> actTypesFilter=new LinkedList<String>();
		actTypesFilter.add("h");
		parkingTimesPlugin.setActTypesFilter(actTypesFilter);
		
		events.addHandler(parkingTimesPlugin);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		assertEquals(1, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).size());
		assertEquals("h", parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).getFirst().getActTypeOfFirstActDuringParking());
		assertEquals(38040, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(0).getArrivalTime(),1.0);
		assertEquals(21600, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(0).getDepartureTime(),1.0);
	}
	
	
	public void testWithParkingLocationsFilterWork(){
		String eventsFile=getPackageInputDirectory() +"0.events.txt.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		LinkedList<String> actTypesFilter=new LinkedList<String>();
		actTypesFilter.add("w");
		parkingTimesPlugin.setActTypesFilter(actTypesFilter);
		
		events.addHandler(parkingTimesPlugin);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		assertEquals(1, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).size());
		assertEquals("w", parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).getFirst().getActTypeOfFirstActDuringParking());
		assertEquals(22500, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(0).getArrivalTime(),1.0);
		assertEquals(35700, parkingTimesPlugin.getParkingTimeIntervals().get(new IdImpl(1)).get(0).getDepartureTime(),1.0);
	}
	
}

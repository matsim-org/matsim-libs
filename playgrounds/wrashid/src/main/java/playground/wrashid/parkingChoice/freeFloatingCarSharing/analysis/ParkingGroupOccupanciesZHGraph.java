/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class ParkingGroupOccupanciesZHGraph {

	public static void main(String[] args) {
		String eventsFile = args[0];

		EventsManager events = EventsUtils.createEventsManager();

		ParkingGroupOccupanciesZH parkingGroupOccupanciesZH = new ParkingGroupOccupanciesZH();

		events.addHandler(parkingGroupOccupanciesZH);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		parkingGroupOccupanciesZH.showPlot();
	}

	

}

/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterXML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.pieter.singapore.utils.events.listeners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class TrimEventsWithPersonIds implements EventWriter, BasicEventHandler {
	private BufferedWriter out = null;
	private final HashSet<String> sampledIds;
	private HashSet<String> transitVehicleIds;
	private boolean listenForTransitDrivers = false;

	public void reset(int iteration) {
		closeFile();
	}

	public void closeFile() {
		if (this.out != null)
			try {
				this.out.write("</events>");
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void handleEvent(Event event) {
		StringBuilder eventXML = new StringBuilder(180);
		Map<String, String> attr = event.getAttributes();
		String person = attr.get("person");
		String driverId = attr.get("driverId");
		String vehicleId = attr.get("vehicleId");
		String vehicle = attr.get("vehicle");
		boolean copyEvent = false;
		if (sampledIds.contains(person) || sampledIds.contains(driverId))
			copyEvent = true;
		else if (sampledIds.contains(vehicle) && attr.get("type").startsWith("Vehicle"))
			copyEvent = true;
		else if (sampledIds.contains(vehicle) && person.startsWith("pt"))
			copyEvent = true;
		else if(sampledIds.contains(vehicleId) && driverId.startsWith("pt"))
			copyEvent = true;
		if (copyEvent) {
			if (listenForTransitDrivers = attr.get("vehicle") != null
                    && attr.get("vehicle").startsWith("tr_")) {
				if (transitVehicleIds == null) {
					transitVehicleIds = new HashSet<>();
				}
				transitVehicleIds.add(attr.get("vehicle"));
			}
			eventXML.append("\t<event ");
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				eventXML.append(entry.getKey());
				eventXML.append("=\"");
				eventXML.append(entry.getValue());
				eventXML.append("\" ");
			}
			eventXML.append(" />\n");
			try {
				this.out.write(eventXML.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public TrimEventsWithPersonIds(final String filename,
			HashSet<String> sampledIds, boolean listenForTransitDrivers) {
		init(filename);
		this.sampledIds = sampledIds;
		this.listenForTransitDrivers = listenForTransitDrivers;
	}

	void init(final String outfilename) {

		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public HashSet<String> getTransitVehicleIds() {
		return transitVehicleIds;
	}

}

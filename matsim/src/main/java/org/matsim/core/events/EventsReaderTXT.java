package org.matsim.core.events;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.IOException;

import static org.matsim.core.events.algorithms.EventWriterTXT.Number.*;

/**
 * Created by laemmel on 16/11/15.
 */
public final class EventsReaderTXT {

	private final EventsManager em;

	public EventsReaderTXT(EventsManager em) {
		this.em = em;
	}

	public void runEventsFile(String file) throws IOException {


		BufferedReader br = IOUtils.getBufferedReader(file);
		String l = br.readLine();
		l = br.readLine();//skip first line
		while (l != null) {
			String[] expl = StringUtils.explode(l, '\t');
			double time = Double.parseDouble(expl[0]);
			Id<Vehicle> vehicleId = Id.create(expl[1], Vehicle.class);
			Id<Link> linkId = Id.createLinkId(expl[3]);
			String type = expl[5];


			Event e = null;
			int typeIndex = Integer.parseInt(type);
			// (switch does not work with integer input. kai, mar'18)
			if (typeIndex == ActivityEnd.ordinal()) {
				e = new ActivityEndEvent(time, Id.createPersonId(vehicleId), linkId, null, null, null);
			} else if (typeIndex == PersonDeparture.ordinal()) {
				e = new PersonDepartureEvent(time, Id.createPersonId(vehicleId), linkId, "car", "car");
			} else if (typeIndex == VehicleEntersTraffic.ordinal()) {
				e = new VehicleEntersTrafficEvent(time, Id.createPersonId(vehicleId), linkId, vehicleId, "car", 0);
			} else if (typeIndex == LinkLeave.ordinal()) {
				e = new LinkLeaveEvent(time, vehicleId, linkId);
			} else if (typeIndex == LinkEnter.ordinal()) {
				e = new LinkEnterEvent(time, vehicleId, linkId);
			} else if (typeIndex == PersonArrival.ordinal()) {
				e = new PersonArrivalEvent(time, Id.createPersonId(vehicleId), linkId, "car");
			} else if (typeIndex == ActivityStart.ordinal()) {
				e = new ActivityStartEvent(time, Id.createPersonId(vehicleId), linkId, null, null, null );
			} else {
				throw new RuntimeException("Unsupported event type:" + l);
			}
			this.em.processEvent(e);
			l = br.readLine();
		}
		br.close();
	}
}

package playground.gregor.analysis;
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

/**
 * Created by laemmel on 16/11/15.
 */
public class TxtEventsFileReader {

	private final EventsManager em;

	public TxtEventsFileReader(EventsManager em) {
		this.em = em;
	}

	public void runEventsFile(String file) throws IOException {


		BufferedReader br = IOUtils.getBufferedReader(file);
		String l = br.readLine();
		l = br.readLine();//skip first line
		while (l != null) {
			String[] expl = StringUtils.explode(l, '\t');
			double time = Double.parseDouble(expl[0]);
			Id<Vehicle> id = Id.create(expl[1], Vehicle.class);
			Id<Link> lId = Id.createLinkId(expl[3]);
			String type = expl[5];


			Event e = null;
			switch (type) {
				case "8":
					//actend
					e = new ActivityEndEvent(time, Id.createPersonId(id), lId, null, null);
					break;
				case "6":
					//departure
					e = new PersonDepartureEvent(time, Id.createPersonId(id), lId, "car");
					break;
				case "4":
					//wait2link
					e = new VehicleEntersTrafficEvent(time, Id.createPersonId(id), lId, id, "car", 0);
					break;
				case "2":
					//left link
					e = new LinkLeaveEvent(time, id, lId);
					break;
				case "5":
					//enter link
					e = new LinkEnterEvent(time, id, lId);
					break;
				case "0":
					//arrival
					e = new PersonArrivalEvent(time, Id.createPersonId(id), lId, "car");
					break;
				case "7":
					//actstart
					e = new ActivityStartEvent(time, Id.createPersonId(id), lId, null, null);
					break;
				default:
					throw new RuntimeException("Unsupported event type:" + l);
			}
			this.em.processEvent(e);
			l = br.readLine();
		}
		br.close();
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterTXT.java
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

package org.matsim.core.events.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

public class EventWriterTXT implements EventWriter, ActivityEndEventHandler, ActivityStartEventHandler, PersonArrivalEventHandler,
		PersonDepartureEventHandler, PersonStuckEventHandler, PersonMoneyEventHandler,
		VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private static final Logger LOG = LogManager.getLogger(EventWriterTXT.class);

	/* Implement all the different event handlers by its own. Future event types will no longer be
	 * suitable to be written to a TXT-format file, but will have additional attributes that need to be
	 * stored in XML. Explicitly listing the event handlers makes sure only events are written to TXT that
	 * can also correctly be read in again, and helps fix the test cases during introduction of the new,
	 * additional events.
	 */

	private BufferedWriter out = null;
	private double lastTime = Double.NaN;
	private String timeString = null;

	private Map<Id<Vehicle>, Id<Person>> vehicleToDriverMap = new HashMap<>();

	public EventWriterTXT(final String filename) {
		init(filename);
	}

	@Override
	public void closeFile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	public void init(final String outfilename) {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		try {
			this.out = IOUtils.getBufferedWriter(outfilename);
			String eventsTxtFile = "T_GBL\t";
			eventsTxtFile +=  "VEH_ID\t";
			eventsTxtFile +=  "LEG_NR\t";
			eventsTxtFile +=  "LINK_ID\t";
			eventsTxtFile +=  "FROM_NODE_ID\t";
			eventsTxtFile +=  "EVENT_FLAG\t";
			eventsTxtFile +=  "DESCRIPTION\n";
			this.out.write(eventsTxtFile);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	public void reset(final int iter) {
		closeFile();
	}

	private void writeLine(final double time, final Id<Person> agentId, final Id<Link> linkId, final int flag, final String description) {
		try {
			this.out.write(getTimeString(time));
			if (agentId != null) {
				this.out.write(agentId.toString());
			}
			this.out.write('\t');
			// nothing to be written for leg-nr
			this.out.write('\t');
			if (linkId != null) {
				this.out.write(linkId.toString());
			}
			this.out.write('\t');
			this.out.write('0'); // from-node-id
			this.out.write('\t');
			this.out.write(Integer.toString(flag));
			this.out.write('\t');
			if (description != null) {
				this.out.write(description);
			}
			this.out.write('\n');
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * Returns the passed time as Seconds, including a trailing tab-character.
	 * Internally caches the returned result to speed up writing many events with the same time.
	 * This may no longer be useful if times are switched to double.
	 *
	 * @param time
	 * @return
	 */
	private String getTimeString(final double time) {
		if (time != this.lastTime) {
			this.lastTime = time;
			this.timeString = Long.toString((long) this.lastTime) + "\t";
		}
		return this.timeString;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), Number.ActivityEnd.ordinal(), ActivityEndEvent.EVENT_TYPE + " " + event.getActType());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), Number.ActivityStart.ordinal(), ActivityStartEvent.EVENT_TYPE + " " + event.getActType());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), Number.PersonArrival.ordinal(), PersonArrivalEvent.EVENT_TYPE);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), Number.PersonDeparture.ordinal(), PersonDepartureEvent.EVENT_TYPE);
	}

	public enum Number{ /*0:*/ PersonArrival, dummy, LinkLeave, PersonStuck, /*4:*/ VehicleEntersTraffic,
		/*5:*/ LinkEnter, PersonDeparture, ActivityStart, ActivityEnd, /*9:*/ PersonMoney }

	@Override
	public void handleEvent(PersonStuckEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), Number.PersonStuck.ordinal(), PersonStuckEvent.EVENT_TYPE);
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		writeLine(event.getTime(), event.getPersonId(), null, Number.PersonMoney.ordinal(), "agentMoney" + "\t" + event.getAmount());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), Number.VehicleEntersTraffic.ordinal(), VehicleEntersTrafficEvent.EVENT_TYPE);

		vehicleToDriverMap.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		writeLine(event.getTime(), vehicleToDriverMap.get(event.getVehicleId()), event.getLinkId(), Number.LinkEnter.ordinal(), LinkEnterEvent.EVENT_TYPE);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		writeLine(event.getTime(), vehicleToDriverMap.get(event.getVehicleId()), event.getLinkId(), Number.LinkLeave.ordinal(), LinkLeaveEvent.EVENT_TYPE);
	}
}

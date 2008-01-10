/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReaderTXTv1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.events;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.utils.StringUtils;
import org.matsim.utils.io.IOUtils;

public class EventsReaderTXTv1 {

	private BufferedReader infile = null;
	protected Events events;

	public EventsReaderTXTv1(final Events events) {
		super();
		this.events = events;
	}

	public void readFile(final String filename) {
		try {
			this.infile = IOUtils.getBufferedReader(filename);
			String line = this.infile.readLine();
			if (line != null && line.charAt(0) >= '0' && line.charAt(0) <= '9') {
				/* The line starts with a number, so assume it's an event and parse it.
				 * Otherwise it is likely the header.  */
				parseLine(line);
			}
			// now all other lines should contain events
			while ( (line = this.infile.readLine()) != null) {
				parseLine(line);
			}
			this.infile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static public final void createEvent(final Events events, final int time, final String agentId, final int legNumber,
			final String linkId, final int nodeId, final int flag, final String desc, final int activity, final String acttype) {
		BasicEvent data = null;

		switch (flag) {
			case 2:
				data = new EventLinkLeave(time, agentId, legNumber, linkId);
				break;
			case 5:
				data = new EventLinkEnter(time, agentId, legNumber, linkId);
				break;
			case 3:
				data = new EventAgentStuck(time, agentId, legNumber, linkId);
				break;
			case 4:
				data = new EventAgentWait2Link(time, agentId, legNumber, linkId);
				break;
			case 6:
				data = new EventAgentDeparture(time, agentId, legNumber, linkId);
				break;
			case 0:
				data = new EventAgentArrival(time, agentId, legNumber, linkId);
				break;
			case 7:
				if ("".equals(acttype) && desc != null) {
					data = new EventActivityStart(time, agentId, linkId, activity, desc.replace("actstart ", ""));
				} else {
					data = new EventActivityStart(time, agentId, linkId, activity, acttype);
				}
				break;
			case 8:
				if ("".equals(acttype) && desc != null) {
					data = new EventActivityEnd(time, agentId, linkId, activity, desc.replace("actend ", ""));
				} else {
					data = new EventActivityEnd(time, agentId, linkId, activity, acttype);
				}
				break;
		}
		if (data != null) events.processEvent(data);

	}


	protected void parseLine(final String line) {
		String[] result = StringUtils.explode(line, '\t', 7);
		if (result.length == 7) {
			createEvent(this.events, Integer.parseInt(result[0]),	// time
									result[1],		// vehID
									Integer.parseInt(result[2]),		// legNumber
									result[3],		// linkID
									Integer.parseInt(result[4]),		// nodeID
									Integer.parseInt(result[5]),		// flag
									result[6], 0, "");		// description
		}
	}

}

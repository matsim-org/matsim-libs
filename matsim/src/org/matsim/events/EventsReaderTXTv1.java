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

import org.matsim.basic.v01.IdImpl;
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

	static public final void createEvent(final Events events, final double time, final String agentId, final int legNumber,
			final String linkId, final int flag, final String desc, final String acttype) {
		BasicEvent data = null;

		switch (flag) {
			case 2:
				data = new LinkLeaveEvent(time, agentId, linkId, legNumber);
				break;
			case 5:
				data = new LinkEnterEvent(time, agentId, linkId, legNumber);
				break;
			case 3:
				data = new AgentStuckEvent(time, agentId, linkId, legNumber);
				break;
			case 4:
				data = new AgentWait2LinkEvent(time, agentId, linkId, legNumber);
				break;
			case 6:
				data = new AgentDepartureEvent(time, agentId, linkId, legNumber);
				break;
			case 0:
				data = new AgentArrivalEvent(time, agentId, linkId, legNumber);
				break;
			case 7:
				if ("".equals(acttype) && desc != null) {
					data = new ActStartEvent(time, agentId, linkId, desc.replace("actstart ", ""));
				} else {
					data = new ActStartEvent(time, agentId, linkId, acttype);
				}
				break;
			case 8:
				if ("".equals(acttype) && desc != null) {
					data = new ActEndEvent(time, agentId, linkId, desc.replace("actend ", ""));
				} else {
					data = new ActEndEvent(time, agentId, linkId, acttype);
				}
				break;
			case 9:
				data = new AgentMoneyEvent(time, new IdImpl(agentId), Double.parseDouble(desc.replace("agentMoney\t", "")));
				break;
			default:
				throw new RuntimeException("Type of events with flag = " + flag + " is not known!");
		}
		events.processEvent(data);

	}


	protected void parseLine(final String line) {
		String[] result = StringUtils.explode(line, '\t', 7);
		if (result.length == 7) {
			createEvent(this.events, Double.parseDouble(result[0]),	// time
									result[1],		// vehID
									Integer.parseInt(result[2]),		// legNumber
									result[3],		// linkID
									//Integer.parseInt(result[4]),		// nodeID
									Integer.parseInt(result[5]),		// flag
									result[6], "");		// description
		}
	}

}

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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityEndEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentWait2LinkEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.handler.AgentMoneyEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class EventWriterTXT implements EventWriter, BasicActivityEndEventHandler, BasicActivityStartEventHandler, BasicAgentArrivalEventHandler, 
		BasicAgentDepartureEventHandler, BasicAgentStuckEventHandler, AgentMoneyEventHandler, 
		BasicAgentWait2LinkEventHandler, BasicLinkEnterEventHandler, BasicLinkLeaveEventHandler {
	
	/* Implement all the different event handlers by its own. Future event types will no longer be
	 * suitable to be written to a TXT-format file, but will have additional attributes that need to be
	 * stored in XML. Explicitly listing the event handlers makes sure only events are written to TXT that
	 * can also correctly be read in again, and helps fix the test cases during introduction of the new,
	 * additional events.
	 */
	
	private BufferedWriter out = null;
	private double lastTime = Double.NaN;
	private String timeString = null;

	public EventWriterTXT(final String filename) {
		init(filename);
	}

	public void closeFile() {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void init(final String outfilename) {
		if (this.out != null) {
			try {
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	public void reset(final int iter) {
		closeFile();
	}

	private void writeLine(final double time, final Id agentId, final Id linkId, final int flag, final String description) {
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
			e.printStackTrace();
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

	public void handleEvent(BasicActivityEndEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 8, ActivityEndEventImpl.EVENT_TYPE + " " + event.getActType());
	}

	public void handleEvent(BasicActivityStartEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 7, ActivityStartEventImpl.EVENT_TYPE + " " + event.getActType());
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 0, AgentArrivalEventImpl.EVENT_TYPE);
	}

	public void handleEvent(BasicAgentDepartureEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 6, AgentDepartureEventImpl.EVENT_TYPE);
	}
	
	public void handleEvent(BasicAgentStuckEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 3, AgentStuckEventImpl.EVENT_TYPE);
	}

	public void handleEvent(AgentMoneyEventImpl event) {
		writeLine(event.getTime(), event.getPersonId(), null, 9, AgentMoneyEventImpl.EVENT_TYPE + "\t" + event.getAmount());
	}

	public void handleEvent(BasicAgentWait2LinkEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 4, AgentWait2LinkEventImpl.EVENT_TYPE);
	}

	public void handleEvent(BasicLinkEnterEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 5, LinkEnterEventImpl.EVENT_TYPE);
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		writeLine(event.getTime(), event.getPersonId(), event.getLinkId(), 2, LinkLeaveEventImpl.EVENT_TYPE);
	}
}

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

package org.matsim.events.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentReplanEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentUtilityEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentReplanEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentUtilityEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.utils.io.IOUtils;

public class EventWriterTXT implements ActEndEventHandler, ActStartEventHandler, AgentArrivalEventHandler, 
		AgentDepartureEventHandler, AgentReplanEventHandler, AgentStuckEventHandler, AgentUtilityEventHandler, 
		AgentWait2LinkEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
	
	/* Implement all the different event handlers by its own. Future event types will no longer be
	 * suitable to be written to a TXT-format file, but will have additional attributes that need to be
	 * stored in XML. Explicitly listing the event handlers makes sure only events are written to TXT that
	 * can also correctly be read in again, and helps fix the test cases during introduction of the new,
	 * additional events.
	 */
	
	private BufferedWriter out = null;

	public EventWriterTXT(final String filename) {
		init(filename);
	}

	public void closefile() {
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
		closefile();
	}

	private void writeLine(final String line) {
		try {
			this.out.write(line);
			this.out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(ActEndEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(ActStartEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(AgentArrivalEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(AgentDepartureEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(AgentReplanEvent event) {
		writeLine(event.toString());
	}
	
	public void handleEvent(AgentStuckEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(AgentUtilityEvent event) {
		writeLine(event.toString());		
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(LinkEnterEvent event) {
		writeLine(event.toString());
	}

	public void handleEvent(LinkLeaveEvent event) {
		writeLine(event.toString());
	}
}

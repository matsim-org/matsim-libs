/* *********************************************************************** *
 * project: org.matsim.*
 * Event.java
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

package teach.multiagent07.util;

import org.matsim.basic.v01.BasicLink;
import org.matsim.utils.identifiers.IdI;

public class Event {
	// Event Types
	public static final int UNKNOWN = 0;
	public static final int ENTER_LINK = 1;
	public static final int LEAVE_LINK = 2;

	public static final int ACT_DEPARTURE = 3;
	public static final int ACT_ARRIVAL = 4;

	public int time = 0;
	public int type = UNKNOWN;
	public BasicLink link;
	public IdI agentId;
	public int legNumber = -1;

	public Event( int time, int type, BasicLink link, IdI agentId) {
		this.time = time;
		this.type = type;
		this.link = link;
		this.agentId = agentId;
	}

	public Event( int time, int type, BasicLink link, IdI agentId, int legNumber) {
		this(time, type, link, agentId);
		this.legNumber = legNumber;
	}

	@Override
	public String toString() {
		String typstring = "UNKNOWN";
		switch (type) {
		case 1:
			typstring = "ENTER LINK";
			break;
		case 2:
			typstring = "LEAVE LINK";
			break;
		case 3:
			typstring = "ACTIVITY DEPARTURE";
			break;
		case 4:
			typstring = "ACTIVITY ARRIVAL";
			break;
		}
		return "Event at " + time + " sec : " +typstring + "on Link " + link.toString() + " with agent " + agentId;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.toronto.example;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

// a simple example how to implement an eventsHandler
// More eventsHandler can be found under org.matsim.events.*
// compare them with example events files
// more examples can be found under test/src: org.matsim.events
public class DailyLinkVolumeCalc implements LinkLeaveEventHandler {

	// stores the number of leaveLinkEvents per link
	private static TreeMap<Id,Integer> counts = new TreeMap<Id, Integer>();

	// implementation of a LinkLeaveEventHandler method
	// it fills up the TreeMap
	public void handleEvent(LinkLeaveEvent event) {
		Id id = new IdImpl(event.getLinkId().toString());
		Integer cnt = counts.get(id);
		if (cnt == null) {
			counts.put(id,1);
		}
		else {
			counts.put(id,cnt+1);
		}
	}

	// implementation of a LinkLeaveEventHandler method
	// After each iteration of MATSim the TreeMap will be
	// cleaned up. (This is not important for a Handler that
	// is only used as a post processing step after the
	// MATSim optimization.)
	public void reset(int iteration) {
		counts.clear();
	}
	
	// print the TreeMap to the standard out
	public final void writeTable() {
		System.out.println("Link_id"+"\t"+"daily_volume");
		for (Id id : counts.keySet()) {
			Integer cnt = counts.get(id);
			System.out.println(id.toString()+"\t"+cnt);
		}
	}
}

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

package playground.balmermi.datapuls.modules;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

public class DailyLinkVolumeCalc implements LinkLeaveEventHandler {

	private static TreeMap<Id,Integer> counts = new TreeMap<Id, Integer>();

	public void handleEvent(LinkLeaveEvent event) {
		Id id = event.getLinkId();
		Integer cnt = counts.get(id);
		if (cnt == null) {
			counts.put(id,1);
		}
		else {
			counts.put(id,cnt+1);
		}
	}

	public void reset(int iteration) {
		counts.clear();
	}
	
	public final void writeTable() {
		System.out.println("Link_id"+"\t"+"daily_volume");
		for (Id id : counts.keySet()) {
			Integer cnt = counts.get(id);
			System.out.println(id.toString()+"\t"+cnt);
		}
	}
}

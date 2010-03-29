/* *********************************************************************** *
 * project: org.matsim.*
 * StuckVehStats.java
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

package org.matsim.analysis;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.utils.misc.Time;

public class StuckVehStats implements AgentDepartureEventHandler, AgentStuckEventHandler, AgentWait2LinkEventHandler {

	private TreeMap<Id, ArrayList<Double>> stuckLinkTimes = new TreeMap<Id, ArrayList<Double>>(); // <Link, <Time>>, the times an agent is stuck for each link
	private int[] stuckTimes = new int[24*4 + 1]; // the time of day agents get stuck; counts per 15min-slots; up to 24 hours
	private TreeMap<Id, Double> depTimes = new TreeMap<Id, Double>(); // the time of an agent's last departure event
	private TreeMap<Id, Double> wait2linkTimes = new TreeMap<Id, Double>(); // the time of an agent's last wait2link-event
	private int[] waitTimes = new int[2*60 + 1]; // the time an agent spends waiting to enter a link from parking; counts per minute up to 2 hours
	private int[] driveTimes = new int[2*60 + 1]; // the time an agent spends driving until it is stuck; counts per minute up to 2 hours
	private int[] travelTimes = new int[2*60 + 1]; // the time an agent spends traveling (wait2link + drive) until it is stuck
	private Network network = null;

	public StuckVehStats(Network network) {
		this.network = network;
		reset(-1);
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.depTimes.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		this.wait2linkTimes.put(event.getPersonId(), event.getTime());
		Double depTime = this.depTimes.get(event.getPersonId());
		if (depTime != null) {
			int slot = (int)((event.getTime() - depTime) / 60);
			if (slot > 120) slot = 120;
			this.waitTimes[slot]++;
		}
	}

	public void handleEvent(AgentStuckEvent event) {
		ArrayList<Double> times = this.stuckLinkTimes.get(event.getLinkId());
		if (times == null) {
			times = new ArrayList<Double>(50);
		}
		times.add(event.getTime());
		this.stuckLinkTimes.put(event.getLinkId(), times);

		int timeslot = (int)(event.getTime() / 900);
		if (timeslot > 24*4) timeslot = 24*4;
		this.stuckTimes[timeslot]++;

		Double wait2linkTime = this.wait2linkTimes.remove(event.getPersonId());
		if (wait2linkTime != null) {
			int slot = (int)((event.getTime() - wait2linkTime) / 60);
			if (slot > 120) slot = 120;
			this.driveTimes[slot]++;
		}

		Double depTime = this.depTimes.remove(event.getPersonId());
		if (depTime != null) {
			int slot = (int)((event.getTime() - depTime) / 60);
			if (slot > 120) slot = 120;
			this.travelTimes[slot]++;
		}
	}


	public void reset(int iteration) {
		for (int i = 0; i < this.stuckTimes.length; i++) {
			this.stuckTimes[i] = 0;
		}
		this.stuckLinkTimes.clear();
		this.depTimes.clear();
		this.wait2linkTimes.clear();
		for (int i = 0; i < this.waitTimes.length; i++) {
			this.waitTimes[i] = 0;
		}
		for (int i = 0; i < this.driveTimes.length; i++) {
			this.driveTimes[i] = 0;
		}
		for (int i = 0; i < this.travelTimes.length; i++) {
			this.travelTimes[i] = 0;
		}
	}

	public void printResults() {
		System.out.println("===   S T U C K   V E H I C L E S   ===");
		System.out.println("number of stuck vehicles / time of day");
		for (int i = 0; i < this.stuckTimes.length; i++) {
			System.out.println((i*900) + "\t" + Time.writeTime(i*900) + "\t" + this.stuckTimes[i]);
		}
		System.out.println();
		System.out.println("number of stuck vehicles / time after wait2link");
		for (int i = 0; i < this.driveTimes.length; i++) {
			System.out.println((i*60) + "\t" + Time.writeTime(i*60) + "\t" + this.driveTimes[i]);
		}
		System.out.println();
		System.out.println("number of stuck vehicles / time after departure (incl. wait2link-time)");
		for (int i = 0; i < this.travelTimes.length; i++) {
			System.out.println((i*60) + "\t" + Time.writeTime(i*60) + "\t" + this.travelTimes[i]);
		}
		System.out.println();
		System.out.println("wait2link time distribution");
		for (int i = 0; i < this.waitTimes.length; i++) {
			System.out.println((i*60) + "\t" + Time.writeTime(i*60) + "\t" + this.waitTimes[i]);
		}
		System.out.println();
		System.out.println("Links on which vehicles get stuck");
		System.out.println("LINK\tCAPACITY\tFREESPEED\tLENGTH\tcountStuck\ttimesStuck");
		for (Id linkId : this.stuckLinkTimes.keySet()) {
			ArrayList<Double> times = this.stuckLinkTimes.get(linkId);
			Link link = this.network.getLinks().get(linkId);
			System.out.print(linkId + "\t" + link.getCapacity() + "\t" + link.getFreespeed() + "\t" + link.getLength() + "\t" + times.size() + "\t");
			for (Double time : times) System.out.print(time + " ");
			System.out.println();
		}
	}

}

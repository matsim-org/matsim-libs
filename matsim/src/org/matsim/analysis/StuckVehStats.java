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

import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.misc.Time;

public class StuckVehStats implements AgentDepartureEventHandler, AgentStuckEventHandler, AgentWait2LinkEventHandler {

	private TreeMap<String, ArrayList<Double>> stuckLinkTimes = new TreeMap<String, ArrayList<Double>>(); // <Link, <Time>>, the times an agent is stuck for each link
	private int[] stuckTimes = new int[24*4 + 1]; // the time of day agents get stuck; counts per 15min-slots; up to 24 hours
	private TreeMap<String, Double> depTimes = new TreeMap<String, Double>(); // the time of an agent's last departure event
	private TreeMap<String, Double> wait2linkTimes = new TreeMap<String, Double>(); // the time of an agent's last wait2link-event
	private int[] waitTimes = new int[2*60 + 1]; // the time an agent spends waiting to enter a link from parking; counts per minute up to 2 hours
	private int[] driveTimes = new int[2*60 + 1]; // the time an agent spends driving until it is stuck; counts per minute up to 2 hours
	private int[] travelTimes = new int[2*60 + 1]; // the time an agent spends traveling (wait2link + drive) until it is stuck
	private NetworkLayer network = null;
	
	public StuckVehStats(NetworkLayer network) {
		this.network = network;
		reset(-1);
	}
	
	public void handleEvent(AgentDepartureEvent event) {
		depTimes.put(event.agentId, event.time);
	}

	// DS TODO can not see if slot handling is corrupted through int -> double transition MR??
	public void handleEvent(AgentWait2LinkEvent event) {
		wait2linkTimes.put(event.agentId, event.time);
		Double depTime = depTimes.get(event.agentId);
		if (depTime != null) {
			int slot = (int)((event.time - depTime) / 60);
			if (slot > 120) slot = 120;
			waitTimes[slot]++;
		}
	}

	public void handleEvent(AgentStuckEvent event) {
		ArrayList<Double> times = stuckLinkTimes.get(event.linkId);			
		if (times == null) {
			times = new ArrayList<Double>(50);
		}
		times.add(event.time);
		stuckLinkTimes.put(event.linkId, times);
		
		int timeslot = (int)(event.time / 900);
		if (timeslot > 24*4) timeslot = 24*4;
		stuckTimes[timeslot]++;
		
		Double wait2linkTime = wait2linkTimes.remove(event.agentId);
		if (wait2linkTime != null) {
			int slot = (int)((event.time - wait2linkTime) / 60);
			if (slot > 120) slot = 120;
			driveTimes[slot]++;
		}
		
		Double depTime = depTimes.remove(event.agentId);
		if (depTime != null) {
			int slot = (int)((event.time - depTime) / 60);
			if (slot > 120) slot = 120;
			travelTimes[slot]++;
		}
	}


	public void reset(int iteration) {
		for (int i = 0; i < stuckTimes.length; i++) {
			stuckTimes[i] = 0;
		}
		stuckLinkTimes.clear();
		depTimes.clear();
		wait2linkTimes.clear();
		for (int i = 0; i < waitTimes.length; i++) {
			waitTimes[i] = 0;
		}
		for (int i = 0; i < driveTimes.length; i++) {
			driveTimes[i] = 0;
		}
		for (int i = 0; i < travelTimes.length; i++) {
			travelTimes[i] = 0;
		}
	}
	
	public void printResults() {
		System.out.println("===   S T U C K   V E H I C L E S   ===");
		System.out.println("number of stuck vehicles / time of day");
		for (int i = 0; i < stuckTimes.length; i++) {
			System.out.println((i*900) + "\t" + Time.writeTime(i*900) + "\t" + stuckTimes[i]);
		}
		System.out.println();
		System.out.println("number of stuck vehicles / time after wait2link");
		for (int i = 0; i < driveTimes.length; i++) {
			System.out.println((i*60) + "\t" + Time.writeTime(i*60) + "\t" + driveTimes[i]);
		}
		System.out.println();
		System.out.println("number of stuck vehicles / time after departure (incl. wait2link-time)");
		for (int i = 0; i < travelTimes.length; i++) {
			System.out.println((i*60) + "\t" + Time.writeTime(i*60) + "\t" + travelTimes[i]);
		}
		System.out.println();
		System.out.println("wait2link time distribution");
		for (int i = 0; i < waitTimes.length; i++) {
			System.out.println((i*60) + "\t" + Time.writeTime(i*60) + "\t" + waitTimes[i]);
		}
		System.out.println();
		System.out.println("Links on which vehicles get stuck");
		System.out.println("LINK\tCAPACITY\tFREESPEED\tLENGTH\tcountStuck\ttimesStuck");
		for (String linkId : stuckLinkTimes.keySet()) {
			ArrayList<Double> times = stuckLinkTimes.get(linkId);
			Link link = (Link)network.getLocation(linkId);
			System.out.print(linkId + "\t" + link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME) + "\t" + link.getFreespeed(Time.UNDEFINED_TIME) + "\t" + link.getLength() + "\t" + times.size() + "\t");
			for (Double time : times) System.out.print(time + " ");
			System.out.println();
		}
	}
	
}

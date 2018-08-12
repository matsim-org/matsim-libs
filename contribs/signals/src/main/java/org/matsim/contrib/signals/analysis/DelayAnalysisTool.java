/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author tthunig
 */
public class DelayAnalysisTool implements VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	private double totalDelay = 0.0;
	private Map<Id<Link>, Double> totalDelayPerLink = new HashMap<>();
	private Map<Id<Link>, Integer> numberOfVehPerLink = new HashMap<>();
	private Map<Id<Link>,LinkedList<Id<Person>>> agentsStuckedAtLink = new HashMap<>();
	private Map<Id<Vehicle>, Double> veh2earliestLinkExitTime = new HashMap<>();
	
	private final Network network;
	
	public DelayAnalysisTool(Network network) {
		this.network = network;
	}
	
	@Inject
	public DelayAnalysisTool(Network network, EventsManager em) {
		this(network);
		em.addHandler(this);
	}
	
	@Override
	public void reset(int iteration) {
		this.totalDelay = 0.0;
		this.veh2earliestLinkExitTime.clear();
		this.totalDelayPerLink.clear();
		this.numberOfVehPerLink.clear();
		this.agentsStuckedAtLink.clear();
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		// for the first link every vehicle needs one second without delay
		veh2earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + 1);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// calculate earliest link exit time
		Link currentLink = network.getLinks().get(event.getLinkId());
		double freespeedTt = currentLink.getLength() / currentLink.getFreespeed();
		// this is the earliest time where matsim sets the agent to the next link
		double matsimFreespeedTT = Math.floor(freespeedTt + 1);	
		veh2earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + matsimFreespeedTT);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// initialize link based analysis data structure
		if (!totalDelayPerLink.containsKey(event.getLinkId())) {
			totalDelayPerLink.put(event.getLinkId(), 0.0);
			numberOfVehPerLink.put(event.getLinkId(), 0);
		}
		// calculate delay
		double currentDelay = event.getTime() - veh2earliestLinkExitTime.get(event.getVehicleId());
		totalDelayPerLink.put(event.getLinkId(), totalDelayPerLink.get(event.getLinkId()) + currentDelay);
		totalDelay += currentDelay;

		int vehCount = numberOfVehPerLink.get(event.getLinkId());
		numberOfVehPerLink.put(event.getLinkId(), ++vehCount);
	}
	
	public double getTotalDelay(){
		return totalDelay;
	}
	
	public Map<Id<Link>, Double> getTotalDelayPerLink(){
		return totalDelayPerLink;
	}

	public Map<Id<Link>, Double> getAvgDelayPerLink(){
		Map<Id<Link>, Double> avgDelayMap = new HashMap<>();
		for (Id<Link> linkId : totalDelayPerLink.keySet()){
			avgDelayMap.put(linkId, totalDelayPerLink.get(linkId) / numberOfVehPerLink.get(linkId));
		}
		return avgDelayMap;
	}

	
}

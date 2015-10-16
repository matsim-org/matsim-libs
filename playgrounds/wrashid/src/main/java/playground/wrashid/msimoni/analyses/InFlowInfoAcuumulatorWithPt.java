/* *********************************************************************** *
 * project: org.matsim.*
 * InFlowInfoAcuumulatorWithPt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.wrashid.msimoni.analyses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;

public class InFlowInfoAcuumulatorWithPt implements LinkEnterEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final int binSizeInSeconds; // set the length of interval
	private final int numBins;

	public HashMap<Id, int[]> linkInFlow;
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks;
	private Map<Id, Integer> entersPerLink;
	private Set<Id> carAgents;

	
	public InFlowInfoAcuumulatorWithPt(Map<Id<Link>, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds) {
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
		this.numBins = (86400 / this.binSizeInSeconds) + 1;
	}

	@Override
	public void reset(int iteration) {
		// reset the variables (private ones)
		linkInFlow = new HashMap<Id, int[]>();
		entersPerLink = new HashMap<Id, Integer>();
		carAgents = new HashSet<Id>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		// ignore non-car travelers
		if (!this.carAgents.contains(event.getDriverId())) return;
				
		// call from NetworkReadExample
		enterLink(event.getLinkId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		if (event.getLegMode().equals(TransportMode.car)) {
			carAgents.add(event.getPersonId());
			enterLink(event.getLinkId(), event.getTime());		
		}		
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
		this.carAgents.remove(event.getPersonId());
	}
	
	private void enterLink(Id linkId, double time) {
		
		if (time > 86400) return;
		
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (!linkInFlow.containsKey(linkId)) {
			// set the number of intervals
			linkInFlow.put(linkId, new int[numBins]); 
		}

		int[] bins = linkInFlow.get(linkId);
		
		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

		Integer enteredVehicles = this.entersPerLink.get(linkId);
		if (enteredVehicles == null) enteredVehicles = 1;
		else enteredVehicles++;
		
		// update map
		this.entersPerLink.put(linkId, enteredVehicles);
		
		// count the number of agents entered the link so far
		for (int i = binIndex; i < numBins; i++) bins[i] = enteredVehicles;
	}

	public void printLinkInFlow() { 
		// print
		for (Id linkId : linkInFlow.keySet()) {
			int[] bins = linkInFlow.get(linkId);

			Link link = filteredEquilNetLinks.get(linkId);

			System.out.print(linkId + " - " + link.getCoord() + ": ");

			for (int i = 0; i < bins.length; i++) {
				System.out.print(bins[i] * 3600 / binSizeInSeconds + "\t");
			}

			System.out.println();
		}
	}

	public HashMap<Id, int[]> getLinkInFlow() {
		return linkInFlow;
	}

}
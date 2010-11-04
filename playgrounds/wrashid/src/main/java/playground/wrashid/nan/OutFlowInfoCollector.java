package playground.wrashid.nan;

/* *********************************************************************** *
 * project: org.matsim.*
 * DensityInfoCollector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;

public class OutFlowInfoCollector implements LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentArrivalEventHandler {

	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id, int[]> linkOutFlow; // define
	private Map<Id, Link> filteredEquilNetLinks; // define
	
	// personId, linkId
	private HashMap<Id, Id> lastEnteredLink=new HashMap<Id, Id>(); // define
	
	private boolean isOldEventFile;

	public OutFlowInfoCollector(Map<Id, Link> filteredEquilNetLinks,
			boolean isOldEventFile,int binSizeInSeconds) { // to create the class FlowInfoCollector
		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.isOldEventFile = isOldEventFile;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	public void reset(int iteration) {
		linkOutFlow = new HashMap<Id, int[]>(); // reset the variables (private
												// ones)
	}

	public void handleEvent(LinkLeaveEvent event) { // call from
													// NetworkReadExample
		linkLeave(event.getLinkId(), event.getTime());
	}

	private void linkLeave(Id linkId, double time) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (!linkOutFlow.containsKey(linkId)) {
			linkOutFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]); // set
																				// the
																				// number
																				// of
																				// intervals
		}

		int[] bins = linkOutFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

		if (time < 86400) {
			bins[binIndex] = bins[binIndex] + 1; // count the number of agents
													// for each link at each
													// time interval
		}
	}

	public void printLinkFlows() { // print
		for (Id linkId : linkOutFlow.keySet()) {
			int[] bins = linkOutFlow.get(linkId);

			Link link = filteredEquilNetLinks.get(linkId);
			System.out.print(linkId + " - " + link.getCoord() + ": ");

			for (int i = 0; i < bins.length; i++) {
				System.out.print(bins[i] * 3600 / binSizeInSeconds + "\t");
			}

			System.out.println();
		}
	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return linkOutFlow;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId())!=null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())){
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getPersonId(),null); //reset value
			}
			
			
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getPersonId(), event.getLinkId());
	}

}

package playground.wrashid.fd;

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
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.wrashid.lib.obj.TwoKeyHashMapsWithDouble;


// TODO: accumulate
public class DensityInfoCollectorWithPt implements LinkLeaveEventHandler,
		LinkEnterEventHandler, AgentArrivalEventHandler {

	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id, int[]> density; // define
	private Map<Id, ? extends Link> filteredEquilNetLinks; // define
	private TwoKeyHashMapsWithDouble<Id, Id> linkEnterTime=new TwoKeyHashMapsWithDouble<Id, Id>();

	// personId, linkId
	private HashMap<Id, Id> lastEnteredLink = new HashMap<Id, Id>(); // define

	public DensityInfoCollectorWithPt(
			Map<Id, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds) { // to create the
															// class
															// FlowInfoCollector
		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		density = new HashMap<Id, int[]>(); // reset the variables (private
												// ones)
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) { // call from
													// NetworkReadExample
		linkLeave(event.getLinkId(), event.getTime(), event.getPersonId());
	}

	private void linkLeave(Id linkId, double time, Id personId) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (!density.containsKey(linkId)) {
			density.put(linkId, new int[(86400 / binSizeInSeconds) + 1]); // set
																				// the
																				// number
																				// of
																				// intervals
		}

		int[] bins = density.get(linkId);

		if (time < 86400) {
			int startBinIndex = (int) Math.round(Math.floor(GeneralLib.projectTimeWithin24Hours(linkEnterTime.get(linkId, personId)) / binSizeInSeconds));
			int endBinIndex = (int) Math.round(Math.floor(GeneralLib.projectTimeWithin24Hours(time / binSizeInSeconds)));
			
			for (int i=startBinIndex;i<=endBinIndex;i++){
				bins[i]++;
			}
		}
	}

	public void printLinkFlows() { // print
		for (Id linkId : density.keySet()) {
			int[] bins = density.get(linkId);

			Link link = filteredEquilNetLinks.get(linkId);

			boolean hasTraffic = false;
			for (int i = 0; i < bins.length; i++) {
				if (bins[i] != 0.0) {
					hasTraffic = true;
					break;
				}
			}

			if (hasTraffic) {
				System.out.print(linkId + " - " + link.getCoord() + ": \t");

				for (int i = 0; i < bins.length; i++) {
					System.out.print(bins[i] * 3600 / binSizeInSeconds + "\t");
				}

				System.out.println();
			}
		}
	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return density;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId())
				&& lastEnteredLink.get(event.getPersonId()) != null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(
					event.getLinkId())) {
				linkLeave(event.getLinkId(), event.getTime(),event.getPersonId());
				lastEnteredLink.put(event.getPersonId(), null); // reset value
			}

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEnterTime.put(event.getLinkId(), event.getPersonId(), event.getTime());
	}
	

}

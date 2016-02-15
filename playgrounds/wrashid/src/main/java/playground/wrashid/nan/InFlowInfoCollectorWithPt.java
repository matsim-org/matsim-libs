/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.nan;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;

public class InFlowInfoCollectorWithPt implements LinkEnterEventHandler,
		VehicleEntersTrafficEventHandler {

	private int binSizeInSeconds; // set the length of interval

	public HashMap<Id<Link>, int[]> linkInFlow;
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks; //

	private boolean isOldEventFile;

	public InFlowInfoCollectorWithPt(Map<Id<Link>, ? extends Link> filteredEquilNetLinks,
			boolean isOldEventFile, int binSizeInSeconds) {
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.isOldEventFile = isOldEventFile;
		this.binSizeInSeconds=binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		linkInFlow = new HashMap<>(); // reset the variables (private
												// ones)
	}

	@Override
	public void handleEvent(LinkEnterEvent event) { // call from
													// NetworkReadExample
		enterLink(event.getLinkId(), event.getTime());
	}

	private void enterLink(Id<Link> linkId, double time) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (!linkInFlow.containsKey(linkId)) {
			linkInFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]); // set
																				// the
																				// number
																				// of
																				// intervals
		}

		int[] bins = linkInFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

		if (time < 86400) {
			bins[binIndex] = bins[binIndex] + 1; // count the number of agents
													// each link each time
													// interval
		}
	}

	public void printLinkInFlow() { // print
		for (Id<Link> linkId : linkInFlow.keySet()) {
			int[] bins = linkInFlow.get(linkId);

			Link link = filteredEquilNetLinks.get(linkId);

			System.out.print(linkId + " - " + link.getCoord() + ": ");

			for (int i = 0; i < bins.length; i++) {
				System.out.print(bins[i] * 3600 / binSizeInSeconds + "\t");
			}

			System.out.println();
		}
	}

	public HashMap<Id<Link>, int[]> getLinkInFlow() {
		return linkInFlow;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		enterLink(event.getLinkId(), event.getTime());		
	}

}
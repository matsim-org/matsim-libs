package playground.wrashid.msimoni.analyses;

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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class OutFlowInfoCollectorWithPt extends AbstractFlowAccumulator implements LinkLeaveEventHandler,
		LinkEnterEventHandler, VehicleLeavesTrafficEventHandler {

	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id<Link>, int[]> linkOutFlow; // define
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks; // define

	// personId, linkId
	private HashMap<Id<Vehicle>, Id<Link>> lastEnteredLink = new HashMap<>(); // define

	public OutFlowInfoCollectorWithPt(
			Map<Id<Link>, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds) { // to create the
															// class
															// FlowInfoCollector
		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		linkOutFlow = new HashMap<Id<Link>, int[]>(); // reset the variables (private
												// ones)
	}

	@Override
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

	public HashMap<Id<Link>, int[]> getLinkOutFlow() {
		return linkOutFlow;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (lastEnteredLink.containsKey(event.getVehicleId())
				&& lastEnteredLink.get(event.getVehicleId()) != null) {
			if (lastEnteredLink.get(event.getVehicleId()).equals(
					event.getLinkId())) {
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getVehicleId(), null); // reset value
			}

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getVehicleId(), event.getLinkId());
	}
	
	@Override
	protected int[] getFlow(Id linkId) {
		return linkOutFlow.get(linkId);
	}

}

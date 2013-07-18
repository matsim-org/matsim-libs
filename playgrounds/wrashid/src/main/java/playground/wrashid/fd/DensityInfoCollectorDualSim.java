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
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.wrashid.lib.obj.TwoKeyHashMapsWithDouble;

// TODO: accumulate
public class DensityInfoCollectorDualSim extends AbstractDualSimHandler {

	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id, int[]> density; // define
	private Map<Id, ? extends Link> filteredEquilNetLinks; // define
	private TwoKeyHashMapsWithDouble<Id, Id> linkEnterTime = new TwoKeyHashMapsWithDouble<Id, Id>();

	private boolean isJDEQSim;

	public DensityInfoCollectorDualSim(
			Map<Id, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds, boolean isJDEQSim) {
		this.isJDEQSim = isJDEQSim;

		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		density = new HashMap<Id, int[]>();
	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return density;
	}

	@Override
	public boolean isJDEQSim() {
		return isJDEQSim;
	}

	@Override
	public boolean isLinkPartOfStudyArea(Id linkId) {
		return filteredEquilNetLinks.containsKey(linkId);
	}

	@Override
	public void processLeaveLink(Id linkId, Id personId, double time) {

		if (!density.containsKey(linkId)) {
			density.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
		}

		int[] bins = density.get(linkId);

		if (time < 86400) {
			int startBinIndex = (int) Math.round(Math.floor(GeneralLib
					.projectTimeWithin24Hours(linkEnterTime.get(linkId,
							personId))
					/ binSizeInSeconds));
			int endBinIndex = (int) Math.round(Math.floor(GeneralLib
					.projectTimeWithin24Hours(time / binSizeInSeconds)));

			for (int i = startBinIndex; i <= endBinIndex; i++) {
				bins[i]++;
			}
		}

	}

}

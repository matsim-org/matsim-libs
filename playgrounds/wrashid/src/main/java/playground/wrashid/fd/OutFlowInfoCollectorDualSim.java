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

public class OutFlowInfoCollectorDualSim extends AbstractDualSimHandler {

	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id, int[]> linkOutFlow; // define
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks; // define

	private boolean isJDEQSim;

	public OutFlowInfoCollectorDualSim(
			Map<Id<Link>, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds, boolean isJDEQSim) { 

		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
		this.isJDEQSim = isJDEQSim;
	}

	@Override
	public void reset(int iteration) {
		linkOutFlow = new HashMap<Id, int[]>(); 
	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return linkOutFlow;
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
	public void processLeaveLink(Id linkId, Id personId, double enterTime, double leaveTime) {
		if (!linkOutFlow.containsKey(linkId)) {
			linkOutFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
		}

		int[] bins = linkOutFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(leaveTime / binSizeInSeconds));

		if (leaveTime < 86400) {
			bins[binIndex]++;
		}

	}

}

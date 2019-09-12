/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
* @author ikaddoura, mmayobre
*/

class AccidentLinkInfo {
	
	private final Id<Link> linkId;
	
	private AccidentsConfigGroup.AccidentsComputationMethod computationMethod;

	private final Map<Integer, TimeBinInfo> timeSpecificInfo = new HashMap<>();
	
	public AccidentLinkInfo(Id<Link> linkId) {
		this.linkId = linkId;
	}
	
	public Id<Link> getLinkId() {
		return linkId;
	}

	public Map<Integer, TimeBinInfo> getTimeSpecificInfo() {
		return timeSpecificInfo;
	}

	public AccidentsConfigGroup.AccidentsComputationMethod getComputationMethod() {
		return computationMethod;
	}

	public void setComputationMethod( AccidentsConfigGroup.AccidentsComputationMethod computationMethod ) {
		this.computationMethod = computationMethod;
	}

}


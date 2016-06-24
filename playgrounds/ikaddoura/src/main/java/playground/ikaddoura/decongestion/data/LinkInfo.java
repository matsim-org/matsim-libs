/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.decongestion.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * 
 * @author ikaddoura
 */

public class LinkInfo {
	
	private final Id<Link> linkId;
	
	private final Map<Integer, Double> time2toll = new HashMap<>();
	private Map<Integer, Double> time2avgDelay = new HashMap<>();
	
	public LinkInfo(Id<Link> linkId) {
		this.linkId = linkId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Map<Integer, Double> getTime2toll() {
		return time2toll;
	}

	public Map<Integer, Double> getTime2avgDelay() {
		return time2avgDelay;
	}

	public void setTime2avgDelay(Map<Integer, Double> time2avgDelay) {
		this.time2avgDelay = time2avgDelay;
	}

}


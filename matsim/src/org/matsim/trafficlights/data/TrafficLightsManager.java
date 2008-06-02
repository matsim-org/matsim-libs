/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.trafficlights.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;


/**
 * This class offers some convenient methods to acces the traffic light database of matsim.
 * @author dgrether
 *
 */
public class TrafficLightsManager {

	private Set<SignalGroupDefinition> signalDefinitions;

	private Map<SignalLane, SignalGroupDefinition> fromLaneGroupMap;

	private NetworkLayer network;

	public TrafficLightsManager(Set<SignalGroupDefinition> defs, NetworkLayer net) {
		this.signalDefinitions = defs;
		this.network = net;
		this.fromLaneGroupMap = new HashMap<SignalLane, SignalGroupDefinition>(defs.size(), 0.95f);
		for (SignalGroupDefinition def : this.signalDefinitions) {
			for (SignalLane l : def.getFromLanes()) {
				this.fromLaneGroupMap.put(l, def);
			}
		}

	}

	/**
	 *
	 * @param linkId
	 * @return the FromLanes of the given link
	 */
	public List<SignalLane> getFromLanes(Id linkId) {
		List<SignalLane> lanes = new ArrayList<SignalLane>();
		for (SignalGroupDefinition def : this.signalDefinitions) {
			for (SignalLane l : def.getFromLanes()) {
				if (l.getLinkId().equals(linkId)) {
					lanes.add(l);
				}
			}
		}

		return lanes;
	}


	/**
	 *
	 * @param lane a fromLane of a signal Group
	 * @return A List of Link instances, which can be accessed from the fromLane given as parameter.
	 */
	public List<Link> getToLinks(SignalLane lane) {
		SignalGroupDefinition group = this.fromLaneGroupMap.get(lane);
		List<Link> ret = new ArrayList<Link>();
		for (SignalLane l :group.getToLanes()) {
			ret.add(this.network.getLink(l.getLinkId()));
		}
		return ret;
	}



}

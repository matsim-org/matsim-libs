/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.data.conflicts;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author tthunig
 */
public class ConflictingDirectionsImpl implements ConflictingDirections {

	private Id<SignalSystem> signalSystemId;
	private Id<Node> nodeId;
	private List<Direction> directionsOfThisIntersection = new ArrayList<>();
	
	ConflictingDirectionsImpl(Id<SignalSystem> signalSystemId, Id<Node> nodeId) {
		this.signalSystemId = signalSystemId;
		this.nodeId = nodeId;
	}
	
	@Override
	public Id<SignalSystem> getSignalSystemId() {
		return signalSystemId;
	}

	@Override
	public Id<Node> getNodeId() {
		return nodeId;
	}

	@Override
	public void addDirection(Direction direction) {
		this.directionsOfThisIntersection.add(direction);
	}

	@Override
	public Direction getDirection(Id<Link> fromLink, Id<Link> toLink) {
		for (Direction d : directionsOfThisIntersection) {
			if (d.getFromLink().equals(fromLink) && d.getToLink().equals(toLink)) {
				return d;
			}
		}
		throw new RuntimeException("SignalSystem " + signalSystemId + " has no direction with from-Link " + fromLink + " and to-link " + toLink);
	}

	@Override
	public List<Direction> getDirections() {
		return directionsOfThisIntersection;
	}

}

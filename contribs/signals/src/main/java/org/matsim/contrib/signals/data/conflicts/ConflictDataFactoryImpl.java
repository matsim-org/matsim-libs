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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author tthunig
 */
public class ConflictDataFactoryImpl implements ConflictDataFactory {

	@Override
	public IntersectionDirections createConflictingDirectionsContainerForIntersection(Id<SignalSystem> signalSystemId,
			Id<Node> nodeId) {
		return new IntersectionDirectionsImpl(signalSystemId, nodeId);
	}

	@Override
	public Direction createDirection(Id<SignalSystem> signalSystemId, Id<Node> nodeId, Id<Link> fromLinkId, Id<Link> toLinkId, Id<Direction> directionId) {
		return new DirectionImpl(signalSystemId, nodeId, fromLinkId, toLinkId, directionId);
	}

}

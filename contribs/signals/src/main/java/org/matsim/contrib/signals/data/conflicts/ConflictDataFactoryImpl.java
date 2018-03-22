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
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author tthunig
 */
public class ConflictDataFactoryImpl implements ConflictDataFactory {

	@Override
	public ConflictingDirections createConflictingDirectionsContainerForSignalSystem(Id<SignalSystem> signalSystemId) {
		return new ConflictingDirectionsImpl(signalSystemId);
	}

	@Override
	public Direction createDirection(Id<SignalSystem> signalSystemId, Id<Link> fromLinkId, Id<Link> toLinkId) {
		return new DirectionImpl(signalSystemId, fromLinkId, toLinkId);
	}

}

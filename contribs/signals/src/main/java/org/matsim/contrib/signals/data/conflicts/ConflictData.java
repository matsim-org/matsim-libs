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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * @author tthunig
 */
public interface ConflictData extends MatsimToplevelContainer {

	public void addConflictingDirectionsForIntersection(Id<SignalSystem> signalSystemId, Id<Node> nodeId, ConflictingDirections conflictingDirections);
	
	public Map<Id<SignalSystem>, ConflictingDirections> getConflictsPerSignalSystem();
	
	public Map<Id<Node>, ConflictingDirections> getConflictsPerNode();
	
	@Override
	public ConflictDataFactory getFactory() ;
	
	public void setFactory(ConflictDataFactory factory);
	
}

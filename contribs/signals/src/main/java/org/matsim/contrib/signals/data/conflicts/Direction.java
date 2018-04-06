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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author tthunig
 */
public interface Direction extends Identifiable<Direction> {
	
	public Id<SignalSystem> getSignalSystemId();
	public Id<Node> getNodeId();
	public Id<Link> getFromLink();
	public Id<Link> getToLink();
	
	public List<Id<Direction>> getConflictingDirections();
	public void addConflictingDirection(Id<Direction> directionId);
	
	public List<Id<Direction>> getDirectionsWithRightOfWay();
	public void addDirectionWithRightOfWay(Id<Direction> directionId);
	
	public List<Id<Direction>> getDirectionsWhichMustYield();
	public void addDirectionWhichMustYield(Id<Direction> directionId);
	
	public List<Id<Direction>> getNonConflictingDirections();
	public void addNonConflictingDirection(Id<Direction> directionId);

}

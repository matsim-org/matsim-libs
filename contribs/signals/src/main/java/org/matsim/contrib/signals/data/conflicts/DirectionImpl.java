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
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author tthunig
 */
public class DirectionImpl implements Direction {

	private Id<SignalSystem> signalSystemId;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private List<Direction> conflictingDirections = new ArrayList<>();
	private List<Direction> directionsWithRightOfWay = new ArrayList<>();
	private List<Direction> directionsWhichMustYield = new ArrayList<>();
	private List<Direction> nonConflictingDirections = new ArrayList<>();
	
	DirectionImpl(Id<SignalSystem> signalSystemId, Id<Link> fromLinkId, Id<Link> toLinkId) {
		this.signalSystemId = signalSystemId;
		this.fromLinkId = fromLinkId;
		this.toLinkId = toLinkId;
	}
	
	@Override
	public Id<SignalSystem> getSignalSystemId() {
		return signalSystemId;
	}

	@Override
	public Id<Link> getFromLink() {
		return fromLinkId;
	}

	@Override
	public Id<Link> getToLink() {
		return toLinkId;
	}

	@Override
	public List<Direction> getConflictingDirections() {
		return conflictingDirections;
	}

	@Override
	public void addConflictingDirection(Direction direction) {
		conflictingDirections.add(direction);
	}

	@Override
	public List<Direction> getDirectionsWithRightOfWay() {
		return directionsWithRightOfWay;
	}

	@Override
	public void addDirectionWithRightOfWay(Direction direction) {
		directionsWithRightOfWay.add(direction);
	}

	@Override
	public List<Direction> getDirectionsWhichMustYield() {
		return directionsWhichMustYield;
	}

	@Override
	public void addDirectionWhichMustYield(Direction direction) {
		directionsWhichMustYield.add(direction);
	}

	@Override
	public List<Direction> getNonConflictingDirections() {
		return nonConflictingDirections;
	}

	@Override
	public void addNonConflictingDirection(Direction direction) {
		nonConflictingDirections.add(direction);
	}

}

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
package org.matsim.contrib.signals.data.conflicts.io;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.ConflictingDirections;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author tthunig
 */
public class ConflictingDirectionsReader extends MatsimXmlParser {

	final static String INTERSECTION = "intersection";
	final static String SIGNAL_SYSTEM_ID = "signalSystemId";
	final static String NODE_ID = "nodeId";
	final static String DIRECTION = "direction";
	final static String ID = "id";
	final static String FROM_LINK_ID = "fromLinkId";
	final static String TO_LINK_ID = "toLinkId";
	final static String CONFLICTING_DIRECTIONS = "conflicting directions";
	final static String DIRECTIONS_WITH_RIGHT_OF_WAY = "directions with right of way";
	final static String DIRECTIONS_WHICH_MUST_YIELD = "directions which must yield";
	final static String NON_CONFLICTING_DIRECTIONS = "non-conflicting directions";
	
	private ConflictData conflictData;
	
	private ConflictingDirections currentIntersection;
	private Direction currentDirection;
	
	public ConflictingDirectionsReader(ConflictData conflictData) {
		this.conflictData = conflictData;
	}
	
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		switch (name) {
		case INTERSECTION:
			startIntersection(Id.create(atts.getValue(SIGNAL_SYSTEM_ID), SignalSystem.class), Id.createNodeId(atts.getValue(NODE_ID)));
			break;
		case DIRECTION:
			startDirection(Id.create(atts.getValue(ID), Direction.class), Id.createLinkId(atts.getValue(FROM_LINK_ID)), Id.createLinkId(atts.getValue(TO_LINK_ID)));
			break;
		default:
			break;
		}
	}

	private void startDirection(Id<Direction> directionId, Id<Link> fromLinkId, Id<Link> toLinkId) {
		currentDirection = conflictData.getFactory().createDirection(currentIntersection.getSignalSystemId(), currentIntersection.getNodeId(), fromLinkId, toLinkId, directionId);
		currentIntersection.addDirection(currentDirection);
	}


	private void startIntersection(Id<SignalSystem> signalSystemId, Id<Node> nodeId) {
		currentIntersection = conflictData.getFactory().createConflictingDirectionsContainerForIntersection(signalSystemId, nodeId);
		conflictData.addConflictingDirectionsForIntersection(signalSystemId, nodeId, currentIntersection);
	}


	@Override
	public void endTag(String name, String content, Stack<String> context) {
		switch (name) {
		case CONFLICTING_DIRECTIONS:
		case DIRECTIONS_WITH_RIGHT_OF_WAY:
		case DIRECTIONS_WHICH_MUST_YIELD:
		case NON_CONFLICTING_DIRECTIONS:
			addConflictsForCurrentDirection(name, content);
			break;
		default:
			break;
		}
	}


	private void addConflictsForCurrentDirection(String name, String directions) {
		List<String> directionsList = Arrays.asList(directions.trim().split(" "));
		for (String direction : directionsList) {
			Id<Direction> directionId = Id.create(direction, Direction.class);
			switch (name) {
			case CONFLICTING_DIRECTIONS:
				currentDirection.addConflictingDirection(directionId);
				break;
			case DIRECTIONS_WITH_RIGHT_OF_WAY:
				currentDirection.addDirectionWithRightOfWay(directionId);
				break;
			case DIRECTIONS_WHICH_MUST_YIELD:
				currentDirection.addDirectionWhichMustYield(directionId);
				break;
			case NON_CONFLICTING_DIRECTIONS:
				currentDirection.addNonConflictingDirection(directionId);
				break;
			}
		}
	}

}

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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.model.SignalSystem;

/**
 * @author tthunig
 */
public class ConflictingDirectionsWriterHandlerImpl implements ConflictingDirectionsWriterHandler {

	@Override
	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
//		out.write("<!DOCTYPE conflictData SYSTEM \"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "conflictData.dtd\">\n\n");
	}

	@Override
	public void startConflictData(BufferedWriter out) throws IOException {
		out.write("<conflictData>\n\n");
	}

	@Override
	public void endConflictData(BufferedWriter out) throws IOException {
		out.write("</conflictData>\n");
	}

	@Override
	public void writeIntersections(ConflictData conflictData, BufferedWriter out) throws IOException {
		for (IntersectionDirections conflictingDirections : conflictData.getConflictsPerSignalSystem().values()) {
			this.startIntersection(conflictingDirections.getNodeId(), conflictingDirections.getSignalSystemId(), out);
			for (Direction direction : conflictingDirections.getDirections().values()) {
				this.writeDirection(direction, out);
			}
			this.endIntersection(out);
		}
	}

	private void startIntersection(Id<Node> nodeId, Id<SignalSystem> systemId, BufferedWriter out) throws IOException {
		out.write("\t<intersection");
		out.write(" nodeId=\"" + nodeId + "\"");
		out.write(" signalSystemId=\"" + systemId + "\"");
		out.write(" >\n");
	}

	private void endIntersection(BufferedWriter out) throws IOException {
		out.write("\t</intersection>\n");
		this.writeSeparator(out);
		out.flush();
	}

	@Override
	public void writeDirection(Direction direction, BufferedWriter out) throws IOException {
		this.startDirection(direction, out);
		
		this.startTag(ConflictingDirectionsReader.CONFLICTING_DIRECTIONS, out);
		this.writeListOfDirections(direction.getConflictingDirections(), out);
		this.endTag(ConflictingDirectionsReader.CONFLICTING_DIRECTIONS, out);
		
		this.startTag(ConflictingDirectionsReader.DIRECTIONS_WITH_RIGHT_OF_WAY, out);
		this.writeListOfDirections(direction.getDirectionsWithRightOfWay(), out);
		this.endTag(ConflictingDirectionsReader.DIRECTIONS_WITH_RIGHT_OF_WAY, out);
		
		this.startTag(ConflictingDirectionsReader.DIRECTIONS_WHICH_MUST_YIELD, out);
		this.writeListOfDirections(direction.getDirectionsWhichMustYield(), out);
		this.endTag(ConflictingDirectionsReader.DIRECTIONS_WHICH_MUST_YIELD, out);
		
		this.startTag(ConflictingDirectionsReader.NON_CONFLICTING_DIRECTIONS, out);
		this.writeListOfDirections(direction.getNonConflictingDirections(), out);
		this.endTag(ConflictingDirectionsReader.NON_CONFLICTING_DIRECTIONS, out);
		
		this.endDirection(out);
	}

	private void startDirection(Direction direction, BufferedWriter out) throws IOException {
		out.write("\t\t<direction");
		out.write(" id=\"" + direction.getId() + "\"");
		out.write(" fromLinkId=\"" + direction.getFromLink() + "\"");
		out.write(" toLinkId=\"" + direction.getToLink() + "\"");
		out.write(" >\n");
	}

	private void endDirection(BufferedWriter out) throws IOException {
		out.write("\t\t</direction>\n\n");
	}
	
	private void startTag(String tagName, BufferedWriter out) throws IOException {
		out.write("\t\t\t<"+tagName+">");
	}
	
	private void endTag(String tagName, BufferedWriter out) throws IOException {
		out.write(" </"+tagName+">\n");
	}
	
	private void writeListOfDirections(List<Id<Direction>> directionList, BufferedWriter out) throws IOException {
		for (Id<Direction> directionId : directionList) {
			out.write(" " + directionId);
		}
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		out.write("<!-- ====================================================================== -->\n\n");
	}

}

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

import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;

/**
 * @author tthunig
 */
public interface ConflictingDirectionsWriterHandler {
	
	public void writeHeaderAndStartElement(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <conflictData ... > ... </conflictData>
	//////////////////////////////////////////////////////////////////////
	public void startConflictData(final BufferedWriter out) throws IOException;	
	
	public void endConflictData(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <signalSystem ... > ... </signalSystem>
	//////////////////////////////////////////////////////////////////////

	public void writeIntersections(final ConflictData conflictData, final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <direction ... > ... </direction>
	//////////////////////////////////////////////////////////////////////

	public void writeDirection(final Direction direction, final BufferedWriter out) throws IOException;
	
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}

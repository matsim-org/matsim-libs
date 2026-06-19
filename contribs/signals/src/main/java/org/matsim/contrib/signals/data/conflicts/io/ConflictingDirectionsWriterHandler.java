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

import java.io.IOException;
import java.io.Writer;

import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;

/**
 * @author tthunig
 */
public interface ConflictingDirectionsWriterHandler {
	
	public void writeHeaderAndStartElement(final Writer out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <conflictData ... > ... </conflictData>
	//////////////////////////////////////////////////////////////////////
	public void startConflictData(final Writer out) throws IOException;	
	
	public void endConflictData(final Writer out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <signalSystem ... > ... </signalSystem>
	//////////////////////////////////////////////////////////////////////

	public void writeIntersections(final ConflictData conflictData, final Writer out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <direction ... > ... </direction>
	//////////////////////////////////////////////////////////////////////

	public void writeDirection(final Direction direction, final Writer out) throws IOException;
	
	
	public void writeSeparator(final Writer out) throws IOException;
	
}

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

import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;

/**
 * @author tthunig
 */
public interface ConflictingDirectionsWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <conflictData ... > ... </conflictData>
	//////////////////////////////////////////////////////////////////////
	public void startConflictData(final ConflictData conflictData, final BufferedWriter out) throws IOException;	
	
	public void endConflictData(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <signalSystem ... > ... </signalSystem>
	//////////////////////////////////////////////////////////////////////

	public void startSignalSystem(final Node node, final BufferedWriter out) throws IOException;

	public void endSignalSystem(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <direction ... > ... </direction>
	//////////////////////////////////////////////////////////////////////

	public void startDirection(final Direction direction, final BufferedWriter out) throws IOException;

	public void endDirection(final BufferedWriter out) throws IOException;
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}

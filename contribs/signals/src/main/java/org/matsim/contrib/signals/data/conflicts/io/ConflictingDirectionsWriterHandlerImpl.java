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
public class ConflictingDirectionsWriterHandlerImpl implements ConflictingDirectionsWriterHandler {

	@Override
	public void startConflictData(ConflictData conflictData, BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endConflictData(BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startSignalSystem(Node node, BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endSignalSystem(BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startDirection(Direction direction, BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endDirection(BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		// TODO Auto-generated method stub
		
	}

}

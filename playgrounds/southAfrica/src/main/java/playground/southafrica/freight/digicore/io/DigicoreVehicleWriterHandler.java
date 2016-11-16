/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.southafrica.freight.digicore.io;

import java.io.BufferedWriter;
import java.io.IOException;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicorePosition;
import playground.southafrica.freight.digicore.containers.DigicoreTrace;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public interface DigicoreVehicleWriterHandler {

	/* <vehicle> ... </vehicle> */
	public void startVehicle(final DigicoreVehicle vehicle, final BufferedWriter out) throws IOException;
	public void endVehicle(final BufferedWriter out) throws IOException;
	
	/* <chain> ... </chain> */
	public void startChain(final BufferedWriter out) throws IOException;
	public void endChain(final BufferedWriter out) throws IOException;
	
	/* <activity> ... </activity> */
	public void startActivity(final DigicoreActivity activity, final BufferedWriter out) throws IOException;
	public void endActivity(final BufferedWriter out) throws IOException;
	
	/* <trace> ... </trace> */
	public void startTrace(final DigicoreTrace trace, final BufferedWriter out) throws IOException;
	public void endTrace(final BufferedWriter out) throws IOException;
	
	/* <position ... /> */
	public void startPosition(final DigicorePosition pos, final BufferedWriter out) throws IOException;
	public void endPosition(final BufferedWriter out) throws IOException;
	
	/*TODO <route ... > */
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}


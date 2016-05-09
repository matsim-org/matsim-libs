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
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

public interface DigicoreVehiclesWriterHandler {
	
	/* <vehicles> ... </vehicles> */
	public void startVehicles(final DigicoreVehicles vehicles, final BufferedWriter out) throws IOException;
	public void endVehicles(final BufferedWriter out) throws IOException;

	/* <vehicle> ... </vehicle> */
	public void startVehicle(final DigicoreVehicle vehicle, final BufferedWriter out) throws IOException;
	public void endVehicle(final BufferedWriter out) throws IOException;
	
	/* <chain> ... </chain> */
	public void startChain(final BufferedWriter out) throws IOException;
	public void endChain(final BufferedWriter out) throws IOException;
	
	/* <activity> ... </activity> */
	public void startActivity(final DigicoreActivity activity, final BufferedWriter out) throws IOException;
	public void endActivity(final BufferedWriter out) throws IOException;
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}


/* *********************************************************************** *
 * project: org.matsim.*
 * TransimsSnapshotWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.vis.snapshots.writers;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/**
 * Writes the current position of vehicles into a file that can be read by
 * transims.
 *
 * @author mrieser
 */
public class TransimsSnapshotWriter implements SnapshotWriter {
	private BufferedWriter out = null;
	private double currentTime = -1;

	public TransimsSnapshotWriter(String filename) {
		try {
			this.out = IOUtils.getBufferedWriter(filename, true);
			String header = "VEHICLE"
            + "\tTIME"
            + "\tLINK"
            + "\tNODE"
            + "\tLANE"
            + "\tDISTANCE"
            + "\tVELOCITY"
            + "\tVEHTYPE"
            + "\tACCELER"
            + "\tDRIVER"
            + "\tPASSENGERS"
            + "\tEASTING"
            + "\tNORTHING"
            + "\tELEVATION"
            + "\tAZIMUTH"
            + "\tUSER\n";
			this.out.write(header);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addAgent(PositionInfo position) {
		
		//drop all parking vehicles 
		if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
		
//		String visualizerData = 	position.getVisualizerData();
//		if (visualizerData == null){
//			visualizerData = "0";
//		}
		
		String buffer = position.getId().toString()
					    			+ "\t" + (int)this.currentTime
		                + "\t0\t0\t1\t0\t" + position.getColorValueBetweenZeroAndOne() // link(0), from node(0), lane(1), dist(0), speed
		                + "\t1\t0\t" + position.getId().toString()   // vehtype(1), acceleration(0), driver-id
		                + "\t0\t" + position.getEasting()   // # of passengers(0), easting
		                + "\t" + position.getNorthing() 
		                + "\t" + position.getElevation()
		                + "\t" + position.getAzimuth()
		                + "\t"+ "0" + "\n"; // user(0)
		try {
			out.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void beginSnapshot(double time) {
		this.currentTime = time;
	}

	public void endSnapshot() {
		this.currentTime = -1;
	}

	public void finish() {
		if (this.out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * TableSnapshotWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.snapshots.writers;

import java.io.IOException;
import java.io.Writer;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;


public class TableSnapshotWriter {

	private final Writer writer;
	
	public TableSnapshotWriter(String filename) {
		this.writer = IOUtils.getBufferedWriter(filename,true);
		try {
			this.writer.append("time\tagent_id\teasting\tnorthing\tazimuth\tspeed\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addVehicle(double time, AgentSnapshotInfo pos) throws IOException {
		StringBuffer buff = new StringBuffer();
		buff.append(time);
		buff.append("\t");
		buff.append(pos.getId());
		buff.append("\t");
		buff.append(pos.getEasting());
		buff.append("\t");
		buff.append(pos.getNorthing());
		buff.append("\t");
		buff.append(pos.getAzimuth());
		buff.append("\t");
		buff.append(pos.getColorValueBetweenZeroAndOne());
		buff.append("\n");
		this.writer.append(buff.toString());
		
	}

	public void finish() {
		try {
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	

	
	
	
}

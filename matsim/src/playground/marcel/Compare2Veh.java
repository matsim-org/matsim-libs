/* *********************************************************************** *
 * project: org.matsim.*
 * Compare2Veh.java
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

package playground.marcel;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.utils.io.IOUtils;

public class Compare2Veh {

	final private String compareFile;
	final private String vehFile;

	public Compare2Veh(final String compareFile, final String vehFile) {
		this.compareFile = compareFile;
		this.vehFile = vehFile;
	}

	public void run() {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(this.vehFile);
			BufferedReader reader = IOUtils.getBufferedReader(this.compareFile);
			writer.write("VEHICLE\tTIME\tLINK\tNODE\tLANE\tDISTANCE\tVELOCITY\tVEHTYPE"
        + "\tACCELER\tDRIVER\tPASSENGERS\tEASTING\tNORTHING\tELEVATION\tAZIMUTH\tUSER\n");
			String line = reader.readLine(); // header
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				double score1 = Double.parseDouble(parts[3]);
				double score2 = Double.parseDouble(parts[4]);
				String speed = (score2 - score1 > 2 ? "30" : (score2 - score1 < -2 ? "0": "1"));
				String buffer = parts[0]
  			+ "\t0"
        + "\t0\t0\t1\t0\t" + speed // link(0), from node(0), lane(1), dist(0), speed
        + "\t1\t0\t" + parts[0]   // vehtype(1), acceleration(0), driver-id
        + "\t0\t" + parts[1]   // # of passengers(0), easting
        + "\t" + parts[2]
        + "\t0"
        + "\t0"
        + "\t0\n"; // user(0)
				writer.write(buffer);
			}
			reader.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		Compare2Veh app = new Compare2Veh(args[0], args[1]);
		app.run();
	}

}

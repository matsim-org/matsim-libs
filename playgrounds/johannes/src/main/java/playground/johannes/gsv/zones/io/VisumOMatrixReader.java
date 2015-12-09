/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.zones.io;

import playground.johannes.gsv.zones.KeyMatrix;

import java.io.*;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class VisumOMatrixReader {

	public static KeyMatrix read(KeyMatrix m, String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));

		String line = reader.readLine();
		if(!line.equals("$O;D3")) throw new RuntimeException("Unknown matrix format.");

		boolean comment = false;
		int sectionCount = 0;

		while((line = reader.readLine()) != null) {
			if(line.startsWith("*")) {
				comment = true;
			} else {
				if(comment) sectionCount++;
				comment = false;

				if(sectionCount == 3) {
					line = line.trim();
					String[] tokens = line.split("\\s+");
					String i = tokens[0];
					String j = tokens[1];
					Double val = new Double(tokens[2]);

					m.set(i, j, val);
				} else if(sectionCount > 3) {
					break;
				}
			}


		}

		reader.close();

		return m;
	}

	public static void write(KeyMatrix m, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("$O;D3");
		writer.newLine();
		writer.write("* Von  Bis");
		writer.newLine();
		writer.write("0.00 24.00");
		writer.newLine();
		writer.write("* Faktor");
		writer.newLine();
		writer.write("1.00");
		writer.newLine();
		writer.write("*");
		writer.newLine();

		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double vol = m.get(i, j);
				if(vol != null) {
					writer.write(i);
					writer.write(" ");
					writer.write(j);
					writer.write(" ");
					writer.write(String.valueOf(vol));
					writer.newLine();
				}
			}
		}

		writer.close();
	}
}

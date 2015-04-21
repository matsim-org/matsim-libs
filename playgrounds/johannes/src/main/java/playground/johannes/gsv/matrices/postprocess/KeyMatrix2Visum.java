/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.postprocess;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;

/**
 * @author johannes
 * 
 */
public class KeyMatrix2Visum {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String in = args[0];
		String out = args[1];

		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(in);

		KeyMatrix inMatrix = reader.getMatrix();

		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
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

		Set<String> keys = inMatrix.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = inMatrix.get(i, j);
				if (val != null) {
					writer.write(i);
					writer.write(" ");
					writer.write(j);
					writer.write(" ");
					writer.write(String.valueOf(val));
					writer.newLine();
				}
			}
		}
		writer.write("* Netzobjektnamen");
		writer.newLine();
		writer.write("$NAMES");
		writer.newLine();
		for (String key : keys) {
			writer.write(key);
			writer.write(" \"\"");
			writer.newLine();
		}
		writer.close();
	}
}

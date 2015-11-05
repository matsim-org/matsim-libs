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

package playground.johannes.gsv.matrices.io;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class KeyMatrix2Txt {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(args[0]);
		KeyMatrix m = reader.getMatrix();

//		playground.johannes.gsv.zones.MatrixOperations.applyFactor(m, 11);
//		playground.johannes.gsv.zones.MatrixOperations.applyDiagonalFactor(m, 1.25);

		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
		writer.write("from\tto\tvolume");
		writer.newLine();
		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double val = m.get(i, j);
				if(val != null) {
					writer.write(i);
					writer.write("\t");
					writer.write(j);
					writer.write("\t");
					writer.write(String.valueOf(val));
					writer.newLine();
				}
			}
		}
		writer.close();
	}

}

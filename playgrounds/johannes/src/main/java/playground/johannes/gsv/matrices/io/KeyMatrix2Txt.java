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

import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

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
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.readFile(args[0]);
		NumericMatrix m = reader.getMatrix();

		int odId = 0;
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
		writer.write("from\tto\tvalue\todId");
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
					writer.write("\t");
					writer.write(String.valueOf(odId));
					writer.newLine();
					odId++;
				}
			}
		}
		writer.close();
	}

}

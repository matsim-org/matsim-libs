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

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;
import java.util.List;

/**
 * @author johannes
 *
 */
public class Visum2KeyMatrix {

	public static NumericMatrix convert(Matrix visumMatrix) {
		NumericMatrix keyMatrix = new NumericMatrix();
		
		for(List<Entry> entries : visumMatrix.getFromLocations().values()) {
			for(Entry entry : entries) {
				keyMatrix.add(entry.getFromLocation(), entry.getToLocation(), entry.getValue());
			}
		}

		return keyMatrix;
	}
	
	public static void main(String[] args) throws IOException {
		Matrix visumMatrix = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(visumMatrix);
		reader.readFile("/Users/johannes/gsv/miv-matrix/raw/modena/miv.edu.txt");
		
		NumericMatrix keyMatrix = convert(visumMatrix);
		
//		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		NumericMatrixIO.write(keyMatrix, "/Users/johannes/gsv/miv-matrix/raw/modena/miv.edu.2.txt");
//		writer.write(keyMatrix, "/Users/johannes/Desktop/rail.2013.txt");
	}

}

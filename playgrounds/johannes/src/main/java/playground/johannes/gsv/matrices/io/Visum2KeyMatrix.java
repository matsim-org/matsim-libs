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

import java.util.List;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;

/**
 * @author johannes
 *
 */
public class Visum2KeyMatrix {

	public static KeyMatrix convert(Matrix visumMatrix) {
		KeyMatrix keyMatrix = new KeyMatrix();
		
		for(List<Entry> entries : visumMatrix.getFromLocations().values()) {
			for(Entry entry : entries) {
				keyMatrix.add(entry.getFromLocation(), entry.getToLocation(), entry.getValue());
			}
		}

		return keyMatrix;
	}
	
	public static void main(String[] args) {
		Matrix visumMatrix = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(visumMatrix);
		reader.readFile("/home/johannes/gsv/prognose-update/iv-2030.txt");
		
		KeyMatrix keyMatrix = convert(visumMatrix);
		
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(keyMatrix, "/home/johannes/gsv/prognose-update/iv-2030.xml");
	}

}

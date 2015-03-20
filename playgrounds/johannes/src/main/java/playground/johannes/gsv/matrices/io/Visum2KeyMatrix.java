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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Matrix visumMatrix = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(visumMatrix);
		reader.readFile("/home/johannes/gsv/matrices/analysis/marketShares/Split_IV_Modell.mtx");
		
		KeyMatrix keyMatrix = new KeyMatrix();
		
		for(List<Entry> entries : visumMatrix.getFromLocations().values()) {
			for(Entry entry : entries) {
				keyMatrix.add(entry.getFromLocation(), entry.getToLocation(), entry.getValue());
			}
		}
		
//		for(List<Entry> entries : visumMatrix.getToLocations().values()) {
//			for(Entry entry : entries) {
//				keyMatrix.add(entry.getFromLocation(), entry.getToLocation(), entry.getValue());
//			}
//		}
		
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(keyMatrix, "/home/johannes/gsv/matrices/analysis/marketShares/car.share.xml");
	}

}

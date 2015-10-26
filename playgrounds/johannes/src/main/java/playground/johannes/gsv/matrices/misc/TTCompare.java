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

package playground.johannes.gsv.matrices.misc;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.VisumOMatrixReader;

import java.io.IOException;

/**
 * @author johannes
 *
 */
public class TTCompare {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		String file2009 = args[0];
//		String file2030 = args[1];
//		String zoneFile = args[2];
//		String primKey = args[3];
		
		KeyMatrix tt2009 = new KeyMatrix();
		VisumOMatrixReader.read(tt2009, "/home/johannes/gsv/prognose-update/infra2030lkw2030.txt");
		KeyMatrix tt2030 = new KeyMatrix();
		VisumOMatrixReader.read(tt2030, "/home/johannes/gsv/prognose-update/Kreis_025.txt");
//		System.out.println("Loading matrix 2009...");
//		KeyMatrix tt2009 = VisumOMatrixReader.read(file2009);
//		System.out.println("Loading matrix 2030...");
//		KeyMatrix tt2030 = VisumOMatrixReader.read(file2030);
		KeyMatrix dmd2030 = new KeyMatrix();
		VisumOMatrixReader.read(dmd2030, "/home/johannes/gsv/prognose-update/iv-2030.txt");
		
		
		tt2009 = ExtractDE.extract(tt2009, "/home/johannes/gsv/gis/nuts/ger/geojson/de.nuts3.gk3.geojson", "gsvId");
		tt2030 = ExtractDE.extract(tt2030, "/home/johannes/gsv/gis/nuts/ger/geojson/de.nuts3.gk3.geojson", "gsvId");
//		tt2009 = ExtractDE.extract(tt2009, zoneFile, primKey);
//		tt2030 = ExtractDE.extract(tt2030, zoneFile, primKey);
		
//		KeyMatrix ttErr = MatrixOperations.errorMatrix(tt2009, tt2030);
		KeyMatrix ttErr = MatrixOperations.diffMatrix(tt2030, tt2009);
		
		double sum = MatrixOperations.sum(ttErr);
		int N = ttErr.keys().size();
		System.out.println("Average rel diff = " + sum/(double)(N*N));
		
		System.out.println("Weighted avr = " + MatrixOperations.weightedCellAverage(ttErr, dmd2030));
		
		MatrixOperations.removeDiagonal(ttErr);
		
		sum = MatrixOperations.sum(ttErr);
		N = ttErr.keys().size();
		System.out.println("Average rel diff = " + sum/(double)(N*N));
		
		System.out.println("Weighted avr = " + MatrixOperations.weightedCellAverage(ttErr, dmd2030));
	}

}

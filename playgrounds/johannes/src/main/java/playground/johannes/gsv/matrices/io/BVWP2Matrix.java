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

package playground.johannes.gsv.matrices.io;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author johannes
 * 
 */
public class BVWP2Matrix {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Matrix mA = new Matrix("A", null);
		VisumMatrixReader reader = new VisumMatrixReader(mA);
		reader.readFile("/home/johannes/gsv/matrices/IV_EKL_A.fma");

		Matrix mG = new Matrix("G", null);
		reader = new VisumMatrixReader(mG);
		reader.readFile("/home/johannes/gsv/matrices/IV_EKL_G.fma");

		Matrix mP = new Matrix("P", null);
		reader = new VisumMatrixReader(mP);
		reader.readFile("/home/johannes/gsv/matrices/IV_EKL_P.fma");

		Matrix mU = new Matrix("U", null);
		reader = new VisumMatrixReader(mU);
		reader.readFile("/home/johannes/gsv/matrices/IV_EKL_U.fma");
		
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/de.nuts3.json")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		
		NumericMatrix m = new NumericMatrix();
		
		for(Zone i : zones.getZones()) {
			for(Zone j : zones.getZones()) {
				double sum = 0;
				
				String id_i = i.getAttribute("gsvId");
				String id_j = j.getAttribute("gsvId");
				
				Entry eA = mA.getEntry(id_i, id_j);
				if(eA != null) {
					sum += eA.getValue();
				}
				
				Entry eG = mG.getEntry(id_i, id_j);
				if(eG != null) {
					sum += eG.getValue();
				}
				
				Entry eP = mP.getEntry(id_i, id_j);
				if(eP != null) {
					sum += eP.getValue();
				}
				
				Entry eU = mU.getEntry(id_i, id_j);
				if(eU != null) {
					sum += eU.getValue();
				}
				
				m.set(id_i, id_j, sum);
			}
			
		}
		
		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, "/home/johannes/gsv/matrices/bmbv2010.xml");
	}

}

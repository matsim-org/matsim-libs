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

import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author johannes
 *
 */
public class IVV2Matrix {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/de.nuts3.json")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		
		NumericMatrix m = new NumericMatrix();
		
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/matrices/raw/ivv/Matrix2005_PS_Mobility.txt"));
		
		String line = reader.readLine();
		
		int nozone = 0;
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			String id_i = tokens[0];
			String id_j = tokens[1];
			String val = tokens[4];
			
			Zone z_i = zones.get(id_i);
			Zone z_j = zones.get(id_j);
			
			if(z_i != null && z_j != null) {
				Double value = m.get(id_i, id_j);
				if(value == null) value = 0.0;
				
				double v = Double.parseDouble(val);
				
				m.set(id_i, id_j, v+value);
			} else {
				nozone++;
			}
		}
		if(nozone > 0) {
			System.out.println(String.format("%s zones not found.", nozone));
		}
		
		reader.close();
		
		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, "/home/johannes/gsv/matrices/raw/ivv/ivv.xml");
	}

}

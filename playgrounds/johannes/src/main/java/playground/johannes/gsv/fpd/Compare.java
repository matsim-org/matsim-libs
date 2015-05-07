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

package playground.johannes.gsv.fpd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import playground.johannes.gsv.sim.cadyts.ODUtils;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;

/**
 * @author johannes
 *
 */
public class Compare {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		
		reader.parse("/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/28-04-2015/iais.2h.xml");
		KeyMatrix iais = reader.getMatrix();

		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.de.modena.xml");
		KeyMatrix tomtom = reader.getMatrix();
		
		reader.parse("/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/ref/modena.all.xml");
		KeyMatrix model = reader.getMatrix();
		
		reader.parse("/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/ref/miv.sym.874.xml");
		KeyMatrix sim = reader.getMatrix();
		
		ZoneCollection zones = ZoneCollection.readFromGeoJSON("/home/johannes/gsv/fpd/fraunhofer/study/data/gis/zones.geojson", "NO");
		ODUtils.cleanDistances(iais, zones, 50000, WGS84DistanceCalculator.getInstance());
		
		
		double c = ODUtils.calcNormalization(iais, model);
		playground.johannes.gsv.zones.MatrixOperations.applyFactor(model, 1/c);
		
		c = ODUtils.calcNormalization(iais, tomtom);
		playground.johannes.gsv.zones.MatrixOperations.applyFactor(tomtom, 1/c);
		
		c = ODUtils.calcNormalization(iais, sim);
		playground.johannes.gsv.zones.MatrixOperations.applyFactor(sim, 1/c);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/fpd/fraunhofer/study/analysis/28-04-2015/compare.2h.txt"));
		writer.write("from\tto\tvolIais\tvolTomTom");
		writer.newLine();
		
//		TDoubleArrayList iaisVols = new TDoubleArrayList();
//		TDoubleArrayList tomtomVols = new TDoubleArrayList();
		
		BufferedWriter scatterWriter = new BufferedWriter(new FileWriter("/home/johannes/gsv/fpd/fraunhofer/study/analysis/28-04-2015/scatter.2h.txt"));
		scatterWriter.write("iais\ttomtom\tmodel\tsim");
		scatterWriter.newLine();
		
		Set<String> keys = iais.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double volIais = iais.get(i, j);
				if(volIais != null) {
					
					Double volTomTom = tomtom.get(i, j);
					if(volTomTom == null) volTomTom = 0.0;
					
					Double volModel = model.get(i, j);
					if(volModel == null) volModel = 0.0;
					
					Double volSim = sim.get(i, j);
					if(volSim == null) volSim = 0.0;
					
					writer.write(i);
					writer.write("\t");
					writer.write(j);
					writer.write("\t");
					writer.write(String.valueOf(volIais));
					writer.write("\t");
					writer.write(String.valueOf(volTomTom));
					writer.newLine();
					
					scatterWriter.write(String.valueOf(volIais));
					scatterWriter.write("\t");
					scatterWriter.write(String.valueOf(volTomTom));
					scatterWriter.write("\t");
					scatterWriter.write(String.valueOf(volModel));
					scatterWriter.write("\t");
					scatterWriter.write(String.valueOf(volSim));
					scatterWriter.newLine();
				}
			}
		}
		
		writer.close();
		scatterWriter.close();
	}

}

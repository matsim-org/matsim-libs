/* *********************************************************************** *
 * project: org.matsim.*
 * TextReferenzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.referencing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;

import playground.gregor.gis.referencing.CRN.CaseNode;
import playground.gregor.gis.utils.ShapeFileReader;

public class TextReferencer {
	
	
	
	private Collection<Feature> referenced;
	private String unclassified;
	private CRN crn;

	public TextReferencer(Collection<Feature> ft) {
		this.referenced = ft;
		this.crn = new CRN(ft);
		
		
	}

	
	private void classify(String unclassified) {
		TextFileReader tfr = new TextFileReader(unclassified);
		TextFileWriter rfw = new TextFileWriter("test.csv");
		
		String [] out = new String [3];
		String [] line = tfr.readLine();
		
		while (line != null) {
			String query = line[6];
			if (query.toLowerCase().equals("jl. damar iii")) {
				int ii =0; 
				ii++;
			}
			CaseNode resp = this.crn.getCase(query);
			if (resp == null){ 
				line = tfr.readLine();
				continue;
			}
			System.out.println("Query: " + query.toLowerCase() + " Resp:" + resp.getExpression() + "   Activation:" + resp.getActivation());
			out[0] = query.toLowerCase();
			out[1] = resp.getExpression();
			out[2] = resp.getActivation() + "";
			rfw.writeLine(out);
			line = tfr.readLine();
			
		}
		
		rfw.finish();
		
	}
	
	
	
	public static void main(String [] args) throws Exception {
		String referenced =  "./padang/referencing/referenced.shp";
		String unclassified = "./padang/referencing/unclassified.csv";
		Collection<Feature> ft = getFeatures(ShapeFileReader.readDataFile(referenced));
		
		new TextReferencer(ft).classify(unclassified);
		
		
		
	}





	private static Collection<Feature> getFeatures(FeatureSource n) {
		Collection<Feature> features = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
//			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
//			if (multiPolygon.getNumGeometries() > 1) {
//				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
//			}
//			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			features.add(feature);
	}
	
		return features;
	}
}

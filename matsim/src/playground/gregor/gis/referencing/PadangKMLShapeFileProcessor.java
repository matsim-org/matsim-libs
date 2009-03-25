/* *********************************************************************** *
 * project: org.matsim.*
 * PadangKMLShapeFileProcessor.java
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
import java.util.HashMap;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class PadangKMLShapeFileProcessor {


	public static void main(final String [] args) throws IOException {
		
		String fileName = "./ttt/paths2.shp";
		String locations = "./ttt/locations.csv";
		TextFileReader tfr = new TextFileReader(locations,';',4);
		HashMap<Integer,String> names = new HashMap<Integer,String>();
		String [] line = tfr.readLine();
		while (line != null) {
			int id = Integer.parseInt(line[0]);
			String name = line[3];
			names.put(id, name);
			
			
			line = tfr.readLine();
		}
		
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		FeatureGenerator pfg = new FeatureGenerator(crs,new String[] {"ids", "id", "name"});
		FeatureSource fs = ShapeFileReader.readDataFile(fileName);
		FeatureReader fr = new FeatureReader();
		Collection<Feature> fts = fr.getFeatures(fs);
		Collection<Feature> dubbed = new ArrayList<Feature>();
		for (Feature ft : fts) {

			String ids = (String) ft.getAttribute(2);
			String tmp = "";
			for (int i = 0; i < ids.length(); i++) {
				if (ids.charAt(i) != ',') {
					tmp += ids.charAt(i);
				} else {
					System.out.println(tmp);
					int id = Integer.parseInt(tmp);
					String name = names.get(id);
					LineString ls = (LineString) ((MultiLineString)ft.getDefaultGeometry()).getGeometryN(0);
					
					dubbed.add(pfg.getFeature(ls.getCoordinates(), new String[] {ids,tmp,name}));
					tmp = ""; 
				}
			}
			System.out.println(tmp);
			int id = Integer.parseInt(tmp);
			String name = names.get(id);
			LineString ls = (LineString) ((MultiLineString)ft.getDefaultGeometry()).getGeometryN(0);
			
			dubbed.add(pfg.getFeature(ls.getCoordinates(), new String[] {ids,tmp,name}));
			
		}
		ShapeFileWriter.writeGeometries(dubbed, "./ttt/manuallyReferenced02.shp" );
	
		
	}
}

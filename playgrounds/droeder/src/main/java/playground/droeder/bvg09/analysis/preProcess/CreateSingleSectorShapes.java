/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.bvg09.analysis.preProcess;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author droeder
 *
 */
public class CreateSingleSectorShapes {
	
//	@SuppressWarnings("unchecked")
//	public static void main(String[] args) throws IOException{
//		final String SHAPEFILE = "D:/VSP/BVG09_Auswertung/input/Bezirke_BVG_zone.SHP";
//		final String OUTDIR = "D:/VSP/BVG09_Auswertung/BerlinSHP/sectors/";
//		FeatureSource features = ShapeFileReader.readDataFile(SHAPEFILE);
//		HashMap<Integer, Collection<SimpleFeature>> newFeatures = new HashMap<Integer, Collection<SimpleFeature>>();
//		
//		Feature f;
//		Collection<SimpleFeature> temp;
//		for(Iterator<SimpleFeature> it = features.getFeatures().iterator(); it.hasNext(); ){
//			f = it.next();
//			temp  = new LinkedList<SimpleFeature>(); 
//			temp.add(f);
//			ShapeFileWriter.writeGeometries(temp, OUTDIR + "Berlin_Zone_" + f.getAttribute(4) + "_sector_" + f.getAttribute(1) + ".shp");
//			if(!newFeatures.containsKey(f.getAttribute(4))){
//				newFeatures.put((Integer) f.getAttribute(4), new LinkedList<SimpleFeature>());
//			}
//			newFeatures.get(f.getAttribute(4)).add(f);
//		}
//		
//		for(Entry<Integer, Collection<SimpleFeature>> e: newFeatures.entrySet()){
//			ShapeFileWriter.writeGeometries(e.getValue(), OUTDIR + "Berlin_Zone_" + e.getKey() + ".shp");
//		}
//		
//	}
	
	public static void main(String[] args) throws IOException{
		final String SHAPEFILE = "D:/VSP/BVG09_Auswertung/input/Bezirke_BVG_zone.SHP";
		final String OUTDIR = "D:/VSP/BVG09_Auswertung/BerlinSHP/sectors/";
		HashMap<String, Collection<SimpleFeature>> newFeatures = new HashMap<String, Collection<SimpleFeature>>();
		
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(SHAPEFILE)) {
			if(!((Integer)f.getAttribute(4) == 4)){
				if(!newFeatures.containsKey(f.getAttribute(1).toString().substring(0, 5))){
					newFeatures.put(f.getAttribute(1).toString().substring(0, 5), new LinkedList<SimpleFeature>());
				}
				newFeatures.get(f.getAttribute(1).toString().substring(0, 5)).add(f);
			}
		}
		
		for(Entry<String, Collection<SimpleFeature>> e: newFeatures.entrySet()){
			ShapeFileWriter.writeGeometries(e.getValue(), OUTDIR + "Berlin_Zone_" + e.getKey() + ".shp");
		}
	}
	
//	public static void main(String[] args) throws IOException{
//		final String SHAPEFILE1 = "D:/VSP/BVG09_Auswertung/BerlinSHP/sectors_new/Mitte.shp";
//		final String SHAPEFILE2 = "D:/VSP/BVG09_Auswertung/BerlinSHP/sectors_new/Wedding_Tiergarten.shp";
//		
//		final String OUTDIR = "D:/VSP/BVG09_Auswertung/BerlinSHP/sectors_new/";
//		
//		FeatureSource features1 = ShapeFileReader.readDataFile(SHAPEFILE1);
//		FeatureSource features2 = ShapeFileReader.readDataFile(SHAPEFILE2);
//		
//		Feature one = (Feature) features1.getFeatures().iterator().next();
//		Feature two = (Feature) features2.getFeatures().iterator().next();
//		
//		Collection<SimpleFeature> features = new LinkedList<SimpleFeature>();
//		features.add(one);
//		features.add(two);
//		
//		ShapeFileWriter.writeGeometries(features, OUTDIR + "temp.shp");
//		
//	}

}

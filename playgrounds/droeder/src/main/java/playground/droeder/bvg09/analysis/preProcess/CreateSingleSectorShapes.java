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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

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
//		HashMap<Integer, Collection<Feature>> newFeatures = new HashMap<Integer, Collection<Feature>>();
//		
//		Feature f;
//		Collection<Feature> temp;
//		for(Iterator<Feature> it = features.getFeatures().iterator(); it.hasNext(); ){
//			f = it.next();
//			temp  = new LinkedList<Feature>(); 
//			temp.add(f);
//			ShapeFileWriter.writeGeometries(temp, OUTDIR + "Berlin_Zone_" + f.getAttribute(4) + "_sector_" + f.getAttribute(1) + ".shp");
//			if(!newFeatures.containsKey(f.getAttribute(4))){
//				newFeatures.put((Integer) f.getAttribute(4), new LinkedList<Feature>());
//			}
//			newFeatures.get(f.getAttribute(4)).add(f);
//		}
//		
//		for(Entry<Integer, Collection<Feature>> e: newFeatures.entrySet()){
//			ShapeFileWriter.writeGeometries(e.getValue(), OUTDIR + "Berlin_Zone_" + e.getKey() + ".shp");
//		}
//		
//	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException{
		final String SHAPEFILE = "D:/VSP/BVG09_Auswertung/input/Bezirke_BVG_zone.SHP";
		final String OUTDIR = "D:/VSP/BVG09_Auswertung/BerlinSHP/sectors/";
		FeatureSource features = ShapeFileReader.readDataFile(SHAPEFILE);
		HashMap<String, Collection<Feature>> newFeatures = new HashMap<String, Collection<Feature>>();
		
		Feature f;
		for(Iterator<Feature> it = features.getFeatures().iterator(); it.hasNext(); ){
			f = it.next();
			if(!((Integer)f.getAttribute(4) == 4)){
				if(!newFeatures.containsKey(f.getAttribute(1).toString().substring(0, 5))){
					newFeatures.put(f.getAttribute(1).toString().substring(0, 5), new LinkedList<Feature>());
				}
				newFeatures.get(f.getAttribute(1).toString().substring(0, 5)).add(f);
			}
		}
		
		for(Entry<String, Collection<Feature>> e: newFeatures.entrySet()){
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
//		Collection<Feature> features = new LinkedList<Feature>();
//		features.add(one);
//		features.add(two);
//		
//		ShapeFileWriter.writeGeometries(features, OUTDIR + "temp.shp");
//		
//	}

}

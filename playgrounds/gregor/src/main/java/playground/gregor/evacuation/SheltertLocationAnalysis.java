/* *********************************************************************** *
 * project: org.matsim.*
 * SheltertLocationAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.evacuation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


public class SheltertLocationAnalysis {
	
	
	private static final Map<String,Integer> shelterCaps = new HashMap<String,Integer>();
	private static final Map<String,Feature> featureMap = new HashMap<String,Feature>();
	
	public static void main(String [] args) throws FileNotFoundException, IOException, IllegalAttributeException {
		String RUN = "1048";
		int it = 2000;
		String buildings = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/network/evac_zone_buildings_250m_shelter_grid_v20100615.shp";
		String log = "/home/laemmel/arbeit/svn/runs-svn/run" + RUN + "/output/logfile.log";
		String out = "/home/laemmel/arbeit/svn/runs-svn/run" + RUN + "/analysis/shelters_it" + it + ".shp";
		
		String activationTag = "ITERATION " + it + " BEGINS";
		
		getShelterCaps(activationTag, log);
		getFeatureMap(buildings);
		createShapes(out);
		
		
	}

	private static void createShapes(String out) throws IllegalAttributeException, IOException {
		FeatureType ft = initFeatureType();
		GeometryFactory geofac = new GeometryFactory();
		List<Feature> fts = new ArrayList<Feature>(); 
		for (Entry<String, Integer> e : shelterCaps.entrySet()) {
			Feature f = featureMap.get(e.getKey());
			Point p = f.getDefaultGeometry().getCentroid();
			fts.add(ft.create(new Object[]{p,e.getValue(), e.getValue()/10, e.getValue()/50}));
		}
		ShapeFileWriter.writeGeometries(fts, out);
	}

	private static FeatureType initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
		AttributeType cap = AttributeTypeFactory.newAttributeType("cap", Integer.class);
		AttributeType cap10 = AttributeTypeFactory.newAttributeType("cap10", Integer.class);
		AttributeType cap50 = AttributeTypeFactory.newAttributeType("cap50", Integer.class);
//		AttributeType agLostRate = AttributeTypeFactory.newAttributeType("agLostRate", Double.class);
//		AttributeType agLostPerc = AttributeTypeFactory.newAttributeType("agLostPerc", Integer.class);
//		AttributeType agLostPercStr = AttributeTypeFactory.newAttributeType("agLostPercStr", String.class);
//		AttributeType agLabel = AttributeTypeFactory.newAttributeType("agLabel", String.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, cap, cap10, cap50}, "Shelters");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}
	
	
	private static void getFeatureMap(String buildings) throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(buildings);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			String key = "sl" + ((Integer) ft.getAttribute("ID"))+"b";
			featureMap.put(key, ft);
		}
		
	}

	private static void getShelterCaps(String activationTag, String log) throws FileNotFoundException, IOException {
		boolean active = false;
		
		BufferedReader infile = IOUtils.getBufferedReader(log);
		String line = infile.readLine();
		while (line != null) {
			if (line.contains(activationTag)) {
				active = true;
			}
			if (!active) {
				line = infile.readLine();
				continue;
			}
			if (line.contains("total Shelters capacity")) {
				handleShelterCapBlock(infile);
				break;
			}
			line = infile.readLine();
			
			
			
		}
		infile.close();
		
	}

	private static void handleShelterCapBlock(BufferedReader infile) throws IOException {
		
		for (String line = infile.readLine(); !line.contains(" ======================================"); line = infile.readLine()) {
			String [] tokLine = StringUtils.explode(line, ' ',10);
			String id = tokLine[6];
			if (id.equals("el1")) {
//				line = infile.readLine();
				continue;
			}
			int cap = Integer.parseInt(StringUtils.explode(tokLine[8], ':',2)[1]);
			if (cap > 0) {
				System.out.println("id:" + id + " cap:" +cap);
				shelterCaps.put(id, cap);
			}
//			line = infile.readLine();
		}
		
	}

}

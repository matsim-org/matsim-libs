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

/**
 * 
 */
package playground.johannes.gsv.demand;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.opengis.feature.simple.SimpleFeature;
import playground.johannes.sna.gis.Zone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class NutsLevel3Zones {

	public static String zonesFile;
	
	public static String idMappingsFile;
	
	private static Map<String, Zone<?>> zones;
	
	public static Zone<?> getZone(String id) {
		if(zones == null) {
			try {
				loadZones();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return zones.get(id);
	}
	
	private static void loadZones() throws IOException {
		if(zonesFile == null && idMappingsFile == null)
			throw new RuntimeException("File names not set.");
		
		Set<SimpleFeature> features = EsriShapeIO.readFeatures(zonesFile);
		Map<String, SimpleFeature> featureMap = new HashMap<String, SimpleFeature>(features.size());
		for(SimpleFeature fearure : features) {
			String code = (String)fearure.getAttribute("NUTS3_CODE");
			featureMap.put(code, fearure);
		}
		
		zones = new LinkedHashMap<String, Zone<?>>();
		BufferedReader reader = new BufferedReader(new FileReader(idMappingsFile));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			String gsvId = tokens[0];
			String nutsId = tokens[1];
			SimpleFeature feature = featureMap.get(nutsId);
			if(feature != null) {
				Zone<?> zone = new Zone<Object>(((Geometry)feature.getDefaultGeometry()).getGeometryN(0));
//				zone.getGeometry().setSRID(4326);
				zones.put(gsvId, zone);
			}
			
		}
		
		reader.close();
	}
}

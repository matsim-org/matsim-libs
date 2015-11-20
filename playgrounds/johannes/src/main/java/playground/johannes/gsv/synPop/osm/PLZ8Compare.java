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

package playground.johannes.gsv.synPop.osm;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class PLZ8Compare {

	private final static Logger logger = Logger.getLogger(PLZ8Compare.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		logger.info("Reading zones...");
		Set<SimpleFeature> features = EsriShapeIO.readFeatures("/home/johannes/gsv/Marktzellen_2011/Grenzen+Sachdaten/PLZ8_10w_XXL_region.shp");
		logger.info("Reading attributes...");
		Map<String, Integer> values = readAttributes("/home/johannes/gsv/Marktzellen_2011/Mitarbeiter_Einkauf.csv");
		
		Set<Zone<Integer>> zones = new HashSet<Zone<Integer>>();
		logger.info("Merging attributes...");
		for(SimpleFeature feature : features) {
			String id = (String) feature.getAttribute("PLZ8");
			Zone<Integer> zone = new Zone<Integer>(((Geometry) feature.getDefaultGeometry()).getGeometryN(0));
			Integer val = values.get(id);
			if(val != null) {
				zone.setAttribute(val);
				zones.add(zone);
			}
		}
		ZoneLayer<Integer> zoneLayer = new ZoneLayer<Integer>(zones);
		
		logger.info(String.format("Loaded %s zones.", zones.size()));
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/osm/hessen-shops2.xml");
//		parser.parse("/home/johannes/gsv/osm/hessen-buildings.osm");
//		parser.parse("/home/johannes/gsv/osm/hessen-landuse.osm");
		Map<String, OSMWay> ways = parser.getWays();

		logger.info(String.format("%s nodes,  %s ways", parser.getNodes().size(), parser.getWays().size()));
		
		logger.info("Converting ways...");
		Set<Geometry> geometries = new HashSet<Geometry>();
		OSM2Geometry osm2Geo = new OSM2Geometry();
		for(OSMWay way : ways.values()) {
			
			try{
			Geometry geo = osm2Geo.convert(way);
			geometries.add(geo);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		
		TObjectDoubleHashMap<Zone<?>> areas = new TObjectDoubleHashMap<Zone<?>>();
		
		logger.info("Summing areas...");
		int nullZones = 0;
		for(Geometry geo : geometries) {
			Point p = geo.getCentroid();
			Zone<?> zone = zoneLayer.getZone(p);
			if(zone == null)
				nullZones++;
			else {
//			double A = geo.getArea();
//			areas.adjustOrPutValue(zone, A, A);
				areas.adjustOrPutValue(zone, 1, 1);
			}
		}
		logger.warn(String.format("No zone found for %s buildings.", nullZones));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/osm/areacompare.txt"));
		writer.write("Area\tEmployees");
		writer.newLine();
		
		for(Zone<Integer> zone : zoneLayer.getZones()) {
			double A = areas.get(zone);
			if(A > 0) {
			writer.write(String.valueOf(A));
			writer.write("\t");
			writer.write(String.valueOf(zone.getAttribute()));
			writer.newLine();
			}
		}
		
		writer.close();
	}
	
	private static Map<String, Integer> readAttributes(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Map<String, Integer> values = new HashMap<String, Integer>();
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";");
			values.put(tokens[0], Integer.parseInt(tokens[4]));
		}
		
		reader.close();
		
		return values;
	}
	

}

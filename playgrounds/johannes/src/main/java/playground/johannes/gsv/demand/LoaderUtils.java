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
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 *
 */
public class LoaderUtils {

	public static final String FIELD_SEPARATOR = "\t";
	
	public static ZoneLayer<Double> loadSingleColumnRelative(String file, String key) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		/*
		 * read headers
		 */
		String line = reader.readLine();
		
		String[] tokens = line.split(FIELD_SEPARATOR);
		int idx = 0;
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals(key)) {
				idx = i;
				break;
			}
		}
		/*
		 * read lines
		 */
		Set<Zone<Double>> zones = new LinkedHashSet<Zone<Double>>();
		Map<Zone<?>, Integer> values = new LinkedHashMap<Zone<?>, Integer>();
		int total = 0;
		while((line = reader.readLine()) != null) {
			tokens = line.split(FIELD_SEPARATOR);
			String id = tokens[0];
			if(!id.isEmpty()) {
				Zone<?> zone = NutsLevel3Zones.getZone(id);
			
				int val = Integer.parseInt(tokens[idx]);
				total += val;

				Zone<Double> newzone = new Zone<Double>(zone.getGeometry());
				zones.add(newzone);
				values.put(newzone, val);
			}
		}
		reader.close();
		/*
		 * fill zones
		 */
		for(Zone<Double> zone : zones) {
			zone.setAttribute(values.get(zone)/ (double)total);
		}
		return new ZoneLayer<Double>(zones);
	}
	
	public static ZoneLayer<Double> loadSingleColumnAbsolute(String file, String key) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		/*
		 * read headers
		 */
		String line = reader.readLine();
		
		String[] tokens = line.split(FIELD_SEPARATOR);
		int idx = 0;
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals(key)) {
				idx = i;
				break;
			}
		}
		/*
		 * read lines
		 */
		Set<Zone<Double>> zones = new HashSet<Zone<Double>>();
		Map<Zone<?>, Double> values = new HashMap<Zone<?>, Double>();
//		int total = 0;
		while((line = reader.readLine()) != null) {
			tokens = line.split(FIELD_SEPARATOR);
			String id = tokens[0];
			if(!id.isEmpty()) {
				Zone<?> zone = NutsLevel3Zones.getZone(id);
			
				double val = Double.parseDouble(tokens[idx]);
//				total += val;

				Zone<Double> newzone = new Zone<Double>(zone.getGeometry());
				zones.add(newzone);
				values.put(newzone, val);
			}
		}
		reader.close();
		/*
		 * fill zones
		 */
		for(Zone<Double> zone : zones) {
			zone.setAttribute(values.get(zone));
		}
		return new ZoneLayer<Double>(zones);
	}
	
	public static TObjectDoubleHashMap<String> loadSingleColumn(String file, String key) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		/*
		 * read headers
		 */
		String line = reader.readLine();
		
		String[] tokens = line.split(FIELD_SEPARATOR);
		int idx = 0;
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals(key)) {
				idx = i;
				break;
			}
		}
		/*
		 * read lines
		 */
		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<String>();
		while((line = reader.readLine()) != null) {
			tokens = line.split(FIELD_SEPARATOR);
			String id = tokens[0];
			if(!id.isEmpty()) {
				double val = Double.parseDouble(tokens[idx]);
				values.put(id, val);
			}
		}
		reader.close();
		
		return values;
	}
	
	public static ZoneLayer<Double> mapValuesToZones(TObjectDoubleHashMap<String> values) {
		Set<Zone<Double>> zones = new LinkedHashSet<Zone<Double>>();
		TObjectDoubleIterator<String> it = values.iterator();
		while(it.hasNext()) {
			it.advance();
			String key = it.key();
			Zone<?> nutsZone = NutsLevel3Zones.getZone(key);
			if (nutsZone != null) {
				Geometry geometry = nutsZone.getGeometry();
				Zone<Double> zone = new Zone<Double>(geometry);
				zone.setAttribute(it.value());
				zones.add(zone);
			}
		}
		
		return new ZoneLayer<Double>(zones);
	}
}

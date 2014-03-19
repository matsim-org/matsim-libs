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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;


/**
 * @author johannes
 *
 */
public class PersonEqualZoneDistributionWrapper extends AbstractTaskWrapper {

	public PersonEqualZoneDistributionWrapper(String file, String key, Random random) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		/*
		 * read headers
		 */
		String line = reader.readLine();
		
		String[] tokens = line.split("\t");
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
		Map<Zone<?>, Integer> values = new HashMap<Zone<?>, Integer>();
		int total = 0;
		while((line = reader.readLine()) != null) {
			tokens = line.split("\t");
			String id = tokens[0];
			int val = Integer.parseInt(tokens[idx]);
			total += val;
			
			Zone<Double> zone = new Zone<Double>(NutsLevel3Zones.getZone(id).getGeometry()); 
			zones.add(zone);
			values.put(zone, val);
		}
		reader.close();
		/*
		 * fill zones
		 */
		for(Zone<Double> zone : zones) {
			zone.setAttribute(values.get(zone)/ (double)total);
		}
		ZoneLayer<Double> zoneLayer = new ZoneLayer<Double>(zones);
		
		this.delegate = new PersonEqualZoneDistribution(zoneLayer, random);
	}

}

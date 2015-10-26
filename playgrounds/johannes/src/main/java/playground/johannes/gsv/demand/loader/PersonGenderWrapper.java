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
package playground.johannes.gsv.demand.loader;

import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.NutsLevel3Zones;
import playground.johannes.gsv.demand.tasks.PersonGender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;


/**
 * @author johannes
 *
 */
public class PersonGenderWrapper extends AbstractTaskWrapper {

	public PersonGenderWrapper(String file, String maleKey, String femaleKey, Random random) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		/*
		 * read headers
		 */
		String line = reader.readLine();
		
		String[] tokens = line.split("\t");
		int maleIdx = 0;
		int femaleIdx = 0;
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals(maleKey)) {
				maleIdx = i;
			} else if(tokens[i].equals(femaleKey)) {
				femaleIdx = i;
			}
		}
		/*
		 * read lines
		 */
		Set<Zone<Double>> zones = new LinkedHashSet<Zone<Double>>();
		while((line = reader.readLine()) != null) {
			tokens = line.split("\t");
			String id = tokens[0];
			Zone<?> zone = NutsLevel3Zones.getZone(id);
			if (zone != null) {
				int males = Integer.parseInt(tokens[maleIdx]);
				int females = Integer.parseInt(tokens[femaleIdx]);

				Zone<Double> newzone = new Zone<Double>(zone.getGeometry());
				newzone.setAttribute(females / (double) (males + females));
				zones.add(newzone);
			}
			
		}
		reader.close();
		
		ZoneLayer<Double> zoneLayer = new ZoneLayer<Double>(zones);
		
		this.delegate = new PersonGender(zoneLayer, random);
	}
}

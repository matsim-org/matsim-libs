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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class PersonAgeWrapper extends AbstractTaskWrapper {

	public PersonAgeWrapper(String baseDir, String key, Random random) throws IOException {
		File file = new File(baseDir);
		Map<String, List<Category>> categories = new HashMap<String, List<Category>>();
		for(String fil2 : file.list()) {
			if(fil2.startsWith("age.")) {
				String[] tokens = fil2.split("\\.");
				String[] tokens2 = tokens[1].split("-");
				int lower = Integer.parseInt(tokens2[0]);
				int upper = Integer.parseInt(tokens2[1]);
				
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				/*
				 * read headers
				 */
				String line = reader.readLine();
				
				String[] tokens3 = line.split("\t");
				int idx = 0;
				for(int i = 0; i < tokens.length; i++) {
					if(tokens3[i].equals(key)) {
						idx = i;
						break;
					}
				}
				/*
				 * 
				 */
				while((line = reader.readLine()) != null) {
					String tokens4[] = line.split("\t");
					int val = Integer.parseInt(tokens4[idx]);
					String id = tokens4[0];
					
					Category cat = new Category();
					cat.lower = lower;
					cat.upper = upper;
					cat.value = val;
					List<Category> list = categories.get(id);
					list.add(cat);
				}
				reader.close();
			}
		}
		
		Set<Zone<ChoiceSet<Tuple<Integer, Integer>>>> zones = new HashSet<Zone<ChoiceSet<Tuple<Integer, Integer>>>>();
		for(Entry<String, List<Category>> entry : categories.entrySet()) {
			Zone<ChoiceSet<Tuple<Integer, Integer>>> zone = new Zone<ChoiceSet<Tuple<Integer, Integer>>>(NutsLevel3Zones.getZone(entry.getKey()).getGeometry());
			ChoiceSet<Tuple<Integer, Integer>> choiceSet = new ChoiceSet<Tuple<Integer,Integer>>(random);
			
			for(Category cat : entry.getValue()) {
				Tuple<Integer, Integer> bounds = new Tuple<Integer, Integer>(cat.lower, cat.upper);
				choiceSet.addChoice(bounds, cat.value);
			}
			zone.setAttribute(choiceSet);
			zones.add(zone);
		}
		ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>> zoneLayer = new ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>>(zones);
		this.delegate = new PersonAge(zoneLayer, random);
	}
	
	private static class Category {
		
		private int lower;
		
		private int upper;
		
		private int value;
	}
}

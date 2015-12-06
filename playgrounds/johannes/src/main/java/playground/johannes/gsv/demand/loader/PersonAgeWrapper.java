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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.contrib.common.collections.ChoiceSet;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.NutsLevel3Zones;
import playground.johannes.gsv.demand.tasks.PersonAge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class PersonAgeWrapper extends AbstractTaskWrapper {
	
	private static final String PREFIX = "age.";

	public PersonAgeWrapper(String baseDir, String key, Random random) throws IOException {
		File dirFile = new File(baseDir);
		Map<String, List<Category>> categories = new HashMap<String, List<Category>>();
		for(String file : dirFile.list()) {
			if(file.startsWith(PREFIX)) {
				readFile(baseDir + file, key, categories);
			}
		}
		
		Set<Zone<ChoiceSet<Tuple<Integer, Integer>>>> zones = new LinkedHashSet<Zone<ChoiceSet<Tuple<Integer, Integer>>>>();
		for(Entry<String, List<Category>> entry : categories.entrySet()) {
			Zone<?> zone = NutsLevel3Zones.getZone(entry.getKey());
			if (zone != null) {
				Zone<ChoiceSet<Tuple<Integer, Integer>>> newzone = new Zone<ChoiceSet<Tuple<Integer, Integer>>>(
						zone.getGeometry());
				ChoiceSet<Tuple<Integer, Integer>> choiceSet = new ChoiceSet<Tuple<Integer, Integer>>(
						random);

				for (Category cat : entry.getValue()) {
					Tuple<Integer, Integer> bounds = new Tuple<Integer, Integer>(
							cat.lower, cat.upper);
					choiceSet.addOption(bounds, cat.inhabitants);
				}
				newzone.setAttribute(choiceSet);
				zones.add(newzone);
			}
		}
		ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>> zoneLayer = new ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>>(zones);
		this.delegate = new PersonAge(zoneLayer, random);
	}
	
	private void readFile(String file, String key, Map<String, List<Category>> categories) throws IOException {
		String[] fileNameElements = file.split("\\.");
		String[] bounds = fileNameElements[fileNameElements.length-2].split("-");
		int lower = Integer.parseInt(bounds[0]);
		int upper = Integer.parseInt(bounds[1]);
		
		
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
		 * 
		 */
		while((line = reader.readLine()) != null) {
			tokens = line.split("\t");
			int val = Integer.parseInt(tokens[idx]);
			String id = tokens[0];
			
			Category cat = new Category();
			cat.lower = lower;
			cat.upper = upper;
			cat.inhabitants = val;
			List<Category> list = categories.get(id);
			if(list == null) {
				list = new ArrayList<PersonAgeWrapper.Category>(50);
				categories.put(id, list);
			}
			list.add(cat);
		}
		reader.close();
	}
	
	private static class Category {
		
		private int lower;
		
		private int upper;
		
		private int inhabitants;
	}
}

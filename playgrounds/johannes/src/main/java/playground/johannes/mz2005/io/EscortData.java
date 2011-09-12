/* *********************************************************************** *
 * project: org.matsim.*
 * AccompanistsData.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.mz2005.io;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author illenberger
 * 
 */
public class EscortData {

	private Map<String, TIntIntHashMap> data = new HashMap<String, TIntIntHashMap>();

	public void add(Person person, int legIndex, int escorts) {
		TIntIntHashMap map = data.get(person.getId().toString());
		if (map == null) {
			map = new TIntIntHashMap();
			data.put(person.getId().toString(), map);
		}
		map.put(legIndex, escorts);
	}

	public int getEscorts(Person person, int legIndex) {
		TIntIntHashMap map = data.get(person.getId().toString());
		if (map == null)
			return 0;
		else
			return map.get(legIndex);
	}

	public void write(String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.write("personId\tlegIndex\tescorts");
		writer.newLine();

		for (Entry<String, TIntIntHashMap> entry : data.entrySet()) {
			TIntIntHashMap map = entry.getValue();
			TIntIntIterator it = map.iterator();
			for (int i = 0; i < map.size(); i++) {
				it.advance();

				writer.write(entry.getKey());
				writer.write("\t");
				writer.write(String.valueOf(it.key()));
				writer.write("\t");
				writer.write(String.valueOf(it.value()));

				writer.newLine();
			}
		}

		writer.close();
	}

	public static EscortData read(String file, Population population) throws IOException {
		EscortData data = new EscortData();

		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");

			Person person = population.getPersons().get(new IdImpl(tokens[0]));
			if (person != null) {
				int legIndex = Integer.parseInt(tokens[1]);
				int escorts = Integer.parseInt(tokens[2]);

				Plan plan = person.getSelectedPlan();
				if (plan.getPlanElements().size() < legIndex + 1)
					throw new IllegalArgumentException(String.format("This plan has no element with index %1$s.",
							legIndex));

				data.add(person, legIndex, escorts);
			}
		}

		return data;
	}
}

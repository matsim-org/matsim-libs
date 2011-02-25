/* *********************************************************************** *
 * project: org.matsim.*
 * ChoiceSet.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.contrib.sna.util.TXTWriter;

/**
 * @author illenberger
 * 
 */
public class ChoiceSet {

	private static final Logger logger = Logger.getLogger(ChoiceSet.class);

	private List<Person> opportunities;

	private long[] randomSeeds;

	private static final int k = 5;

	private static final double gamma = -1.6;

	public static Map<Person, ChoiceSet> create(Population population, Random random, String output) {
		Map<Person, ChoiceSet> choiceSets = new HashMap<Person, ChoiceSet>();

		List<Person> personList = new ArrayList<Person>(population.getPersons().values());
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();

		logger.info("Building choice set...");
		Discretizer discretizer = new LinearDiscretizer(500.0);

		int N = population.getPersons().size();
		ProgressLogger.init(N, 1, 5);
		for (Person person : population.getPersons().values()) {
			List<Person> choice = new ArrayList<Person>(k);
//			double sum = 0;
//			for (Person p2 : personList) {
//				double d = calcDistance(person, p2);
//				d = discretizer.index(d);
//				d = Math.max(d, 1.0);
//				sum += Math.pow(d, gamma);
//			}

			while (choice.size() < k) {
//			for(Person p2 : personList) {
				Person p2 = personList.get(random.nextInt(N));
				if (!p2.equals(person)) {
					double d = calcDistance(person, p2);
					d = discretizer.index(d);
					d = Math.max(d, 1.0);

//					double p = k / sum * Math.pow(d, gamma);
					double p = Math.pow(d, gamma);

					if (random.nextDouble() <= p) {

						choice.add(p2);
						hist.adjustOrPutValue(d, 1, 1);
					}
				}
			}
			
		
			ChoiceSet set = new ChoiceSet();
			set.opportunities = choice;

			// Arrays.fill(set.randomSeeds, 0);

			choiceSets.put(person, set);

			ProgressLogger.step();
		}
		try {
			TXTWriter.writeMap(hist, "d", "n", output + "/choiceset.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return choiceSets;
	}

	private static double calcDistance(Person p1, Person p2) {
		Coord c1 = ((Activity) p1.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Coord c2 = ((Activity) p2.getSelectedPlan().getPlanElements().get(0)).getCoord();

		double dx = c1.getX() - c2.getX();
		double dy = c1.getY() - c2.getY();
		double d = Math.sqrt(dx * dx + dy * dy);

		return d;
	}

	public List<Person> getOpportunities() {
		return opportunities;
	}

	public long[] getRandomSeeds() {
		return randomSeeds;
	}
}

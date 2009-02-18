/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.socialnets;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.population.Person;
import org.matsim.population.Population;

import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class DistanceDistribution {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioData data = new ScenarioData(config);
		Population population = data.getPopulation();
		
		Collection<Person> persons2 = new HashSet<Person>();
		double xmin = 678000;
		double ymin = 243000;
		double xmax = 687000;
		double ymax = 254000;
		for(Person p : population) {
			Coord c = p.getSelectedPlan().getFirstActivity().getCoord();
			if(c.getX() >= xmin && c.getX() <= xmax && c.getY() >= ymin && c.getY() <= ymax)
				persons2.add(p);
		}
		
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		double binsize = 1000;
		int count = 0;
		for(Person p1 : population) {
			persons2.remove(p1);
			for(Person p2 : persons2) {
				Coord c1 = p1.getSelectedPlan().getFirstActivity().getCoord();
				Coord c2 = p2.getSelectedPlan().getFirstActivity().getCoord();
				double d = c1.calcDistance(c2);
				double bin = Math.floor(d/binsize);
				double val = hist.get(bin);
				val++;
				hist.put(bin, val);
			}
			count++;
			if(count % 1000 == 0) {
				System.out.println(String.format(
						"Processed %1$s of %2$s persons. (%3$s )", count,
						population.getPersons().size(), count
								/ (double) population.getPersons().size()));
			}
		}
		
		WeightedStatistics.writeHistogram(hist, args[1]);
	}

}

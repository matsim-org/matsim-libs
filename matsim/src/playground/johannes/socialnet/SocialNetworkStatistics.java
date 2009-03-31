/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnet;

import gnu.trove.TDoubleDoubleHashMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class SocialNetworkStatistics {

	public static WeightedStatistics getEdgeLengthDistribution(SocialNetwork<?> network) {
		return getEdgeLengthDistribution(network, false, 0);
	}
	
	public static WeightedStatistics getEdgeLengthDistribution(SocialNetwork<?> network, boolean normalize, double normBinSize) {
		WeightedStatistics stats = new WeightedStatistics();
		for(Ego<?> e : network.getVertices()) {
			Coord c1 = e.getCoord();
			TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
			
			if(normalize) {
				for(Ego<?> e2 : network.getVertices()) {
					Coord c2 = e2.getCoord();
					double d = CoordUtils.calcDistance(c1, c2);
					double bin = Math.floor(d / normBinSize);
					double count = hist.get(bin);
					count++;
					hist.put(bin, count);
				}
			}
			
			for(Ego<?> e2 : e.getNeighbours()) {
				double d = CoordUtils.calcDistance(e.getCoord(), e2.getCoord());
				double w = 1;
				if(normalize)
					w = 1 / hist.get(Math.floor(d / normBinSize));
				stats.add(d, w);
			}
		}
		
		return stats;
	}
	
	public static double getAgeCorrelation(SocialNetwork<Person> g) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;

		for (SocialTie e : g.getEdges()) {
			Ego<Person> v1 = (Ego<Person>) e.getVertices().getFirst();
			Ego<Person> v2 = (Ego<Person>) e.getVertices().getSecond();
			int age1 = v1.getPerson().getAge();
			int age2 = v2.getPerson().getAge();

			sum += 0.5 * (age1 + age2);
			squareSum += 0.5 * (Math.pow(age1, 2) + Math.pow(age2, 2));
			product += age1 * age2;			
		}
		
		double norm = 1 / (double)g.getEdges().size();
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
	}
}

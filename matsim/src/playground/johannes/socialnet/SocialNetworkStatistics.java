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
import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.core.api.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SocialNetworkStatistics {
	
	public static Distribution edgeLengthDistribution(SocialNetwork<? extends BasicPerson<?>> network) {
		return edgeLengthDistribution(network.getVertices());
	}
	
	public static Distribution edgeLengthDistribution(Set<? extends Ego<? extends BasicPerson<?>>> vertices) {
		Distribution stats = new Distribution();
		for(Ego<?> e : vertices) {
			for(Ego<?> e2 : e.getNeighbours()) {
				double d = CoordUtils.calcDistance(e.getCoord(), e2.getCoord());
				stats.add(d);
			}
		}
		
		return stats;
	}
	
	@SuppressWarnings("unchecked")
	public static double ageCorrelation(SocialNetwork<Person> g) {
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
	
	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(SocialNetwork<? extends BasicPerson<?>> network) {
		return edgeLengthDegreeCorrelation(network.getVertices());
	}
	
	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(Set<? extends Ego<?>> vertices) {
		TObjectDoubleHashMap<Ego<?>> d_distr = new TObjectDoubleHashMap<Ego<?>>();
		for(Ego<?> e : vertices) {
			double sum = 0;
			for(Ego<?> e2 : e.getNeighbours()) {
				sum += CoordUtils.calcDistance(e.getCoord(), e2.getCoord());
			}
			d_distr.put(e, sum/(double)e.getNeighbours().size());
		}
		
		return GraphStatistics.getValueDegreeCorrelation(d_distr);
	}
	
	public static <P extends BasicPerson<?>> TObjectDoubleHashMap<Ego<P>> meanEdgeLength(SocialNetwork<P> g) {
		return meanEdgeLength(g.getVertices());
	}
	
	public static <P extends BasicPerson<?>> TObjectDoubleHashMap<Ego<P>> meanEdgeLength(Set<? extends Ego<P>> vertices) {
		TObjectDoubleHashMap<Ego<P>> values = new TObjectDoubleHashMap<Ego<P>>();
		for (Ego<P> i : vertices) {
			if (i.getNeighbours().size() > 0) {
				double sum_d = 0;
				for (Ego<P> j : i.getNeighbours()) {
					sum_d += CoordUtils
							.calcDistance(i.getCoord(), j.getCoord());
				}
				double d_mean = sum_d / (double) i.getNeighbours().size();
				values.put(i, d_mean);
			}
		}
		return values;
	}
	
	public static <T extends Ego<? extends BasicPerson<?>>> TObjectDoubleHashMap<T> localEdgeLengthMSE(Set<T> vertices) {
		TObjectDoubleHashMap<T> mse = new TObjectDoubleHashMap<T>();
		TDoubleDoubleHashMap globalDistr = edgeLengthDistribution(vertices).normalizedDistribution(1000);
		for(T v : vertices) {
			Distribution localDistr = new Distribution();
			Coord c1 = v.getCoord();
			for(Ego<?> t : v.getNeighbours()) {
				Coord c2 = t.getCoord();
				double d = CoordUtils.calcDistance(c1, c2);
				localDistr.add(d);
			}
			mse.put(v, Distribution.meanSquareError(localDistr.normalizedDistribution(1000), globalDistr));
		}
		return mse;
	}
	
	public static <T extends Ego<? extends BasicPerson<?>>> TDoubleDoubleHashMap edgeLengthMSEDegreeCorrelation(Set<T> vertices) {
		TObjectDoubleHashMap<T> d_distr = new TObjectDoubleHashMap<T>();
		TObjectDoubleHashMap<T> mseDistr = localEdgeLengthMSE(vertices);
		for(T e : vertices) {
			d_distr.put(e, mseDistr.get(e));
		}
		
		return GraphStatistics.getValueDegreeCorrelation(d_distr);
	}
}

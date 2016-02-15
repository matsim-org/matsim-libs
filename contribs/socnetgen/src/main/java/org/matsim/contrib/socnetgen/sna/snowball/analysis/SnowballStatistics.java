/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.snowball.analysis;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Set;

import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;

/**
 * A utility class to calculate common statistics for a snowball sample.
 * 
 * @author illenberger
 * 
 */
public class SnowballStatistics {

	private static SnowballStatistics instance;

	public static SnowballStatistics getInstance() {
		if (instance == null)
			instance = new SnowballStatistics();

		return instance;
	}

	/**
	 * Counts the number of vertices that are sampled in <tt>iteration</tt>.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @param iteration
	 *            a valid snowball iteration
	 * @return the number of vertices that are sampled in <tt>iteration</tt>.
	 */
	public int numVerticesSampled(Set<? extends SampledVertex> vertices, int iteration) {
		return numVerticesSampled(vertices)[iteration];
	}

	/**
	 * Returns an array with the number of sampled vertices per iteration.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return an array with the number of sampled vertices per iteration.
	 */
	public int[] numVerticesSampled(Set<? extends SampledVertex> vertices) {
		int it = -1;
		TIntIntHashMap map = new TIntIntHashMap();
		for (SampledVertex v : vertices) {
			if (v.isSampled()) {
				map.adjustOrPutValue(v.getIterationSampled(), 1, 1);
				it = Math.max(it, v.getIterationSampled());
			}
		}
		
		int[] list = new int[it + 1];
		for(int i = 0; i <= it; i++) {
			list[i] = map.get(i);
		}
		
		return list;
	}
	
	/**
	 * Counts the number of vertices that are sampled in or before
	 * <tt>iteration</tt>.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @param iteration
	 *            a valid snowball iteration
	 * @return the number of vertices that are sampled in or before
	 *         <tt>iteration</tt>.
	 */
	public int numVerticesSampledTotal(Set<? extends SampledVertex> vertices, int iteration) {
		return numVerticesSampledTotal(vertices)[iteration];
	}

	/**
	 * Returns an array with the number of sampled vertices in or before an
	 * iteration, where the array index corresponds to the iteration index.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return an array with the number of sampled vertices in or before an
	 *         iteration
	 */
	public int[] numVerticesSampledTotal(Set<? extends SampledVertex> vertices) {
		int[] list = numVerticesSampled(vertices);
		int[] listTotal = new int[list.length];
		
		listTotal[0] = list[0];
		for(int i = 1; i < list.length; i++) {
			listTotal[i] = listTotal[i - 1] + list[i];
		}
		
		return listTotal;
	}
	
	/**
	 * Returns an array with the number of detected vertices per iteration.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return an array with the number of detected vertices per iteration.
	 */
	public int[] numVerticesDetected(Set<? extends SampledVertex> vertices) {
		int it = -1;
		TIntIntHashMap map = new TIntIntHashMap();
		for (SampledVertex v : vertices) {
			if (v.isDetected()) {
				map.adjustOrPutValue(v.getIterationDetected(), 1, 1);
				it = Math.max(it, v.getIterationDetected());
			}
		}
		
		int[] list = new int[it + 1];
		for(int i = 0; i <= it; i++) {
			list[i] = map.get(i);
		}
		
		// add seed vertices
		list[0] += map.get(-1);
		
		return list;
	}
	
	/**
	 * Returns an array with the number of detected vertices in or before an
	 * iteration, where the array index corresponds to the iteration index.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return an array with the number of detected vertices in or before an
	 *         iteration
	 */
	public int[] numVerticesDetectedTotal(Set<? extends SampledVertex> vertices) {
		int[] list = numVerticesDetected(vertices);
		int[] listTotal = new int[list.length];
		
		listTotal[0] = list[0];
		for(int i = 1; i < list.length; i++) {
			listTotal[i] = listTotal[i - 1] + list[i];
		}
		
		return listTotal;
	}
	
	/**
	 * Returns the overall response rate up to and including iteration
	 * <tt>iteration</tt>.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @param iteration
	 *            a valid snowball iteration
	 * @return the overall response rate up to and including iteration
	 *         <tt>iteration</tt>.
	 */
	public double responseRateTotal(Set<? extends SampledVertex> vertices, int iteration) {
		if(iteration == 0)
			return 1.0;
		else
			return numVerticesSampledTotal(vertices)[iteration]/(double)numVerticesDetectedTotal(vertices)[iteration - 1];
	}
	
	/**
	 * Returns an array with the overall response rates up to and including an
	 * iteration, where the iteration index corresponds to the array index.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return an array with the overall response rates up to and including an
	 *         iteration.
	 */
	public double[] responseRateTotal(Set<? extends SampledVertex> vertices) {
		int[] sampled = numVerticesSampledTotal(vertices);
		int[] detected = numVerticesDetectedTotal(vertices);
		
		double[] rates = new double[sampled.length];
		
		rates[0] = 1.0;
		for(int i = 1; i < sampled.length; i++) {
			rates[i] = sampled[i]/(double)detected[i - 1];
		}
		
		return rates;		
	}
	
	public double[] responseRatePerIteration(Set<? extends SampledVertex> vertices) {
		int[] sampled = numVerticesSampled(vertices);
		int[] detected = numVerticesDetected(vertices);
		
		double[] rates = new double[sampled.length];
		
		rates[0] = 1.0;
		for(int i = 1; i < sampled.length; i++) {
			rates[i] = sampled[i]/(double)detected[i - 1];
		}
		
		return rates;
	}
	/**
	 * Returns the highest (last) iteration value of <tt>vertices</tt>.
	 * 
	 * @param vertices
	 *            a set of sampled vertices.
	 * @return the highest (last) iteration value of <tt>vertices</tt>.
	 */
	public int lastIteration(Set<? extends SampledVertex> vertices) {
		int it = -1;
		for (SampledVertex v : vertices) {
			if (v.isSampled()) {
				it = Math.max(it, v.getIterationSampled());
			}
		}

		return it;
	}
}

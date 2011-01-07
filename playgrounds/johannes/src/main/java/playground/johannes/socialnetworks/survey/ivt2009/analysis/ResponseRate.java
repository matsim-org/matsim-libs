/* *********************************************************************** *
 * project: org.matsim.*
 * ResponseRate.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TIntIntHashMap;

import java.util.Set;

import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class ResponseRate {

	public static double responseRate(Set<? extends SampledVertex> vertices) {
		double[] rates = responseRatesAccumulated(vertices); 
		return rates[rates.length - 1];
	}
	
	public static double[] responseRatesAccumulated(Set<? extends SampledVertex> vertices) {
		TIntIntHashMap sampled = new TIntIntHashMap();
		TIntIntHashMap detected = new TIntIntHashMap();
		int maxIt = 0;
		
		for(SampledVertex vertex : vertices) {
			if(vertex.isDetected()) {
				detected.adjustOrPutValue(vertex.getIterationDetected(), 1, 1);
			}
			if(vertex.isSampled()) {
				sampled.adjustOrPutValue(vertex.getIterationSampled(), 1, 1);
				maxIt = Math.max(maxIt, vertex.getIterationSampled());
			}
		}
		
		TIntIntHashMap sampledSum = new TIntIntHashMap();
		TIntIntHashMap detectedSum = new TIntIntHashMap();
		for(int i = 0; i <= maxIt; i++) {
			sampledSum.put(i, sampledSum.get(i-1) + sampled.get(i));
			detectedSum.put(i, detectedSum.get(i-1) + detected.get(i));
		}
		
		double[] rates = new double[maxIt+1];
		
		rates[0] = 1;
		for(int i = 1; i <= maxIt; i++) {
			rates[i] = sampledSum.get(i)/(double)detectedSum.get(i-1);
		}
		
		return rates;
	}
}

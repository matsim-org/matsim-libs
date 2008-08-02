/* *********************************************************************** *
 * project: org.matsim.*
 * Degree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author illenberger
 *
 */
public class Degree extends GraphStatistic {

	private boolean biasCorrection;
	
	private double gamma;
	
	public Degree(String outputDir) {
		super(outputDir);
	}

	public void setBiasCorrection(boolean flag) {
		biasCorrection = flag;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		TDoubleArrayList values = new TDoubleArrayList(g.numVertices());
		TDoubleArrayList weights = new TDoubleArrayList(g.numVertices());
		TDoubleArrayList normWeights = new TDoubleArrayList(g.numVertices());
		
		
		if(g instanceof SampledGraph) {
			Set<SampledVertex> vertices = g.getVertices();
			
			double wsum = 0.0;
			for(SampledVertex v : vertices) {
				if(!v.isAnonymous()) {
					values.add(v.degree());
					if(biasCorrection) {
						weights.add(1 / v.getSampleProbability());
						wsum += 1 / v.getSampleProbability();
					} else {
						weights.add(1.0);
						wsum++;
					}
				}
			}
			double k = values.size() / wsum;
			for(int i = 0; i < weights.size(); i++) {
				normWeights.add(weights.getQuick(i) * k);
				stats.addValue(values.getQuick(i) * normWeights.getQuick(i));
			}
				
		} else {
			Set<Vertex> vertices = g.getVertices();
			for (Vertex v : vertices) {
				stats.addValue(v.degree());
				values.add(v.degree());
				weights.add(1.0);
			}
		}
		
		gamma = calcGammaExponent(values.toNativeArray(), weights.toNativeArray(), 1.0, 0);
		
		dumpStatistics(getStatisticsMap(stats), iteration);
		
		
		if(reference != null) {
			Histogram hist = new Histogram(1.0, reference.getMin(), reference.getMax());
			plotHistogram(values.toNativeArray(), weights.toNativeArray(), hist, iteration);
		} else {
			plotHistogram(values.toNativeArray(), weights.toNativeArray(), new Histogram(1.0), iteration);
		}
		
		return stats;
	}

	@Override
	protected List<String> getStatisticsKeys() {
		List<String> keys = super.getStatisticsKeys();
		keys.add("gamma");
		return keys;
	}

	@Override
	protected TObjectDoubleHashMap<String> getStatisticsMap(
			DescriptiveStatistics stats) {
		TObjectDoubleHashMap<String> statsMap = super.getStatisticsMap(stats);
		statsMap.put("gamma", gamma);
		return statsMap;
	}
}

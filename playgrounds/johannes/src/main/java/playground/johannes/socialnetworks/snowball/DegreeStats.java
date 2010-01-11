/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeStats.java
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
package playground.johannes.socialnetworks.snowball;

import gnu.trove.TObjectDoubleHashMap;

import java.util.List;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.statistics.PowerLawFit;

/**
 * @author illenberger
 *
 */
public class DegreeStats extends GraphPropertyEstimator {

	public DegreeStats(String outputDir) {
		super(outputDir);
		openStatsWriters("degree");
	}
	
	@Override
	protected List<String> getStatisticsKeys() {
		List<String> keys = super.getStatisticsKeys();
		if(keys.size() <= 6) { // FIXME!!!
			keys.add("gamma");
			keys.add("xmin");
		}
		return keys;
	}

	@Override
	public DescriptiveStatistics calculate(GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph, int iteration) {
		DescriptiveStatistics observed = new DescriptiveStatistics();
		Frequency observedDistr = new Frequency();
		
		Distribution estimated = new Distribution();

		for(VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if(!v.getDelegate().isAnonymous()) {
				estimated.add(v.getEdges().size(), v.getDelegate().getNormalizedWeight());
				
				observed.addValue(v.getEdges().size());
				observedDistr.addValue(v.getEdges().size());
				
			}
		}
		
		TObjectDoubleHashMap<String> statsMap = getStatisticsMap(observed);
		Tuple<Double, Double> plfit = PowerLawFit.fit(observed.getValues());
		statsMap.put("xmin", plfit.getFirst());
		statsMap.put("gamma", plfit.getSecond());
		dumpObservedStatistics(statsMap, iteration);
		
		statsMap = getStatisticsMap(estimated);
		plfit = PowerLawFit.fit(estimated.getValues(), estimated.getWeights());
		statsMap.put("xmin", plfit.getFirst());
		statsMap.put("gamma", plfit.getSecond());
		dumpEstimatedStatistics(statsMap, iteration);
		
		dumpFrequency(observedDistr, iteration, "observed");
		dumpFrequency(estimated, iteration, "estimated");
		
		return observed;
	}

}

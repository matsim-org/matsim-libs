/* *********************************************************************** *
 * project: org.matsim.*
 * Betweenness.java
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

import java.io.IOException;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import edu.uci.ics.jung.graph.Graph;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author illenberger
 *
 */
public class Betweenness extends GraphStatistic {

	private Centrality centrality;
	
	private double gamma;
	
	private double wMean;
	
	public Betweenness(Centrality centrality, String outputDir) {
		super(outputDir);
		this.centrality = centrality;
	}

	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration,
			DescriptiveStatistics reference) {
		centrality.run(g, iteration);
		
		gamma = calcGammaExponent(centrality.betweennessValues.getValues(), Double.NEGATIVE_INFINITY);
		wMean = centrality.getBetweennessWeighted();
		dumpStatistics(getStatisticsMap(centrality.betweennessValues), iteration);
		
		if(reference != null) {
			try {
				centrality.getBetweennessHistogram(reference.getMin(), reference.getMax()).
					plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				centrality.getBetweennessHistogram().
					plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return centrality.betweennessValues;
	}
	
	@Override
	protected List<String> getStatisticsKeys() {
		List<String> keys = super.getStatisticsKeys();
		keys.add("gamma");
		keys.add("wMean");
		return keys;
	}

	@Override
	protected TObjectDoubleHashMap<String> getStatisticsMap(
			DescriptiveStatistics stats) {
		TObjectDoubleHashMap<String> statsMap = super.getStatisticsMap(stats);
		statsMap.put("gamma", gamma);
		statsMap.put("wMean", wMean);
		return statsMap;
	}

}

/* *********************************************************************** *
 * project: org.matsim.*
 * Closeness.java
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

import playground.johannes.snowball.Histogram;

import edu.uci.ics.jung.graph.Graph;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author illenberger
 *
 */
public class Closeness extends GraphStatistic {

	private Centrality centrality;
	
	public Closeness(Centrality centrality, String outputDir) {
		super(outputDir);
		this.centrality = centrality;
	}
	
	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration,
			DescriptiveStatistics reference) {
		centrality.run(g, iteration);
		
		dumpStatistics(getStatisticsMap(centrality.closenessValues), iteration);
		if(reference != null) {
			try {
				Histogram hist = centrality.getClosenessHistogram(reference.getMin(), reference.getMax()); 
				hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
				hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Histogram hist = centrality.getClosenessHistogram(); 
				hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
				hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		centrality.dumpDegreeCorrelation(String.format("%1$s/%2$s.degreeCloseness.txt", outputDir, iteration), "closeness");
		
		return centrality.closenessValues;
	}

	@Override
	protected List<String> getStatisticsKeys() {
		List<String> keys = super.getStatisticsKeys();
		keys.add("wMean");
		return keys;
	}

	@Override
	protected TObjectDoubleHashMap<String> getStatisticsMap(
			DescriptiveStatistics stats) {
		TObjectDoubleHashMap<String> statsMap = super.getStatisticsMap(stats);
		statsMap.put("wMean", centrality.getClosenessWeighted());
		return statsMap;
	}
}

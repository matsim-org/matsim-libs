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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.Histogram;
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
	
	private double normMean;
	
	private double normWMean;
	
	private double gammaNorm;
	
	private double gammaWNorm;
	
	public Betweenness(Centrality centrality, String outputDir) {
		super(outputDir);
		this.centrality = centrality;
	}

	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration,
			DescriptiveStatistics reference) {
		centrality.run(g, iteration);
		
		gamma = calcGammaExponent(centrality.betweennessValues.getValues(), 1, 1.0);
		wMean = centrality.getBetweennessWeighted();
		
		double norm = (g.numVertices() - 1) * (g.numVertices() - 2);
		normMean = centrality.getGraphBetweenness() / norm;
		normWMean = centrality.getBetweennessWeighted() / norm;
		double[] normValues = getNormalizedValues(centrality.betweennessValues.getValues(), g);
		gammaNorm = calcGammaExponent(normValues, 0.000001, 0);
		double[] normWValues = getNormalizedValues(centrality.betweennessWValues.toNativeArray(), g);
		gammaWNorm = calcGammaExponent(normWValues, centrality.betweennessWeights.toNativeArray(), 0.000001, 0);
		
		dumpStatistics(getStatisticsMap(centrality.betweennessValues), iteration);
		
		if(reference != null) {
			try {
				Histogram hist = centrality.getBetweennessHistogram(reference.getMin(), reference.getMax()); 
				hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
				hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Histogram hist = centrality.getBetweennessHistogram(); 
				hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
				hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		dumpValues(centrality.betweennessValues.getSortedValues(), iteration, "plain");
		double[] wvalues = centrality.betweennessWValues.toNativeArray();
		Arrays.sort(wvalues);
		dumpValues(wvalues, iteration, "weighted");
		Arrays.sort(normValues);
		dumpValues(normValues, iteration, "norm");
		Arrays.sort(normWValues);
		dumpValues(normWValues, iteration, "normW");
		
		centrality.dumpDegreeCorrelation(String.format("%1$s/%2$s.degreeBetweenness.txt", outputDir, iteration), "betweenness");
		return centrality.betweennessValues;
	}
	
	private void dumpValues(double[] values, int iteration, String name) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.%3$s.txt", outputDir, iteration, name));
			for(double d : values) {
				writer.write(String.valueOf(d));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	
	private double[] getNormalizedValues(double[] stats, Graph g) {
		double norm = (g.numVertices() - 1) * (g.numVertices() - 2);
		double[] statsNorm = new double[stats.length];
		for(int i = 0; i < stats.length; i++) {
			statsNorm[i] = stats[i] / norm;
		}
		return statsNorm;
	}
	
	@Override
	protected List<String> getStatisticsKeys() {
		List<String> keys = super.getStatisticsKeys();
		keys.add("gamma");
		keys.add("wMean");
		keys.add("normMean");
		keys.add("normWMean");
		keys.add("gammaNorm");
		keys.add("gammaWNorm");
		return keys;
	}

	@Override
	protected TObjectDoubleHashMap<String> getStatisticsMap(
			DescriptiveStatistics stats) {
		TObjectDoubleHashMap<String> statsMap = super.getStatisticsMap(stats);
		statsMap.put("gamma", gamma);
		statsMap.put("wMean", wMean);
		statsMap.put("normMean", normMean);
		statsMap.put("normWMean", normWMean);
		statsMap.put("gammaNorm", gammaNorm);
		statsMap.put("gammaWNorm", gammaWNorm);
		return statsMap;
	}

}

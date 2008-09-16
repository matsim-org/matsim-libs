/* *********************************************************************** *
 * project: org.matsim.*
 * Clustering.java
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.statistics.GraphStatistics;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author illenberger
 *
 */
public class Clustering extends GraphStatistic {

	private double wMean;
	
	public Clustering(String outputDir) {
		super(outputDir);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference) {
		Map<Vertex, Double> values = GraphStatistics.clusteringCoefficients(g);
		DescriptiveStatistics stats = new DescriptiveStatistics();

		TIntDoubleHashMap degreeClustering = new TIntDoubleHashMap();
		TIntIntHashMap numDegree = new TIntIntHashMap();
		
		double sum = 0;
		double wsum = 0;
		
		if (g instanceof SampledGraph) {
			for (Vertex v : values.keySet()) {
				int k = v.degree();
				
				if (!((SampledVertex) v).isAnonymous()) {
					double cc = degreeClustering.get(k);
					
					
					
					if (v.degree() == 1) {
						stats.addValue(0.0);
//						sum += (cc / ((SampledVertex)v).getSampleProbability());
					} else {
						double C = values.get(v);
						stats.addValue(C);
						cc += C; 
						sum += (C / ((SampledVertex)v).getSampleProbability());
					}
					degreeClustering.put(k, cc);
					numDegree.put(k, numDegree.get(k) + 1);
					
					wsum += (1 / ((SampledVertex)v).getSampleProbability());
				}
			}
		} else {
			for (Vertex v : values.keySet()) {
				int k = v.degree();
				double cc = degreeClustering.get(k);
				
				
				wsum ++;
				
				if (v.degree() == 1)
					stats.addValue(0.0);
				else {
					double C = values.get(v);
					stats.addValue(C);
					cc += C;
					sum += C;
				}
				
				degreeClustering.put(k, cc);
				numDegree.put(k, numDegree.get(k) + 1);
			}
		}

		wMean = sum / wsum;
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.degreeDependency.txt", outputDir, iteration));
			int[] keys = numDegree.keys();
			Arrays.sort(keys);
			for (int k : keys) {
				double bc = degreeClustering.get(k);
				int numV = numDegree.get(k);

				writer.write(String.valueOf(k));
				writer.write("\t");
				writer.write(String.valueOf(bc / (double) numV));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		dumpStatistics(getStatisticsMap(stats), iteration);
		
		if(reference != null) {
			Histogram hist = new Histogram(100, reference.getMin(), reference.getMax());
			plotHistogram(stats.getValues(), hist, iteration);
		} else {
			plotHistogram(stats.getValues(), new Histogram(100), iteration);
		}
		
		return stats;
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
		statsMap.put("wMean", wMean);
		return statsMap;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * Mutuality.java
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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.socialnetworks.graph.GraphProjection;
import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 * 
 */
public class MutualityStats extends GraphPropertyEstimator {

	private double responseRate;
	
	public MutualityStats(String outputDir, double responseRate) {
		super(outputDir);
		this.responseRate = responseRate;
		openStatsWriters("mutuality");
	}

	@Override
	public DescriptiveStatistics calculate(
			GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph,
			int iteration) {
		int len2Paths = 0;
		int nVertex2Steps = 0;
		double len2PathsW = 0;
		double nVertex2StepsW = 0;

		TIntObjectHashMap<TDoubleArrayList> degreeMCorrelation = new TIntObjectHashMap<TDoubleArrayList>();
		for (VertexDecorator<SampledVertex> v : graph.getVertices()) {
			if (!v.getDelegate().isAnonymous()) {
				List<? extends Vertex> n1List = v.getNeighbours();
				Set<Vertex> n2Set = new HashSet<Vertex>();
				int numPaths = 0;
				for (Vertex n1 : n1List) {
					for (Vertex n2 : n1.getNeighbours()) {
						if (n2 != v && !n1List.contains(n2)) {
							n2Set.add(n2);
							numPaths++;
						}
					}
				}
				len2Paths += numPaths;
				nVertex2Steps += n2Set.size();

//				double responseRate = 1 - (numNonResponse / (double) v.getNeighbours().size());
//				double p_n2 = v.getDelegate().getSampleProbability();
//				double p_sigma2 = responseRate + p_n2 - responseRate * p_n2;
//				double correction = v.getDelegate().getNormalizedWeight() / (p);
				len2PathsW += numPaths * v.getDelegate().getNormalizedWeight();
				nVertex2StepsW += n2Set.size()	* v.getDelegate().getNormalizedWeight();
//				len2PathsW += numPaths / p_sigma2 * v.getDelegate().getNormalizedWeight();
//				nVertex2StepsW += n2Set.size() / p_n2 * v.getDelegate().getNormalizedWeight();
				
				TDoubleArrayList vals = degreeMCorrelation.get(v.getEdges().size());
				if(vals == null)
					vals = new TDoubleArrayList();
				vals.add(n2Set.size()/(double)numPaths);
				degreeMCorrelation.put(v.getEdges().size(), vals);
			}
		}

		DescriptiveStatistics observed = new DescriptiveStatistics();
		DescriptiveStatistics estimated = new DescriptiveStatistics();
		observed.addValue(nVertex2Steps / (double) len2Paths);
		estimated.addValue(nVertex2StepsW / len2PathsW);
//		double m = nVertex2StepsW / (double) len2PathsW;
//		double p_n2 = 1 - Math.pow(1 - responseRate, 1 / m);
////		double p_sigma2 = responseRate + p_n2 - responseRate * p_n2;
//		double p_sigma2 = 2 * responseRate - Math.pow(responseRate, 2);
//		estimated.addValue(m * p_sigma2 / p_n2);

		dumpObservedStatistics(getStatisticsMap(observed), iteration);
		dumpEstimatedStatistics(getStatisticsMap(estimated), iteration);

		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/"+iteration+".m-k-correlation.txt");
			for(int k : degreeMCorrelation.keys()) {
				writer.write(String.valueOf(k));
				writer.write(TAB);
				double sum = 0;
				TDoubleArrayList vals = degreeMCorrelation.get(k);
				for(int i = 0; i< vals.size(); i++) {
					sum+=vals.get(i);
				}
				writer.write(String.valueOf(sum/(double)vals.size()));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return observed;
	}
}

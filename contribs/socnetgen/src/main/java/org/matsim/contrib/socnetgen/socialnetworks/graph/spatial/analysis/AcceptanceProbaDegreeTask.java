/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbaDegreeTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.socialnetworks.graph.spatial.analysis;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.socnetgen.socialnetworks.graph.analysis.AttributePartition;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis.ObservedAcceptanceProbability;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class AcceptanceProbaDegreeTask extends AnalyzerTask {

	private Set<Point> destinations;
	
	public void setDestinations(Set<Point> destinations) {
		this.destinations = destinations;
	}
	
	/* (non-Javadoc)
	 * @see AnalyzerTask#analyze(Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<Vertex> normValues = ObservedDegree.getInstance().values(graph.getVertices());
		
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(normValues.getValues(), 1, 2));
		TDoubleObjectHashMap<?> partitions = partitioner.partition(normValues);
		TDoubleObjectIterator<?> it = partitions.iterator();
		
		AcceptanceProbability proba = new ObservedAcceptanceProbability();
		
		Map<String, TDoubleDoubleHashMap> histograms = new HashMap<String, TDoubleDoubleHashMap>();
		Map<String, DescriptiveStatistics> distributions = new HashMap<String, DescriptiveStatistics>();
		double sum = 0;
		
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			double key = it.key();
			Set<SpatialVertex> partition = (Set<SpatialVertex>) it.value();
			
			System.out.println("Partition size = " + partition.size() + "; key = " + key);
			
			DescriptiveStatistics distr = proba.distribution(partition, destinations);
			
			double[] values = distr.getValues();
			
			System.out.println("Num samples = " + values.length);
			
			if(values.length > 0) {
				TDoubleDoubleHashMap hist = Histogram.createHistogram(distr, FixedSampleSizeDiscretizer.create(values, 1, 50), true);
				sum += Histogram.sum(hist);
				histograms.put(String.format("p_accept-k%1$.4f", key), hist);
				distributions.put(String.format("p_accept-k%1$.4f", key), distr);
			}
		}

		for(Entry<String, TDoubleDoubleHashMap> entry : histograms.entrySet()) {
			String key = entry.getKey();
			TDoubleDoubleHashMap histogram = entry.getValue();
			Histogram.normalize(histogram, sum);
			try {
				StatsWriter.writeHistogram(histogram, "d", "p", String.format("%1$s/%2$s.txt", getOutputDirectory(), key));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			histogram = Histogram.createCumulativeHistogram(histogram);
			Histogram.complementary(histogram);
			try {
				StatsWriter.writeHistogram(histogram, "d", "p", String.format("%1$s/%2$s.cum.txt", getOutputDirectory(), key));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			DescriptiveStatistics stats = distributions.get(key);
			writeRawData(stats, key);
		}
	}

}

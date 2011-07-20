/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptancePropaCategoryTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinLogDiscretizer;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAcceptanceProbability;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class AcceptancePropaCategoryTask extends ModuleAnalyzerTask<Accessibility> {

	private Set<Point> destinations;

	private Geometry boundary;

	public AcceptancePropaCategoryTask(Accessibility module) {
		this.setModule(module);
	}
	
	public void setDestinations(Set<Point> destinations) {
		this.destinations = destinations;
	}

	public void setBoundary(Geometry boundary) {
		this.boundary = boundary;
	}

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		Accessibility access = module;
//		access.setTargets(destinations);

		destinations = new HashSet<Point>();
		Set<Vertex> vertices = new HashSet<Vertex>();
		for (Vertex v : graph.getVertices()) {
			Point p = ((SpatialVertex) v).getPoint();
			if (p != null) {
//				if (boundary.contains(p)) {
					vertices.add(v);
					destinations.add(((SpatialVertex) v).getPoint());
//				}
			}
		}
		access.setTargets(destinations);
		
		TObjectDoubleHashMap<Vertex> normValues = access.values(vertices);
//		TObjectDoubleHashMap<Vertex> normValues = ObservedDegree.getInstance().values(vertices);
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(normValues.getValues(), 10, 2));
		TDoubleObjectHashMap<?> partitions = partitioner.partition(normValues);
		TDoubleObjectIterator<?> it = partitions.iterator();

//		AcceptanceProbability propa = new ObservedAcceptanceProbability();
		AcceptanceProbability propa = new AcceptanceProbability();

		Map<String, TDoubleDoubleHashMap> histograms = new HashMap<String, TDoubleDoubleHashMap>();
		double sum = 0;
		
		for (int i = 0; i < partitions.size(); i++) {
			it.advance();
			double key = it.key();
			Set<SpatialVertex> partition = (Set<SpatialVertex>) it.value();
			System.out.println("Partition size = " + partition.size());
			DescriptiveStatistics distr = propa.distribution(partition, destinations);

			try {
				double[] values = distr.getValues();
				
					TDoubleDoubleHashMap hist = Histogram.createHistogram(distr, FixedSampleSizeDiscretizer.create(values, 200, 50), true);
//					Histogram.normalize(hist);
//					TXTWriter.writeMap(hist, name, "p",
//							String.format("%1$s/%2$s.n%3$s.txt", getOutputDirectory(), name, values.length / bins));
				sum += Histogram.sum(hist);
				histograms.put(String.format("p_accept-cat%1$.1f", key), hist);
//				writeHistograms(distr, String.format("p_accept-cat%1$.1f", key), 500, 200);
				writeHistograms(distr, new LinLogDiscretizer(1000.0, 2), String.format("p_accept-cat%1$.1f.log", key),
						true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for(Entry<String, TDoubleDoubleHashMap> entry : histograms.entrySet()) {
			String key = entry.getKey();
			TDoubleDoubleHashMap histogram = entry.getValue();
			Histogram.normalize(histogram, sum);
			try {
				TXTWriter.writeMap(histogram, "d", "p", String.format("%1$s/%2$s.txt", getOutputDirectory(), key));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

/* *********************************************************************** *
 * project: org.matsim.*
 * EpsilonTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TDoubleIntIterator;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class EpsilonTask extends AnalyzerTask {

	private Set<Point> opportunities;
	
	private DistanceCalculator distCalculator = new CartesianDistanceCalculator();
	
	private Discretizer discretizer = new LinearDiscretizer(100.0);
	
	public EpsilonTask(Set<Point> opportunities) {
		this.opportunities = opportunities;
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) g; 
		/*
		 * calculate accessibility
		 */
		Accessibility access = new Accessibility(new GravityCostFunction(-1.4, 0));
		Set<? extends SocialSampledVertexDecorator<SocialSparseVertex>> egos = SnowballPartitions.createSampledPartition(graph.getVertices());
		TObjectDoubleHashMap<Vertex> accessValues = access.values(egos);
		/*
		 * create categories with low and high accessibility
		 */
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(accessValues.getValues(), 10, 2));
		TDoubleObjectHashMap<?> partitions = partitioner.partition(accessValues);
		TDoubleObjectIterator<?> it = partitions.iterator();
		/*
		 * for each category do...
		 */
		for(int catIdx = 0; catIdx < partitions.size(); catIdx++) {
			it.advance();
			Set<SpatialVertex> category = (Set<SpatialVertex>) it.value();
			double A_cat = it.key();
			
//			double sum_k = 0;
//			double sum_A = 0;
//			double sum_c = 0;
			TDoubleDoubleHashMap sum_model = new TDoubleDoubleHashMap();
			TDoubleIntHashMap m_i = new TDoubleIntHashMap();
			
			for(SpatialVertex v : category) {
//				sum_k += v.getNeighbours().size();
//				sum_A += accessValues.get(v);
//				sum_c += v.getNeighbours().size()/accessValues.get(v);
				int k_i = v.getNeighbours().size();
				double A_i = accessValues.get(v);
				
				TDoubleIntHashMap M_i = new TDoubleIntHashMap();
				/*
				 * get number of opportunities in distance
				 */
				for(Point w : opportunities) {
					double d = distCalculator.distance(v.getPoint(), w);
					d = discretizer.discretize(d);
					M_i.adjustOrPutValue(d, 1, 1);
				}
				/*
				 * get number of contacts in distance
				 */
				for(SpatialVertex w : v.getNeighbours()) {
					double d = distCalculator.distance(v.getPoint(), w.getPoint());
					d = discretizer.discretize(d);
					m_i.adjustOrPutValue(d, 1, 1);
				}
				/*
				 * calculate epsilon
				 */
				for(double d = discretizer.binWidth(0); d < 300000; d += discretizer.binWidth(d)) {
					int M_d = M_i.get(d);
					
					if(M_d > 0) {
						double model = k_i/A_i * Math.pow(d, -1.4) * M_d;
						sum_model.adjustOrPutValue(d, model, model);
					} else {
						System.err.println("No M_d.");
					}
				}
			}
			TDoubleDoubleHashMap epsilon_d = new TDoubleDoubleHashMap();
			
			for(double d = discretizer.binWidth(0); d < 300000; d += discretizer.binWidth(d)) {
				double espilon = m_i.get(d) - sum_model.get(d);
				epsilon_d.put(d, espilon);
			}
			
			/*
			 * dump histogram
			 */
			try {
				TXTWriter.writeMap(epsilon_d, "d", "epsilon", String.format("%1$s/epsilon.cat%2$s.txt", getOutputDirectory(), A_cat));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

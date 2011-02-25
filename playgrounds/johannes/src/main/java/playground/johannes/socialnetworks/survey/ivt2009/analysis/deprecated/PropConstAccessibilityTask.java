/* *********************************************************************** *
 * project: org.matsim.*
 * PropConstAccessibilityTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.math.DummyDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.AcceptPropConst2;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class PropConstAccessibilityTask extends ModuleAnalyzerTask<AcceptPropConst2> {

	private Set<Point> targets;
	
	public void setTargets(Set<Point> targets) {
		this.targets = targets;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		GravityCostFunction function = new GravityCostFunction(1.6, 0, new CartesianDistanceCalculator());
		function.setDiscretizer(new LinearDiscretizer(1.0));
		Accessibility accessibility = new Accessibility(function);
		accessibility.setTargets(targets);
		
		TObjectDoubleHashMap<Vertex> yVals = accessibility.values(SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)graph.getVertices()));
		
		AcceptPropConst2 acceptConst = new AcceptPropConst2(yVals);
		acceptConst.setTargets(targets);
		
		TObjectDoubleHashMap<Vertex> xVals = acceptConst.values(SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)graph.getVertices()));
		
//		TDoubleDoubleHashMap hist = Histogram.createHistogram(ObservedDegree.getInstance().values(graph.getVertices()).getValues(), new DummyDiscretizer(), false);
		AttributePartition partitioner = new AttributePartition(new DummyDiscretizer());
		TDoubleObjectHashMap<Set<Vertex>> partitions = partitioner.partition(ObservedDegree.getInstance().values(SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)graph.getVertices())));
		TDoubleObjectIterator<Set<Vertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			double k = it.key();
			Set<Vertex> set = it.value();
			
			TObjectDoubleHashMap<Vertex> xVals2 = new TObjectDoubleHashMap<Vertex>();
			TObjectDoubleHashMap<Vertex> yVals2 = new TObjectDoubleHashMap<Vertex>();
			
			for(Vertex v : set) {
				xVals2.put(v, xVals.get(v));
				yVals2.put(v, yVals.get(v));
			}
			
			TDoubleDoubleHashMap map = VertexPropertyCorrelation.mean(yVals2, xVals2, new DummyDiscretizer());//, FixedSampleSizeDiscretizer.create(yVals.getValues(), 50, 20));
			TDoubleDoubleIterator it2 = map.iterator();
			for(int i2 = 0; i2 < map.size(); i2++) {
				it2.advance();
				System.out.println("Ratio "+k+"=" + it2.value()/it2.key());
			}
			try {
				TXTWriter.writeMap(map, "c_i", "accessibility", getOutputDirectory() + "A_ci_k"+k+".txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		TDoubleDoubleHashMap map = VertexPropertyCorrelation.mean(yVals, xVals, new DummyDiscretizer());//, FixedSampleSizeDiscretizer.create(yVals.getValues(), 50, 20));
//		TDoubleDoubleIterator it = map.iterator();
//		for(int i = 0; i < map.size(); i++) {
//			it.advance();
//			System.out.println("Ratio=" + it.key()/it.value());
//		}
		try {
			TXTWriter.writeMap(map, "c_i", "accessibility", getOutputDirectory() + "A_ci.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

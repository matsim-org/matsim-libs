/* *********************************************************************** *
 * project: org.matsim.*
 * AccessibilityPartitioner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.FixedBordersDiscretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAcceptanceProbability;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class AccessibilityPartitioner extends ModuleAnalyzerTask<Accessibility> {

	private SpatialCostFunction costFunction;
	
	private Set<Point> opportunities;
	
	public AccessibilityPartitioner(SpatialCostFunction costFunction, Set<Point> opportunities) {
		this.costFunction = costFunction;
		this.opportunities = opportunities;
	}
	
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		SpatialGraph graph = (SpatialGraph) g;
		TObjectDoubleHashMap<SpatialVertex> values = module.values(graph.getVertices(), costFunction, opportunities);
		
		FixedBordersDiscretizer discretizer = new FixedBordersDiscretizer(new double[]{-10,5.5,10});
		AttributePartition partitioner = new AttributePartition(discretizer);
		
		TDoubleObjectHashMap<Set<SpatialVertex>> partitions = partitioner.partition(values);
		
		TDoubleObjectIterator<Set<SpatialVertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			System.out.println(it.key() +": size=" + it.value().size());
			
			ObservedAcceptanceProbability acc = new ObservedAcceptanceProbability();
			acc.setDistanceCalculator(new CartesianDistanceCalculator());
			DescriptivePiStatistics distr = acc.distribution((Set<? extends SpatialVertex>) it.value(), opportunities);
			try {
				TDoubleDoubleHashMap hist = Histogram.createHistogram(distr, FixedSampleSizeDiscretizer.create(distr.getValues(), 100));
				TXTWriter.writeMap(hist, "d", "p_accept", getOutputDirectory() + "acc_a="+it.key()+".txt");
//				writeHistograms(distr, 1, false, "acc_a="+it.key());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	

}

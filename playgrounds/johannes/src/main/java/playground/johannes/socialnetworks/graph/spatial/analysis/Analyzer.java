/* *********************************************************************** *
 * project: org.matsim.*
 * Analyzer.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.BeelineCostFunction;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph("/Volumes/hertz.math.tu-berlin.de/net/ils/jillenberger/socialnets/mcmc/runs/run230/output/4000000000/graph.graphml");

		String output = "/Users/jillenberger/Work/work/socialnets/mcmc/output/";
		BeelineCostFunction func = new BeelineCostFunction();
		func.setDistanceCalculator(new CartesianDistanceCalculator());
//		AnalyzerTask task = new AccessabilityTask(func);
//		task.setOutputDirectoy("");
//		GraphAnalyzer.analyze(graph, task);
		
		Set<Point> opportunities = new java.util.HashSet<Point>();
		for(SpatialVertex v : graph.getVertices()) {
			opportunities.add(v.getPoint());
		}
		
		Accessibility access = new Accessibility();
		TObjectDoubleHashMap<SpatialVertex> values = access.values(graph.getVertices(), func, opportunities);
		
		AttributePartition partition = new AttributePartition(new LinearDiscretizer(50));
		TDoubleObjectHashMap<Set<SpatialVertex>> partitions = partition.partition(values);
		TDoubleObjectIterator<Set<SpatialVertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			new File(output + "part." + it.key()).mkdirs();
			Distance distance = new Distance();
			Distribution.writeHistogram(distance.distribution(it.value()).absoluteDistributionLog2(1000), output + "part." + it.key() + "/d.txt");
			
			AcceptanceProbability accept = new AcceptanceProbability();
			DescriptiveStatistics distr = accept.distribution(it.value(), opportunities);
			TDoubleDoubleHashMap hist = Histogram.createHistogram(distr, new LinearDiscretizer(1000.0), false);
			TXTWriter.writeMap(hist, "d", "p", String.format("%1$s/%2$s/p_accept.txt", it.key() , output));
//			Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), output + "part." + it.key() + "/p_accept.log2.txt");
//			Distribution.writeHistogram(distr.absoluteDistribution(1000), output + "part." + it.key() + "/p_accept.txt");
			
//			EdgeCosts costs = new ObservedEdgeCosts(new GravityEdgeCostFunction(1.0, 1.0));
//			double c_mean = costs.vertexCostsSum(it.value()).mean();
//			System.out.println(it.key() + " = " + c_mean);
		}
	}

}

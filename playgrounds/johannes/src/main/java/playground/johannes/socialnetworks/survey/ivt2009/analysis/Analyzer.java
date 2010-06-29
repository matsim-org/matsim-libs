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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.opengis.referencing.FactoryException;

import playground.johannes.socialnetworks.gis.BeelineCostFunction;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbability;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeCosts;
import playground.johannes.socialnetworks.graph.spatial.analysis.GraphClippingFilter;
import playground.johannes.socialnetworks.graph.spatial.generators.GravityEdgeCostFunction;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedDistance;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedEdgeCosts;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class Analyzer {

	public static void main(String[] args) throws IOException, FactoryException {
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = reader.readGraph(args[0]);
		
		ZoneLayer zones = ZoneLayerSHP.read("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/zones/Zones.shp");
		zones.overwriteCRS(CRSUtils.getCRS(21781));
		
		Set<Point> choiceSet = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		graph2.transformToCRS(CRSUtils.getCRS(4326));
		for(SpatialVertex v : graph2.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
		/*
		 * analyze the complete graph
		 */
		String output = args[1];
//		analyze(graph, zones, choiceSet, output);
		/*
		 * analyze the swiss clipping
		 */
		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		geometry.setSRID(21781);
		GraphFilter<SpatialGraph> filter = new GraphClippingFilter(new SocialSparseGraphBuilder(graph.getDelegate().getCoordinateReferenceSysten()), geometry);
		filter.apply(graph.getDelegate());
		SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		builder.synchronize(graph);
		
		output = output+"/clip/";
		new File(output).mkdirs();
//		analyze(graph, zones, choiceSet, output);
		/*
		 * 
		 */
		ObservedAccessability obsAccess = new ObservedAccessability();
//		TObjectDoubleHashMap<SpatialVertex> values = obsAccess.values((Set<? extends SpatialVertex>) graph.getVertices(), new GravityCostFunction(1.6, 1.0), choiceSet);
		TObjectDoubleHashMap<SpatialVertex> values = obsAccess.values((Set<? extends SpatialVertex>) graph.getVertices(), new BeelineCostFunction(), choiceSet);
		
//		AttributePartition partition = new AttributePartition(new FixedSampleSizeDiscretizer(values.getValues(), 200));
		AttributePartition partition = new AttributePartition(new LinearDiscretizer(700000));
		TDoubleObjectHashMap<Set<SpatialVertex>> partitions = partition.partition(values);
		TDoubleObjectIterator<Set<SpatialVertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			new File(output + "part." + it.key()).mkdirs();
			ObservedDistance distance = new ObservedDistance();
			Distribution.writeHistogram(distance.distribution(it.value()).absoluteDistributionLog2(1000), output + "part." + it.key() + "/d.txt");
			
			AcceptanceProbability accept = new ObservedAcceptanceProbability();
			Distribution.writeHistogram(accept.distribution(it.value(), choiceSet).absoluteDistributionLog2(1000), output + "part." + it.key() + "/p_accept.log2.txt");
			Distribution.writeHistogram(accept.distribution(it.value(), choiceSet).absoluteDistribution(1000), output + "part." + it.key() + "/p_accept.txt");
			
			EdgeCosts costs = new ObservedEdgeCosts(new GravityEdgeCostFunction(1.0, 1.0));
			double c_mean = costs.vertexCostsSum(it.value()).mean();
			System.out.println(it.key() + " = " + c_mean);
		}
		
	}

	private static void analyze(SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph, ZoneLayer zones, Set<Point> choiceSet, String output) {
		ObservedAnalyzerTask task = new ObservedAnalyzerTask(zones, choiceSet);
		task.setOutputDirectoy(output);
		
		try {
			GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, task), output + "/stats.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

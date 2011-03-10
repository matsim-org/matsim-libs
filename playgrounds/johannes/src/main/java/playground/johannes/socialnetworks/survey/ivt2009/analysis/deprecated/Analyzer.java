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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.FactoryException;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialFilter;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.EstimatedAnalyzerTask;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAnalyzerTask;
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
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(); 
		reader.setGraphProjectionBuilder(builder);
		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) reader.readGraph(args[0]);
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
//		netReader.readFile("/Users/jillenberger/Work/shared-svn/studies/countries/ch/data/network/osm20100831/network.xml.gz");
		
		ZoneLayer zones = ZoneLayerSHP.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/Zones.shp");
		zones.overwriteCRS(CRSUtils.getCRS(21781));
		
		Set<Point> choiceSet = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
//		graph2.transformToCRS(CRSUtils.getCRS(4326));
		for(SpatialVertex v : graph2.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
		/*
		 * analyze the complete graph
		 */
		String output = args[1];
		analyze(graph, zones, choiceSet, scenario.getNetwork(), output, geometry);
		/*
		 * analyze the swiss clipping
		 */
//		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
//		geometry.setSRID(21781);
//		GraphFilter<SpatialGraph> filter = new GraphClippingFilter(new SocialSparseGraphBuilder(graph.getDelegate().getCoordinateReferenceSysten()), geometry);
//		filter.apply(graph.getDelegate());
////		SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
//		builder.synchronize(graph);
		
//		output = output+"/clip/";
//		new File(output).mkdirs();
//		analyze(graph, zones, choiceSet, scenario.getNetwork(), output);
		
//		FrequencyFilter fFilter = new FrequencyFilter((GraphBuilder<? extends SocialGraph, ? extends SocialVertex, ? extends SocialEdge>) builder, 11.0);
//		fFilter.apply(graph);
//		builder.synchronize(graph);
//		
//		output = output+"/clip/f2f/";
//		new File(output).mkdirs();
//		analyze(graph, zones, choiceSet, scenario.getNetwork(), output);
		
//		RemoveOrpahnedComponents rm = new RemoveOrpahnedComponents();
//		rm.setBuilder(builder);
//		rm.apply(graph);
//		builder.synchronize(graph);
//		
//		output = args[1] + "/cleaned/";
//		new File(output).mkdirs();
//		analyze(graph, zones, choiceSet, scenario.getNetwork(), output);
		
	}

	private static void analyze(SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph, ZoneLayer zones, Set<Point> choiceSet, Network network, String output, Geometry boundary) {
		ObservedAnalyzerTask task = new ObservedAnalyzerTask(zones, choiceSet, network, boundary);
		task.setOutputDirectoy(output);
		
		try {
			GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, task), output + "/stats.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		EstimatedAnalyzerTask estimTask = new EstimatedAnalyzerTask(graph);
		output = output + "/estim";
		new File(output).mkdirs();
		estimTask.setOutputDirectoy(output);
		try {
			GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, estimTask), output + "/stats.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}

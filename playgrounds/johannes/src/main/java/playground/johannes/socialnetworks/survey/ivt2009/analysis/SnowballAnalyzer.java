/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballAnalyzerTask.java
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskArray;
import playground.johannes.socialnetworks.graph.analysis.FilteredAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialFilter;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SnowballAnalyzer {

	private static final String MODULE_NAME = "analyzer";
	
	private SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph;
	
	private SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
	
	private AnalyzerTask obsTask;
	
	private AnalyzerTask estimTask;
	
	private Geometry chBorder;
	
	public static void main(String args[]) {
		Config config = new Config();
		ConfigReaderMatsimV1 reader = new ConfigReaderMatsimV1(config);
		reader.readFile(args[0]);
		
		new SnowballAnalyzer(config);
	}
	
	public SnowballAnalyzer(Config config) {
		try {
		graph = GraphReaderFacade.read(config.getParam(MODULE_NAME, "graphfile"));
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
//		netReader.readFile(config.getParam(MODULE_NAME, "networkfile"));
	
		ZoneLayer zones = ZoneLayerSHP.read(config.getParam(MODULE_NAME, "zonesfile"));
		zones.overwriteCRS(CRSUtils.getCRS(21781));
		
		Set<Point> choiceSet = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(config.getParam(MODULE_NAME, "plansfile"));
		
		Feature feature = FeatureSHP.readFeatures(config.getParam(MODULE_NAME, "chzone")).iterator().next();
		chBorder = feature.getDefaultGeometry();
		chBorder.setSRID(21781);
		
		String output = config.getParam(MODULE_NAME, "output");
//		graph2.transformToCRS(CRSUtils.getCRS(4326));
		for(SpatialVertex v : graph2.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
		
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
//		GraphFilter<SpatialGraph> filter = new GraphClippingFilter(new SocialSparseGraphBuilder(graph.getDelegate().getCoordinateReferenceSysten()), chBorder);
//		filter.apply(graph.getDelegate());
//		SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
//		builder.synchronize(graph);
		
		obsTask = new ObservedAnalyzerTask(zones, choiceSet, scenario.getNetwork(), chBorder);
		estimTask = new EstimatedAnalyzerTask(graph);
		
		analyze(graph, output);
		
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private AnalyzerTask createObsEstimTask() {
		AnalyzerTaskArray task = new AnalyzerTaskArray();
		task.addAnalyzerTask(obsTask, "obs");
		task.addAnalyzerTask(estimTask, "estim");
		return task;
	}
	
	private AnalyzerTask createIterationTask() {
		AnalyzerTask obsEstim = createObsEstimTask();
		
		FilteredAnalyzerTask task = new FilteredAnalyzerTask(obsEstim);
//		task.addFilter(new DefaultFilter(), "full");
		task.addFilter(new SpatialFilter((GraphBuilder) builder, chBorder), "ch");
		
		
		return task;
	}
	
	private void analyze(SampledGraph graph, String output) {
		try {
			
			
			int it = 0;
			for(SampledVertex vertex : graph.getVertices()) {
				if(vertex.isSampled())
					it = Math.max(it, vertex.getIterationSampled());
			}
			
			
			AnalyzerTask itTask = createIterationTask();
			FilteredAnalyzerTask task = new FilteredAnalyzerTask(itTask);
			task.addFilter(new DefaultFilter(), "plain");
			for(int i = 0; i <= it; i++) {
				SampledGraphFilter filter = new SampledGraphFilter(builder, i);
				task.addFilter(filter, String.format("it.%1$s", i));
			}
			task.setOutputDirectoy(output);
			
			GraphAnalyzer.analyze(graph, task, output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class DefaultFilter implements GraphFilter<Graph> {

		@Override
		public Graph apply(Graph graph) {
			return graph;
		}
		
	}
}

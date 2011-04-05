/* *********************************************************************** *
 * project: org.matsim.*
 * Analyzer.java
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
package playground.johannes.studies.mcmc;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskArray;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.ExtendedTopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.SocialAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.ExtendedSpatialAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialAnalyzerTask;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(args[0]);
	
		SocialSparseGraphMLReader graphReader = new SocialSparseGraphMLReader();
		SocialGraph graph = graphReader.readGraph(args[1], scenario.getPopulation());
		
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		
		AnalyzerTaskComposite topoTask = new AnalyzerTaskComposite();
		topoTask.addTask(new TopologyAnalyzerTask());
		topoTask.addTask(new ExtendedTopologyAnalyzerTask());
		array.addAnalyzerTask(topoTask, "topo");
		
		AnalyzerTaskComposite spatialTask = new AnalyzerTaskComposite();
		spatialTask.addTask(new SpatialAnalyzerTask());
		spatialTask.addTask(new ExtendedSpatialAnalyzerTask());
		
		array.addAnalyzerTask(spatialTask, "spatial");
		array.addAnalyzerTask(new SocialAnalyzerTask(), "social");
		
		GraphAnalyzer.analyze(graph, array, args[2]);

	}

}

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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetgen.sna.graph.analysis.*;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.SocialAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.ExtendedSpatialAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.SpatialAnalyzerTask;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

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

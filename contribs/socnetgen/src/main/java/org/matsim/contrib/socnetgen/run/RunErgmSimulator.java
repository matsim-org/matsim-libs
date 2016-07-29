/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmSimulator.java
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
package org.matsim.contrib.socnetgen.run;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTaskArray;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTaskComposite;
import org.matsim.contrib.socnetgen.sna.graph.analysis.TopologyAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.generators.BarabasiAlbertGenerator;
import org.matsim.contrib.socnetgen.sna.graph.generators.ErdosRenyiGenerator;
import org.matsim.contrib.socnetgen.sna.graph.generators.RandomGraphGenerator;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.mcmc.Ergm;
import org.matsim.contrib.socnetgen.sna.graph.mcmc.GibbsEdgeFlip;
import org.matsim.contrib.socnetgen.sna.graph.mcmc.SampleAnalyzer;
import org.matsim.contrib.socnetgen.sna.graph.social.*;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.SocialAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.social.generators.ErgmAge;
import org.matsim.contrib.socnetgen.sna.graph.social.generators.ErgmGender;
import org.matsim.contrib.socnetgen.sna.graph.social.io.SocialGraphMLWriter;
import org.matsim.contrib.socnetgen.sna.graph.social.io.SocialSparseVertexPool;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.SpatialAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.generators.ErgmLnDistance;
import org.matsim.contrib.socnetgen.sna.math.LogNormalDistribution;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class RunErgmSimulator {

	private static final String MODULE_NAME = "ergm";

	private static final Logger logger = Logger.getLogger(RunErgmSimulator.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		 * read config file
		 */
		Config config = new Config();
		ConfigReader creader = new ConfigReader(config);
		creader.readFile(args[0]);
		/*
		 * load population
		 */
		Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(config.findParam("plans", "inputPlansFile"));
		Population population = scenario.getPopulation();
		/*
		 * read parameters
		 */
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));;
		long iterations = (long)Double.parseDouble(config.getParam(MODULE_NAME, "iterations"));
		long logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		long sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		String output = config.getParam(MODULE_NAME, "output");
		
		double theta_distance = Double.parseDouble(config.getParam(MODULE_NAME, "theta_distance"));
		double theta_age = Double.parseDouble(config.getParam(MODULE_NAME, "theta_age"));
		double theta_gender = Double.parseDouble(config.getParam(MODULE_NAME, "theta_gender"));
//		double theta_triangles = Double.parseDouble(config.getParam(MODULE_NAME, "theta_triangles"));
		
		boolean conservePk = Boolean.parseBoolean(config.getParam(MODULE_NAME, "conservePk"));
		/*
		 * initialize graph
		 */
		logger.info("Initializing graph...");
		Set<Person> persons = new HashSet<Person>(population.getPersons().values());
		SocialSparseGraphFactory factory = new SocialSparseVertexPool(persons, CRSUtils.getCRS(21781));
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(factory);
		SocialSparseGraph graph = createGraph(config.getParam(MODULE_NAME, "graphtype"), persons, randomSeed, builder, config);
		/*
		 * initialize ergm
		 */
		logger.info("Initializing ergm...");
		AdjacencyMatrix<SocialSparseVertex> y = new AdjacencyMatrix<SocialSparseVertex>(graph, false);
		
		Ergm ergm = new Ergm();
//		ergm.addComponent(new EdgePowerLawDistance(y, theta_distance, 10 * persons.size()));
//		ErgmDensity eden = new ErgmDensity();
//		eden.setTheta(6.9);
//		ergm.addComponent(eden);
		ergm.addComponent(new ErgmLnDistance(y, theta_distance));
		ergm.addComponent(new ErgmAge(theta_age));
		ergm.addComponent(new ErgmGender(theta_gender));
//		ergm.addComponent(new ErgmTriangles(theta_triangles));
		/*
		 * initialize sampler
		 */
//		GibbsEdgeSwitch sampler = new GibbsEdgeSwitch(randomSeed);
		GibbsEdgeFlip sampler = new GibbsEdgeFlip(randomSeed);
//		GibbsEdgeInsert sampler = new GibbsEdgeInsert(randomSeed);
		
		SampleAnalyzer<SocialSparseGraph, SocialSparseEdge, SocialSparseVertex> analyzer = new SampleAnalyzer<SocialSparseGraph, SocialSparseEdge, SocialSparseVertex>(graph, builder, output);
		analyzer.setAnalysisInterval(sampleInterval);
		analyzer.setInfoInteraval(logInterval);
		analyzer.setMaxIteration(iterations);
		analyzer.setWriter(new SocialGraphMLWriter());
		/*
		 * initialize analyzer task
		 */
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		array.addAnalyzerTask(new TopologyAnalyzerTask(), "topo");
		AnalyzerTaskComposite spatialTask = new AnalyzerTaskComposite();
		spatialTask.addTask(new SpatialAnalyzerTask());
//		spatialTask.addTask(new ExtendedSpatialAnalyzerTask());
		array.addAnalyzerTask(spatialTask, "spatial");
		array.addAnalyzerTask(new SocialAnalyzerTask(), "social");
		
		analyzer.setAnalyzerTask(array);
		/*
		 * sample...
		 */
		if(conservePk)
			sampler.sample(y, ergm, analyzer);//, new ConserveDegreeDistribution());
		else
			sampler.sample(y, ergm, analyzer);
		logger.info("Done.");
	}

	public static SocialSparseGraph createGraph(String type, Set<Person> persons, long randomSeed, SocialSparseGraphBuilder builder, Config config) {
		SocialSparseGraph graph = null;
		if("ba".equalsIgnoreCase(type)) {
			BarabasiAlbertGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new BarabasiAlbertGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(builder);
			graph = generator.generate(5, 5, persons.size() - 5 , randomSeed);
			
		} else if("random".equalsIgnoreCase(type)) {
			ErdosRenyiGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new ErdosRenyiGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(builder);
			double p = 14.8/(double)(persons.size() - 1);
			generator.setRandomDrawMode(true);
			graph = generator.generate(persons.size(), p, randomSeed);
			
		} else if("lognormal".equalsIgnoreCase(type)) {
			double sigma = Double.parseDouble(config.getParam(MODULE_NAME, "k_sigma"));
			double mu = Double.parseDouble(config.getParam(MODULE_NAME, "k_mu"));
			int k_max = Integer.parseInt(config.getParam(MODULE_NAME, "k_max"));
			UnivariateRealFunction function = new LogNormalDistribution(sigma, mu, 1);
			RandomGraphGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new RandomGraphGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(function, builder, randomSeed);
			graph = (SocialSparseGraph) generator.generate(persons.size(), k_max);
			
		} else if("empty".equalsIgnoreCase(type)) {
			graph = builder.createGraph();
			for(int i = 0; i < persons.size(); i++) {
				builder.addVertex(graph);
			}
			
		} else {
			throw new IllegalArgumentException("No graph type specified!");
		}
		
		return graph;
	}
}

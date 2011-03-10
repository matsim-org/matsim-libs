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
package playground.johannes.studies.mcmc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.generators.ErdosRenyiGenerator;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskArray;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.generators.BarabasiAlbertGenerator;
import playground.johannes.socialnetworks.graph.generators.RandomGraphGenerator;
import playground.johannes.socialnetworks.graph.mcmc.ConserveDegreeDistribution;
import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeFlip;
import playground.johannes.socialnetworks.graph.mcmc.SampleAnalyzer;
import playground.johannes.socialnetworks.graph.social.analysis.SocialAnalyzerTask;
import playground.johannes.socialnetworks.graph.social.generators.ErgmAge;
import playground.johannes.socialnetworks.graph.social.generators.ErgmGender;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.graph.social.io.SocialSparseVertexPool;
import playground.johannes.socialnetworks.graph.spatial.analysis.ExtendedSpatialAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.generators.ErgmLnDistance;
import playground.johannes.socialnetworks.statistics.LogNormalDistribution;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphFactory;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class ErgmSimulator {

	private static final String MODULE_NAME = "ergm";

	private static final Logger logger = Logger.getLogger(ErgmSimulator.class);
	
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
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		/*
		 * load population
		 */
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
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
		double theta_triangles = Double.parseDouble(config.getParam(MODULE_NAME, "theta_triangles"));
		
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
		spatialTask.addTask(new ExtendedSpatialAnalyzerTask());
		array.addAnalyzerTask(spatialTask, "spatial");
		array.addAnalyzerTask(new SocialAnalyzerTask(), "social");
		
		analyzer.setAnalyzerTask(array);
		/*
		 * sample...
		 */
		if(conservePk)
			sampler.sample(y, ergm, analyzer, new ConserveDegreeDistribution());
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
			double p = 10/(double)(persons.size() - 1);
			graph = generator.generate(persons.size(), p, randomSeed);
			
		} else if("lognormal".equalsIgnoreCase(type)) {
			double sigma = Double.parseDouble(config.getParam(MODULE_NAME, "k_sigma"));
			double mu = Double.parseDouble(config.getParam(MODULE_NAME, "k_mu"));
			int k_max = Integer.parseInt(config.getParam(MODULE_NAME, "k_max"));
			UnivariateRealFunction function = new LogNormalDistribution(sigma, mu, 1);
			RandomGraphGenerator generator = new RandomGraphGenerator(function, builder, randomSeed);
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

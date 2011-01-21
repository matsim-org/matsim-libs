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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.generators.BarabasiAlbertGenerator;
import playground.johannes.socialnetworks.graph.mcmc.ConserveDegreeDistribution;
import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTriangles;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeSwitch;
import playground.johannes.socialnetworks.graph.mcmc.SampleAnalyzer;
import playground.johannes.socialnetworks.graph.social.analysis.SocialAnalyzerTask;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.graph.social.io.SocialSparseVertexPool;
import playground.johannes.socialnetworks.graph.social.mcmc.ErgmAge;
import playground.johannes.socialnetworks.graph.social.mcmc.ErgmGender;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.generators.ErgmLnDistance;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.AnalyzerTaskArray;
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

	private static final String MODULE_NAME = "gravityGenerator";

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
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.findParam("plans", "inputPlansFile"));
		Population population = scenario.getPopulation();
		/*
		 * read parameters
		 */
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));;
		long burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		long logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		long sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		String output = config.getParam(MODULE_NAME, "output");
		/*
		 * initialize graph
		 */
		logger.info("Initializing graph...");
		Set<Person> persons = new HashSet<Person>(population.getPersons().values());
		SocialSparseGraphFactory factory = new SocialSparseVertexPool(persons, CRSUtils.getCRS(21781));
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(factory);
		BarabasiAlbertGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new BarabasiAlbertGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(builder);
		SocialSparseGraph graph = generator.generate(5, 5, persons.size() - 5 , randomSeed);
		/*
		 * initialize ergm
		 */
		logger.info("Initializing ergm...");
		AdjacencyMatrix<SocialSparseVertex> y = new AdjacencyMatrix<SocialSparseVertex>(graph, true);
		
		Ergm ergm = new Ergm();
		ergm.addComponent(new ErgmLnDistance(y, -1.6));
		ergm.addComponent(new ErgmAge(-1));
		ergm.addComponent(new ErgmGender(-1));
		ergm.addComponent(new ErgmTriangles(2));
		/*
		 * initialize sampler
		 */
		GibbsEdgeSwitch sampler = new GibbsEdgeSwitch(randomSeed);
		
		SampleAnalyzer<SocialSparseGraph, SocialSparseEdge, SocialSparseVertex> analyzer = new SampleAnalyzer<SocialSparseGraph, SocialSparseEdge, SocialSparseVertex>(graph, builder, output);
		analyzer.setAnalysisInterval(sampleInterval);
		analyzer.setInfoInteraval(logInterval);
		analyzer.setMaxIteration(burnin);
		analyzer.setWriter(new SocialGraphMLWriter());
		/*
		 * initialize analyzer task
		 */
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		array.addAnalyzerTask(new TopologyAnalyzerTask(), "topo");
		array.addAnalyzerTask(new SpatialAnalyzerTask(), "spatial");
		array.addAnalyzerTask(new SocialAnalyzerTask(), "social");
		
		analyzer.setAnalyzerTask(array);
		/*
		 * sample...
		 */
		sampler.sample(y, ergm, analyzer, new ConserveDegreeDistribution());
		logger.info("Done.");
	}

}

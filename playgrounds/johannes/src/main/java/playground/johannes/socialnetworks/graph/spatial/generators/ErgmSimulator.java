/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.generators;

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
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.generators.BarabasiAlbertGenerator;
import playground.johannes.socialnetworks.graph.mcmc.ConserveDegreeDistribution;
import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeSwitch;
import playground.johannes.socialnetworks.graph.mcmc.SamplerListener;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.social.io.SocialSparseVertexPool;
import playground.johannes.socialnetworks.graph.social.mcmc.DumpHandler;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphFactory;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ErgmSimulator {
	
	private static final Logger logger = Logger.getLogger(ErgmSimulator.class);

	private static final String MODULE_NAME = "gravityGenerator";
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
//		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
//		SpatialSparseGraph graph = reader.read(config.findParam("plans", "inputPlansFile"));
//		
//		Population2SocialGraph reader = new Population2SocialGraph();
//		SocialSparseGraph graph = reader.read(config.findParam("plans", "inputPlansFile"), CRSUtils.getCRS(21781));
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.findParam("plans", "inputPlansFile"));
		Population population = scenario.getPopulation();
		
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));

		double k_mean = Double.parseDouble(config.getParam(MODULE_NAME, "meanDegree"));
		long burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		long logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		long sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		String outputDir = config.getParam(MODULE_NAME, "output");
		
		double totalCost = Double.parseDouble(config.getParam(MODULE_NAME, "totalCost"));
		double theta_edge = Double.parseDouble(config.getParam(MODULE_NAME, "thetaEdge"));
		double gamma = Double.parseDouble(config.getParam(MODULE_NAME, "gamma"));
		
		double beta_k = Double.parseDouble(config.getParam(MODULE_NAME, "beta_k"));
		double beta_c = Double.parseDouble(config.getParam(MODULE_NAME, "beta_c"));
		
//		logger.info("Creating random graph...");
//		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
//		ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> generator = new ErdosRenyiGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>(builder);
//		int k = (int)k_mean;
//		double p = k / (double)graph.getVertices().size();
//		generator.setRandomDrawMode(true);
//		generator.generate(graph, p, randomSeed);
//
//		
//		Set<Point> points = new HashSet<Point>();
//		for(SpatialVertex vertex : graph.getVertices())
//			points.add(vertex.getPoint());
		
//		SpatialSparseGraphFactory factory = new SpatialSparseVertexPool(points, graph.getCoordinateReferenceSysten());
//		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(factory);
//		BarabasiAlbertGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> generator = new BarabasiAlbertGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>(builder);
//		graph = generator.generate(5, 5, points.size() - 5 , randomSeed);
		
		Set<Person> persons = new HashSet<Person>(population.getPersons().values());
		SocialSparseGraphFactory factory = new SocialSparseVertexPool(persons, CRSUtils.getCRS(21781));
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(factory);
		BarabasiAlbertGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new BarabasiAlbertGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(builder);
		SocialSparseGraph graph = generator.generate(5, 5, persons.size() - 5 , randomSeed);
		/*
		 * convert graph to matrix
		 */
//		AdjacencyMatrix<SpatialSparseVertex> y = new AdjacencyMatrix<SpatialSparseVertex>(graph);
		AdjacencyMatrix<SocialSparseVertex> y = new AdjacencyMatrix<SocialSparseVertex>(graph);
		/*
		 * setup ergm terms.
		 */
		logger.info("Initializing ERGM...");
		Ergm ergm = new Ergm();
		
//		EdgeCostFunction func = new GravityEdgeCostFunction(gamma, 1, new CartesianDistanceCalculator());
//		UtilFuncDiff diff = new UtilFuncDiff(func, beta_k, beta_c, totalCost);
//		ergm.addComponent(diff);
//		
//		ErgmDensity density = new ErgmDensity();
//		density.setTheta(theta_edge);
//		ergm.addComponent(density);
		
		EdgeProbabilityFunction func = new EdgePowerLawDistance(y, gamma, graph.getEdges().size());
		ergm.addComponent(new ErgmEdgeProba(func));
		
//		EdgeProbabilityFunction func2 = new DegreeSequence(y);
//		ergm.addComponent(new ErgmEdgeProba(func2));
		
//		for(int i = 0; i < y.getVertexCount(); i++) {
//			for(int j = i + 1; j < y.getVertexCount(); j++) {
//				if(y.getEdge(i, j))
//					y.removeEdge(i, j);
//			}
//		}
		/*
		 * setup gibbs sampler.
		 */
		GibbsEdgeSwitch sampler = new GibbsEdgeSwitch();
//		GibbsSampler sampler = new GibbsEdgeFlip(randomSeed);
//		GibbsSampler sampler = new GibbsSampler(randomSeed);
		sampler.setInterval(1000000);
		
//		DumpHandler handler = new DumpHandler(graph, builder, outputDir);
		SamplerListener<SocialSparseVertex> handler = new playground.johannes.socialnetworks.graph.social.mcmc.DumpHandler(graph, builder, outputDir);
//		handler.getAnalyzerTaks().addTask(new EdgeCostsTask(costFunction));
		((DumpHandler) handler).setBurnin(burnin);
		((DumpHandler) handler).setDumpInterval(sampleInterval);
		((DumpHandler) handler).setLogInterval(logInterval);
		((DumpHandler) handler).analyze(y, 0);
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(y, ergm, handler, new ConserveDegreeDistribution());
		logger.info("Gibbs sampler terminated.");
	}

	
}

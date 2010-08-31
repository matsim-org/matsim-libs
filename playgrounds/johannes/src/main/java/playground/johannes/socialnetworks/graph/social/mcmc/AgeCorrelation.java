/* *********************************************************************** *
 * project: org.matsim.*
 * AgeCorrelation.java
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
package playground.johannes.socialnetworks.graph.social.mcmc;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.mcmc.EdgeSwitchCondition;
import playground.johannes.socialnetworks.graph.mcmc.GibbsEdgeFlip;
import playground.johannes.socialnetworks.graph.mcmc.GraphProbability;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class AgeCorrelation implements GraphProbability {

//	private DistanceCalculator calculator = new CartesianDistanceCalculator();
	
	@Override
	public <V extends Vertex> double difference(AdjacencyMatrix<V> y1, int i, int j, boolean yIj) {
		AdjacencyMatrix<SocialVertex> y = (AdjacencyMatrix<SocialVertex>) y1;
	
		
//		if(yIj)
//			y.removeEdge(i, j);
//		
//		TDoubleArrayList values1 = new TDoubleArrayList();
//		TDoubleArrayList values2 = new TDoubleArrayList();
//		
//		for(int u = 0; u < y.getVertexCount(); u++) {
//			for(int v = u+1; v < y.getVertexCount(); v++) {
//				if(y.getEdge(u, v)) {
//					int a1 = y.getVertex(u).getPerson().getAge();
//					int a2 = y.getVertex(v).getPerson().getAge();
//					if(a1 > 0 && a2 > 0) {
//						values1.add(a1);
//						values2.add(a2);
//					}
//				}
//			}
//		}
//		
//		PearsonsCorrelation r = new PearsonsCorrelation();
//		double r_minus = r.correlation(values1.toNativeArray(), values2.toNativeArray());
//		
//		values1.add(y.getVertex(i).getPerson().getAge());
//		values2.add(y.getVertex(j).getPerson().getAge());
//		
//		double r_plus = r.correlation(values1.toNativeArray(), values2.toNativeArray());
//		
//		if(yIj)
//			y.addEdge(i, j);
//		
//		return Math.exp(r_minus - r_plus);
		
		int a1 = y.getVertex(i).getPerson().getAge();
		int a2 = y.getVertex(j).getPerson().getAge();
		
		return Math.exp(Math.abs(a1 - a2));
	}

	private static final Logger logger = Logger.getLogger(AgeCorrelation.class);

	private static final String MODULE_NAME = "gravityGenerator";

	
	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));

		long burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		long logInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "loginterval"));
		long sampleInterval = (long)Double.parseDouble(config.getParam(MODULE_NAME, "sampleinterval"));
		String outputDir = config.getParam(MODULE_NAME, "output");
		
		
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialSparseGraph graph = reader.readGraph(config.findParam(MODULE_NAME, "inputGraphFile"));
		
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		
		GibbsEdgeFlip sampler = new GibbsEdgeFlip(randomSeed);
		sampler.setInterval(100000);
		
		DumpHandler handler = new DumpHandler(graph, builder, outputDir);
		
		EdgeSwitchCondition condition = new EqualDistanceCondition();
		
		AdjacencyMatrix<SocialSparseVertex> y = new AdjacencyMatrix<SocialSparseVertex>(graph);
		
		GraphProbability proba = new AgeCorrelation();
		
		handler.setBurnin(burnin);
		handler.setDumpInterval(sampleInterval);
		handler.setLogInterval(logInterval);
		handler.analyze(y, 0);
		
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(y, proba, handler, condition);
		logger.info("Gibbs sampler terminated.");
	}
}
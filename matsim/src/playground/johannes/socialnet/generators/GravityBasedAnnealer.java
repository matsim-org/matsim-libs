/* *********************************************************************** *
 * project: org.matsim.*
 * GravityBasedAnnealer.java
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

/**
 * 
 */
package playground.johannes.socialnet.generators;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.graph.GraphAnalyser;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.io.PajekClusteringColorizer;
import playground.johannes.graph.io.PajekDegreeColorizer;
import playground.johannes.graph.mcmc.Ergm;
import playground.johannes.graph.mcmc.ErgmDensity;
import playground.johannes.graph.mcmc.ErgmTerm;
import playground.johannes.graph.mcmc.ErgmTriangles;
import playground.johannes.graph.mcmc.GibbsEdgeSwitcher;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.socialnet.generators.GravityBasedGenerator.Handler;
import playground.johannes.socialnet.io.SNGraphMLReader;
import playground.johannes.socialnet.io.SNGraphMLWriter;
import playground.johannes.socialnet.io.SNPajekWriter;
import playground.johannes.socialnet.mcmc.ErgmDistance;
import playground.johannes.socialnet.mcmc.ErgmDistanceLocal;
import playground.johannes.socialnet.mcmc.SNAdjacencyMatrix;
import playground.johannes.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class GravityBasedAnnealer {

	private static final Logger logger = Logger.getLogger(GravityBasedAnnealer.class);
	
	private static final String MODULE_NAME = "gravityGenerator";
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		Scenario data = loader.getScenario();
		Population population = data.getPopulation();
		
		String outputDir = config.getParam(MODULE_NAME, "output");
		/*
		 * Setup social network and adjacency matrix.
		 */
		SNGraphMLReader<Person> reader = new SNGraphMLReader<Person>(population);
		SocialNetwork<Person> socialnet = reader.readGraph(config.getParam(MODULE_NAME,"socialnetwork"));
		SNAdjacencyMatrix<Person> matrix = new SNAdjacencyMatrix<Person>(socialnet);
		/*
		 * Setup ergm terms.
		 */
		double theta_density = Double.parseDouble(config.getParam(MODULE_NAME, "theta_density"));
		double theta_distance = Double.parseDouble(config.getParam(MODULE_NAME, "theta_distance"));
		double theta_triangle = Double.parseDouble(config.getParam(MODULE_NAME, "theta_triangle"));
		
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[3];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(theta_density);
		terms[1] = new ErgmDistanceLocal(matrix);
		terms[1].setTheta(theta_distance);
		terms[2] = new ErgmTriangles();
		terms[2].setTheta(theta_triangle);
		ergm.setErgmTerms(terms);
		/*
		 * Setup gibbs sampler.
		 */
		int burnin = (int)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		int samplesize = Integer.parseInt(config.getParam(MODULE_NAME, "samplesize"));
		int sampleinterval = Integer.parseInt(config.getParam(MODULE_NAME, "sampleinterval"));
		
		GibbsEdgeSwitcher sampler = new GibbsEdgeSwitcher();
		sampler.setInterval(1000000);
		Handler handler = new Handler(outputDir, args[1]);
		handler.setSampleSize(samplesize);
		handler.setSampleInterval(sampleinterval);
		
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(matrix, ergm, burnin, handler);
		logger.info("Gibbs sampler terminated.");
		logger.info(handler.toString());
		/*
		 * Output results...
		 */
//		socialnet = matrix.getGraph();
//		SNGraphMLWriter writer = new SNGraphMLWriter();
//		writer.write(socialnet, outputDir + "socialnet.graphml");
//		
//		PajekDegreeColorizer<Ego<Person>, SocialTie> colorizer1 = new PajekDegreeColorizer<Ego<Person>, SocialTie>(socialnet, false);
//		PajekClusteringColorizer<Ego<Person>, SocialTie> colorizer2 = new PajekClusteringColorizer<Ego<Person>, SocialTie>(socialnet);
//		SNPajekWriter<Person> pwriter = new SNPajekWriter<Person>();
//		pwriter.write(socialnet, colorizer1, outputDir + "socialnet.degree.net");
//		pwriter.write(socialnet, colorizer2, outputDir + "socialnet.clustering.net");
//		
//		Distribution.writeHistogram(SocialNetworkStatistics.edgeLengthDistribution(socialnet, false, 0).absoluteDistribution(1000), outputDir + "edgelength.hist.txt");
//		Distribution.writeHistogram(GraphStatistics.getDegreeDistribution(socialnet).absoluteDistribution(), outputDir + "degree.hist.txt");
//		
//		GraphAnalyser.main(new String[]{outputDir + "socialnet.graphml", outputDir + "socialnet.txt", "-e"});
	
	}

}

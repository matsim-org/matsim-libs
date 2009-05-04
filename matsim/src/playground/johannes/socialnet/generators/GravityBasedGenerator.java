/* *********************************************************************** *
 * project: org.matsim.*
 * GravityBasedGenerator.java
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

import gnu.trove.TDoubleArrayList;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.graph.GraphAnalyser;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.graph.io.PajekClusteringColorizer;
import playground.johannes.graph.io.PajekDegreeColorizer;
import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.graph.mcmc.AdjacencyMatrixStatistics;
import playground.johannes.graph.mcmc.Ergm;
import playground.johannes.graph.mcmc.ErgmDensity;
import playground.johannes.graph.mcmc.ErgmTerm;
import playground.johannes.graph.mcmc.GibbsSampler;
import playground.johannes.graph.mcmc.MCMCSampleDelegate;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialNetworkFactory;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.socialnet.io.SNGraphMLWriter;
import playground.johannes.socialnet.io.SNPajekWriter;
import playground.johannes.socialnet.mcmc.ErgmDistanceLocal;
import playground.johannes.socialnet.mcmc.SNAdjacencyMatrix;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class GravityBasedGenerator {

	private static final Logger logger = Logger.getLogger(GravityBasedGenerator.class);
	
	private static final String MODULE_NAME = "gravityGenerator";
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		Scenario data = loader.getScenario();
		Population population = data.getPopulation();
		
		String outputDir = config.getParam(MODULE_NAME, "output");
		/*
		 * Setup social network and adjacency matrix.
		 */
		SocialNetworkFactory<Person> factory = new SocialNetworkFactory<Person>(population);
		ErdosRenyiGenerator<SocialNetwork<Person>, Ego<Person>, SocialTie> generator = new ErdosRenyiGenerator<SocialNetwork<Person>, Ego<Person>, SocialTie>(factory);
//		SocialNetwork<Person> socialnet = generator.generate(population.getPersons().size(), 0.00001, config.global().getRandomSeed());
		SocialNetwork<Person> socialnet = new SocialNetwork<Person>(population);
		SNAdjacencyMatrix<Person> matrix = new SNAdjacencyMatrix<Person>(socialnet);
		/*
		 * Setup ergm terms.
		 */
		double theta_density = Double.parseDouble(config.getParam(MODULE_NAME, "theta_density"));
		double theta_distance = Double.parseDouble(config.getParam(MODULE_NAME, "theta_distance"));;
		
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[2];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(theta_density);
		terms[1] = new ErgmDistanceLocal(matrix);
		terms[1].setTheta(theta_distance);
		
		ergm.setErgmTerms(terms);
		/*
		 * Setup gibbs sampler.
		 */
		int burnin = (int)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		int samplesize = Integer.parseInt(config.getParam(MODULE_NAME, "samplesize"));
		int sampleinterval = Integer.parseInt(config.getParam(MODULE_NAME, "sampleinterval"));
		
		GibbsSampler sampler = new GibbsSampler();
		sampler.setInterval(1000000);
		Handler handler = new Handler(outputDir + "samplestats.txt");
		handler.setSampleSize(samplesize);
		handler.setSampleInterval(sampleinterval);
		
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(matrix, ergm, burnin, handler);
		logger.info("Gibbs sampler terminated.");
		logger.info(handler.toString());
		/*
		 * Output results...
		 */
		socialnet = matrix.getGraph();
		SNGraphMLWriter writer = new SNGraphMLWriter();
		writer.write(socialnet, outputDir + "socialnet.graphml");
		
		PajekDegreeColorizer<Ego<Person>, SocialTie> colorizer1 = new PajekDegreeColorizer<Ego<Person>, SocialTie>(socialnet, false);
		PajekClusteringColorizer<Ego<Person>, SocialTie> colorizer2 = new PajekClusteringColorizer<Ego<Person>, SocialTie>(socialnet);
		SNPajekWriter<Person> pwriter = new SNPajekWriter<Person>();
		pwriter.write(socialnet, colorizer1, outputDir + "socialnet.degree.net");
		pwriter.write(socialnet, colorizer2, outputDir + "socialnet.clustering.net");
		
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(socialnet, false, 0).absoluteDistribution(1000), outputDir + "edgelength.hist.txt");
		WeightedStatistics.writeHistogram(GraphStatistics.getDegreeDistribution(socialnet).absoluteDistribution(), outputDir + "degree.hist.txt");
		
		GraphAnalyser.main(new String[]{outputDir + "socialnet.graphml", outputDir + "socialnet.txt", "-e"});
	}

	public static class Handler implements MCMCSampleDelegate {
		
		private static final Logger logger = Logger.getLogger(Handler.class);

		private TDoubleArrayList edges = new TDoubleArrayList();
		
		private TDoubleArrayList degree = new TDoubleArrayList();
		
		private TDoubleArrayList clustering = new TDoubleArrayList();
		
		private TDoubleArrayList distance = new TDoubleArrayList();
		
		private int sampleSize;
		
		private int sampleInterval;
		
		private BufferedWriter writer;
		
		public Handler(String filename) {
			try {
				writer = new BufferedWriter(new FileWriter(filename));
				writer.write("m\t<k>\t<c_local>\t<c_global>\t<d>");
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public boolean checkTerminationCondition(AdjacencyMatrix y) {
			double m = y.getEdgeCount();
			double k_mean = AdjacencyMatrixStatistics.getMeanDegree(y);
			double c_local = AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y);
			double c_global = AdjacencyMatrixStatistics.getGlobalClusteringCoefficient(y);
		
			SocialNetwork<?> net = ((SNAdjacencyMatrix)y).getGraph();
			double d_mean = SocialNetworkStatistics.getEdgeLengthStatistics(net).getMean();
			logger.info(String.format(Locale.US, "m=%1$s, <k>=%2$.4f, <c_local>=%3$.4f, <c_global>=%4$.4f, <d>=%5$.4f", m, k_mean, c_local, c_global, d_mean));
		
			try {
				writer.write(String.format(Locale.US, "%1$s\t%2$.4f\t%3$.4f\t%4$.4f\t%5$.4f", m, k_mean, c_local, c_global, d_mean));
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return false;
		}

		public void handleSample(AdjacencyMatrix y) {
			edges.add(y.getEdgeCount());
			degree.add(AdjacencyMatrixStatistics.getMeanDegree(y));
			clustering.add(AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y));
			
			SocialNetwork<?> net = ((SNAdjacencyMatrix)y).getGraph();
			distance.add(SocialNetworkStatistics.getEdgeLengthStatistics(net).getMean());
		}

		public int getSampleInterval() {
			return sampleInterval;
		}

		public int getSampleSize() {
			return sampleSize;
		}
		
		public void setSampleSize(int sampleSize) {
			this.sampleSize = sampleSize;
		}

		public void setSampleInterval(int sampleInterval) {
			this.sampleInterval = sampleInterval;
		}

		@Override
		public String toString() {
			return String.format("var(m)=%1$.4f, var(<k>)=%2$.4f, var(<c_local>)=%3$s.4f, var(<d>)=%4$.4f",
					StatUtils.variance(edges.toNativeArray()),
					StatUtils.variance(degree.toNativeArray()),
					StatUtils.variance(clustering.toNativeArray()),
					StatUtils.variance(distance.toNativeArray()));
		}
		
	}
}

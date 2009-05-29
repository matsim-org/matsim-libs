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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

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
import playground.johannes.socialnet.SocialNetworkAnalyzer;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.socialnet.io.PajekDistanceColorizer;
import playground.johannes.socialnet.io.SNGraphMLWriter;
import playground.johannes.socialnet.io.SNKMLDegreeStyle;
import playground.johannes.socialnet.io.SNKMLWriter;
import playground.johannes.socialnet.io.SNPajekWriter;
import playground.johannes.socialnet.mcmc.ErgmDistanceLocal;
import playground.johannes.socialnet.mcmc.SNAdjacencyMatrix;
import playground.johannes.socialnet.spatial.GridUtils;
import playground.johannes.socialnet.spatial.SpatialGrid;
import playground.johannes.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class GravityBasedGenerator {

	private static final Logger logger = Logger.getLogger(GravityBasedGenerator.class);
	
	private static final String MODULE_NAME = "gravityGenerator";
	
//	private static String outputDir;
	
	private static SpatialGrid<Double> densityGrid;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadPopulation();
		Scenario scenario = loader.getScenario();
		Population population = scenario.getPopulation();
//		LatticeGenerator generator = new LatticeGenerator();
//		BasicPopulation<BasicPerson<?>> population = generator.generate(100, 100);
		String outputDir = config.getParam(MODULE_NAME, "output");
		/*
		 * Setup social network and adjacency matrix.
		 */
//		SocialNetworkFactory<Person> factory = new SocialNetworkFactory<Person>(population);
//		ErdosRenyiGenerator<SocialNetwork<Person>, Ego<Person>, SocialTie> generator = new ErdosRenyiGenerator<SocialNetwork<Person>, Ego<Person>, SocialTie>(factory);
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
		long burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		int samplesize = Integer.parseInt(config.getParam(MODULE_NAME, "samplesize"));
		int sampleinterval = Integer.parseInt(config.getParam(MODULE_NAME, "sampleinterval"));
		
		GibbsSampler sampler = new GibbsSampler();
		sampler.setInterval(1000000);
		SpatialGrid<Double> densityGrid = SpatialGrid.readFromFile(args[1]);
//		SpatialGrid<Double> densityGrid = GridUtils.createDensityGrid(population, 10);
		Handler handler = new Handler(outputDir, densityGrid);
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
//		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(socialnet, false, 0).absoluteDistribution(1000), outputDir + "edgelength.hist.txt");
//		WeightedStatistics.writeHistogram(GraphStatistics.getDegreeDistribution(socialnet).absoluteDistribution(), outputDir + "degree.hist.txt");
//		
//		GraphAnalyser.main(new String[]{outputDir + "socialnet.graphml", outputDir + "socialnet.txt", "-e"});
	}

	public static class Handler implements MCMCSampleDelegate {
		
		private static final Logger logger = Logger.getLogger(Handler.class);

		private Distribution edges = new Distribution();
		
		private Distribution degree = new Distribution();
		
		private Distribution clustering = new Distribution();
		
		private Distribution distance = new Distribution();
		
		private int sampleSize;
		
		private int sampleInterval;
		
		private BufferedWriter writer;
		
		private int counter;
		
		private String outputDir;
		
		private SpatialGrid<Double> densityGrid;
		
		public Handler(String filename, SpatialGrid<Double> densityGrid) {
			outputDir = filename;
			this.densityGrid = densityGrid;
			try {
				writer = new BufferedWriter(new FileWriter(filename + "samplestats.txt"));
				writer.write("m\t<k>\t<c_local>\t<c_global>\t<d>");
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			counter = 0;
		}
		
		public boolean checkTerminationCondition(AdjacencyMatrix y) {
			double m = y.getEdgeCount();
			double k_mean = AdjacencyMatrixStatistics.getMeanDegree(y);
			double c_local = AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y);
			double c_global = AdjacencyMatrixStatistics.getGlobalClusteringCoefficient(y);
		
			SocialNetwork<?> net = ((SNAdjacencyMatrix)y).getGraph();
			double d_mean = SocialNetworkStatistics.edgeLengthDistribution(net).mean();
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
			
			SocialNetwork<BasicPerson<?>> net = ((SNAdjacencyMatrix)y).getGraph();
			distance.add(SocialNetworkStatistics.edgeLengthDistribution(net).mean());
			
			try {
				String currentOutputDir = String.format("%1$s%2$s/", outputDir, counter);
				File file = new File(currentOutputDir);
				file.mkdirs();
				SNGraphMLWriter writer = new SNGraphMLWriter();
				writer.write(net, String.format("%1$s%2$s.socialnet.graphml", currentOutputDir, counter));
				SocialNetworkAnalyzer.analyze(net, currentOutputDir, false, densityGrid);
				SNKMLWriter<BasicPerson<?>> kmlWriter = new SNKMLWriter<BasicPerson<?>>();
				kmlWriter.setVertexStyle(new SNKMLDegreeStyle<BasicPerson<?>>(kmlWriter.getVertexIconLink()));
				kmlWriter.write((SocialNetwork<BasicPerson<?>>) net, String.format("%1$s%2$s.socialnet.k.kml", currentOutputDir, counter));
				//kmlWriter.write((SocialNetwork<Person>) net, new SNKMLClusteringStyle<Person>(), null, String.format("%1$s%2$s.socialnet.k.kml", currentOutputDir, counter));
				counter++;
				
				PajekDegreeColorizer<Ego<BasicPerson<?>>, SocialTie> colorizer1 = new PajekDegreeColorizer<Ego<BasicPerson<?>>, SocialTie>(net, true);
				PajekClusteringColorizer<Ego<BasicPerson<?>>, SocialTie> colorizer2 = new PajekClusteringColorizer<Ego<BasicPerson<?>>, SocialTie>(net);
				PajekDistanceColorizer<BasicPerson<?>> colorizer3 = new PajekDistanceColorizer<BasicPerson<?>>(net, false);
				SNPajekWriter<BasicPerson<?>> pwriter = new SNPajekWriter<BasicPerson<?>>();
				pwriter.write(net, colorizer1, currentOutputDir + "socialnet.degree.net");
				pwriter.write(net, colorizer2, currentOutputDir+ "socialnet.clustering.net");
				pwriter.write(net, colorizer3, currentOutputDir + "socialnet.distance.net");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(net, false, 0).absoluteDistribution(1000), outputDir + "edgelength.hist.txt");
//			WeightedStatistics.writeHistogram(GraphStatistics.getDegreeDistribution(net).absoluteDistribution(), outputDir + "degree.hist.txt");
			
			
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
		
			return String.format("VarK(m)=%1$.4f, VarK(<k>)=%2$.4f, VarK(<c_local>)=%3$s.4f, VarK(<d>)=%4$.4f",
					edges.varianceCoefficient(),
					degree.varianceCoefficient(),
					clustering.varianceCoefficient(),
					distance.varianceCoefficient());
					
		}
		
	}
}

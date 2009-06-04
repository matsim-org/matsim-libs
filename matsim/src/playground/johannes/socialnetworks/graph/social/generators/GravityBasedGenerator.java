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
package playground.johannes.socialnetworks.graph.social.generators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.socialnetworks.graph.io.PajekClusteringColorizer;
import playground.johannes.socialnetworks.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixStatistics;
import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmDensity;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.mcmc.MCMCSampleDelegate;
import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialNetworkAnalyzer;
import playground.johannes.socialnetworks.graph.social.SocialTie;
import playground.johannes.socialnetworks.graph.social.io.SNGraphMLWriter;
import playground.johannes.socialnetworks.graph.social.mcmc.SNAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.ErgmGravity;
import playground.johannes.socialnetworks.graph.spatial.GridUtils;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.io.KMLDegreeStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.PajekDistanceColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;
import playground.johannes.socialnetworks.statistics.Distribution;

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
		loader.loadPopulation();
		Scenario scenario = loader.getScenario();
		Population population = scenario.getPopulation();
//		LatticeGenerator generator = new LatticeGenerator();
//		BasicPopulation<BasicPerson<?>> population = generator.generate(100, 100);
		

		GravityBasedGenerator generator = new GravityBasedGenerator();
		generator.thetaDensity = Double.parseDouble(config.getParam(MODULE_NAME, "theta_density"));
		generator.burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		generator.sampleSize = Integer.parseInt(config.getParam(MODULE_NAME, "samplesize"));
		generator.sampleInterval = Integer.parseInt(config.getParam(MODULE_NAME, "sampleinterval"));
		generator.outputDir = config.getParam(MODULE_NAME, "output"); 
//		SpatialGrid<Double> densityGrid = SpatialGrid.readFromFile(args[1]);
		SpatialGrid<Double> densityGrid = GridUtils.createDensityGrid(population, 10);
		
		generator.generate(population, densityGrid);

	}

	private long burnin;
	
	private int sampleSize;
	
	private int sampleInterval;
	
	private String outputDir;
	
	private double thetaDensity;
	
	private final double thetaGravity = 1;
	
	public <P extends BasicPerson<?>> void generate(BasicPopulation<P> population, SpatialGrid<Double> densityGrid) {
		SocialNetwork<P> socialnet = new SocialNetwork<P>(population);
		
		
		SNAdjacencyMatrix<P> matrix = new SNAdjacencyMatrix<P>(socialnet);
		/*
		 * Setup ergm terms.
		 */
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[2];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(thetaDensity);
		terms[1] = new ErgmGravity(matrix);
		terms[1].setTheta(thetaGravity);
		
		ergm.setErgmTerms(terms);
		/*
		 * Setup gibbs sampler.
		 */
		GibbsSampler sampler = new GibbsSampler();
		sampler.setInterval(1000000);
		Handler handler = new Handler(outputDir, densityGrid);
		handler.setSampleSize(sampleSize);
		handler.setSampleInterval(sampleInterval);
		
		logger.info(String.format("Starting gibbs sampler. Burnin time: %1$s iterations.", burnin));
		sampler.sample(matrix, ergm, burnin, handler);
		logger.info("Gibbs sampler terminated.");
		logger.info(handler.toString());
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
			double d_mean = SpatialGraphStatistics.edgeLengthDistribution(net).mean();
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
			distance.add(SpatialGraphStatistics.edgeLengthDistribution(net).mean());
			
			try {
				String currentOutputDir = String.format("%1$s%2$s/", outputDir, counter);
				File file = new File(currentOutputDir);
				file.mkdirs();
				SNGraphMLWriter writer = new SNGraphMLWriter();
				writer.write(net, String.format("%1$s%2$s.socialnet.graphml", currentOutputDir, counter));
				SocialNetworkAnalyzer.analyze(net, currentOutputDir, false, densityGrid);
				KMLWriter kmlWriter = new KMLWriter();
				kmlWriter.setVertexStyle(new KMLDegreeStyle(kmlWriter.getVertexIconLink()));
				kmlWriter.write((SocialNetwork<BasicPerson<?>>) net, String.format("%1$s%2$s.socialnet.k.kml", currentOutputDir, counter));
				//kmlWriter.write((SocialNetwork<Person>) net, new SNKMLClusteringStyle<Person>(), null, String.format("%1$s%2$s.socialnet.k.kml", currentOutputDir, counter));
				counter++;
				
				PajekDegreeColorizer<Ego<BasicPerson<?>>, SocialTie> colorizer1 = new PajekDegreeColorizer<Ego<BasicPerson<?>>, SocialTie>(net, true);
				PajekClusteringColorizer<Ego<BasicPerson<?>>, SocialTie> colorizer2 = new PajekClusteringColorizer<Ego<BasicPerson<?>>, SocialTie>(net);
				PajekDistanceColorizer colorizer3 = new PajekDistanceColorizer(net, false);
				SpatialPajekWriter pwriter = new SpatialPajekWriter();
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

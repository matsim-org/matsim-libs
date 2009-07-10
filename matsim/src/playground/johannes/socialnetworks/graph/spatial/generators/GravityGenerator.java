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
package playground.johannes.socialnetworks.graph.spatial.generators;

import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.io.PajekClusteringColorizer;
import playground.johannes.socialnetworks.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixStatistics;
import playground.johannes.socialnetworks.graph.mcmc.Ergm;
import playground.johannes.socialnetworks.graph.mcmc.ErgmDensity;
import playground.johannes.socialnetworks.graph.mcmc.ErgmPrefAttach;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.mcmc.GibbsSampler;
import playground.johannes.socialnetworks.graph.mcmc.MCMCSampleDelegate;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.KMLDegreeStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexDescriptor;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.PajekDistanceColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class GravityGenerator {

	private static final Logger logger = Logger.getLogger(GravityGenerator.class);
	
	private static final String MODULE_NAME = "gravityGenerator";
	
	public static void main(String[] args) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
		Population2SpatialGraph reader = new Population2SpatialGraph();
		SpatialGraph graph = reader.read(config.findParam("plans", "inputPlansFile"));

		

		GravityGenerator generator = new GravityGenerator();
		generator.thetaDensity = Double.parseDouble(config.getParam(MODULE_NAME, "theta_density"));
		generator.burnin = (long)Double.parseDouble(config.getParam(MODULE_NAME, "burnin"));
		generator.sampleSize = Integer.parseInt(config.getParam(MODULE_NAME, "samplesize"));
		generator.sampleInterval = Integer.parseInt(config.getParam(MODULE_NAME, "sampleinterval"));
		generator.outputDir = config.getParam(MODULE_NAME, "output");
		generator.descretization = Double.parseDouble(config.getParam(MODULE_NAME, "descretization"));
		
		String gridFile = config.findParam(MODULE_NAME, "densityGrid");
		SpatialGrid<Double> densityGrid = null;
		if(gridFile != null)
			densityGrid = SpatialGrid.readFromFile(gridFile);
		
		new File(generator.outputDir).mkdirs();
		generator.generate(graph, densityGrid);

	}

	private long burnin;
	
	private int sampleSize;
	
	private int sampleInterval;
	
	private String outputDir;
	
	private double thetaDensity;
	
	private final double thetaGravity = 1;
	
	private double descretization = 1000.0;
	
	public void generate(SpatialGraph graph, SpatialGrid<Double> densityGrid) {
		SpatialAdjacencyMatrix matrix = new SpatialAdjacencyMatrix(graph);
		/*
		 * Setup ergm terms.
		 */
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[2];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(thetaDensity);
		
//		Rectangle2D.Double boundingBox = null;
//		if(densityGrid != null) {
//			boundingBox = new Rectangle2D.Double();
//			boundingBox.setFrame(densityGrid.getXmin() - 5*densityGrid.getResolution(), densityGrid.getYmin() - 5*densityGrid.getResolution(),
//					(densityGrid.getXmax() - densityGrid.getXmin()) + 5*densityGrid.getResolution(),
//					(densityGrid.getYmax() - densityGrid.getYmin()) + 5*densityGrid.getResolution());
//		}
		ErgmGravity gravity = new ErgmGravity(matrix, descretization);
		gravity.setTheta(thetaGravity);
		gravity.setDescretization(descretization);
		terms[1] = gravity;
		
//		ErgmPrefAttach attach = new ErgmPrefAttach();
//		attach.setTheta(0.1);
//		terms[2] = attach;
		
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
		
			SpatialGraph net = ((SpatialAdjacencyMatrix)y).getGraph();
			double d_mean = SpatialGraphStatistics.edgeLengthDistribution(net).mean();
			logger.info(String.format(Locale.US, "m=%1$s, <k>=%2$.4f, <c_local>=%3$.4f, <c_global>=%4$.4f, <d>=%5$.4f", m, k_mean, c_local, c_global, d_mean));
		
			try {
				writer.write(String.format(Locale.US, "%1$s\t%2$.4f\t%3$.4f\t%4$.4f\t%5$.4f", m, k_mean, c_local, c_global, d_mean));
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return false;
		}

		public void handleSample(AdjacencyMatrix y) {
			edges.add(y.getEdgeCount());
			degree.add(AdjacencyMatrixStatistics.getMeanDegree(y));
			clustering.add(AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y));
			
			SpatialGraph net = ((SpatialAdjacencyMatrix)y).getGraph();
			distance.add(SpatialGraphStatistics.edgeLengthDistribution(net).mean());
			
			try {
				/*
				 * make directories
				 */
				String currentOutputDir = String.format("%1$s%2$s/", outputDir, counter);
				File file = new File(currentOutputDir);
				file.mkdirs();
				/*
				 * graph analysis
				 */
				SpatialGraphAnalyzer.analyze(net, currentOutputDir, false, densityGrid);
				/*
				 * graph output
				 * 
				 * graphML
				 */
				SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
				writer.write(net, String.format("%1$s%2$s.graph.graphml", currentOutputDir, counter));
				/*
				 * KML
				 */
				KMLWriter kmlWriter = new KMLWriter();
				kmlWriter.setVertexStyle(new KMLDegreeStyle(kmlWriter.getVertexIconLink()));
				kmlWriter.setVertexDescriptor(new KMLVertexDescriptor(net));
				kmlWriter.setDrawEdges(false);
				kmlWriter.setCoordinateTransformation(new CH1903LV03toWGS84());
				kmlWriter.write(net, String.format("%1$s%2$s.graph.k.kml", currentOutputDir, counter));
				/*
				 * Pajek
				 */
				PajekDegreeColorizer<SpatialVertex, SpatialEdge> colorizer1 = new PajekDegreeColorizer<SpatialVertex, SpatialEdge>(net, true);
				PajekClusteringColorizer<SpatialVertex, SpatialEdge> colorizer2 = new PajekClusteringColorizer<SpatialVertex, SpatialEdge>(net);
				PajekDistanceColorizer colorizer3 = new PajekDistanceColorizer(net, false);
				SpatialPajekWriter pwriter = new SpatialPajekWriter();
				pwriter.write(net, colorizer1, currentOutputDir + "graph.degree.net");
				pwriter.write(net, colorizer2, currentOutputDir+ "graph.clustering.net");
				pwriter.write(net, colorizer3, currentOutputDir + "graph.distance.net");
				
				counter++;
			} catch (IOException e) {
				e.printStackTrace();
			}
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

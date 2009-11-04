/* *********************************************************************** *
 * project: org.matsim.*
 * DumpHandler.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.io.PajekClusteringColorizer;
import playground.johannes.socialnetworks.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixStatistics;
import playground.johannes.socialnetworks.graph.mcmc.SampleHandler;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.graph.spatial.io.KMLDegreeStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexDescriptor;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.PajekDistanceColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;
import playground.johannes.socialnetworks.statistics.Distribution;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class DumpHandler implements SampleHandler {

	private long burnin;
	
	private long dumpInterval;
	
	private long logInterval;
	
	private static final Logger logger = Logger.getLogger(DumpHandler.class);

	private Distribution edges = new Distribution();
	
	private Distribution degree = new Distribution();
	
	private Distribution clustering = new Distribution();
	
	private Distribution distance = new Distribution();
	
	protected String outputDir;
	
	private BufferedWriter writer;
	
//	private SpatialGrid<Double> densityGrid;
	
	protected ZoneLayerDouble zones;
	
	private TravelTimeMatrix matrix;
	
//	private Geometry boundary;
	
	public DumpHandler(String filename, ZoneLayerDouble zones, TravelTimeMatrix matrix) {
		outputDir = filename;
		this.matrix = matrix;
//		this.densityGrid = densityGrid;
		this.zones = zones;
//		this.boundary = boundary;
		try {
			writer = new BufferedWriter(new FileWriter(filename + "samplestats.txt"));
			writer.write("it\tm\t<k>\t<c_local>\t<c_global>\t<d>");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public long getBurnin() {
		return burnin;
	}

	public void setBurnin(long burnin) {
		this.burnin = burnin;
	}

	public long getDumpInterval() {
		return dumpInterval;
	}

	public void setDumpInterval(long dumpInterval) {
		this.dumpInterval = dumpInterval;
	}

	public long getLogInterval() {
		return logInterval;
	}

	public void setLogInterval(long logInterval) {
		this.logInterval = logInterval;
	}

	public boolean handle(AdjacencyMatrix y, long iteration) {
		if(iteration % logInterval == 0) {
			log(y, iteration);
		}
		
		if(iteration >= burnin) {
			dump(y, iteration, matrix);
			return false;
		} else if (iteration % dumpInterval == 0 && iteration > 0) {
			dump(y, iteration, matrix);
			return true;
		} else {
			return true;
		}
	}

	private void log(AdjacencyMatrix y, long iteration) {
		double m = y.getEdgeCount();
		double k_mean = AdjacencyMatrixStatistics.getMeanDegree(y);
		double c_local = AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y);
		double c_global = AdjacencyMatrixStatistics.getGlobalClusteringCoefficient(y);
	
		SpatialSparseGraph net = ((SpatialAdjacencyMatrix)y).getGraph();
		double d_mean = SpatialGraphStatistics.edgeLengthDistribution(net).mean();
		logger.info(String.format(Locale.US, "m=%1$s, <k>=%2$.4f, <c_local>=%3$.4f, <c_global>=%4$.4f, <d>=%5$.4f", m, k_mean, c_local, c_global, d_mean));
	
		try {
			writer.write(String.format(Locale.US, "%6$s\t%1$s\t%2$.4f\t%3$.4f\t%4$.4f\t%5$.4f", m, k_mean, c_local, c_global, d_mean, iteration));
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	protected SpatialSparseGraph dump(AdjacencyMatrix y, long iteration, TravelTimeMatrix matrix) {
		logger.info("Dumping sample...");
		
		edges.add(y.getEdgeCount());
		degree.add(AdjacencyMatrixStatistics.getMeanDegree(y));
		clustering.add(AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y));
		
		SpatialSparseGraph net = ((SpatialAdjacencyMatrix)y).getGraph();
		distance.add(SpatialGraphStatistics.edgeLengthDistribution(net).mean());
		
		logger.info(String.format("VarK(m)=%1$.4f, VarK(<k>)=%2$.4f, VarK(<c_local>)=%3$.4f, VarK(<d>)=%4$.4f",
				edges.varianceCoefficient(),
				degree.varianceCoefficient(),
				clustering.varianceCoefficient(),
				distance.varianceCoefficient()));
		
		try {
			/*
			 * make directories
			 */
			String currentOutputDir = String.format("%1$s%2$s/", outputDir, iteration);
			File file = new File(currentOutputDir);
			file.mkdirs();
			/*
			 * graph analysis
			 */
			SpatialGraphAnalyzer.analyze(net, currentOutputDir, false, zones, matrix, null);
			/*
			 * graph output
			 * 
			 * graphML
			 */
			SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
			writer.write(net, String.format("%1$sgraph.graphml", currentOutputDir));
			/*
			 * KML
			 */
			KMLWriter kmlWriter = new KMLWriter();
			kmlWriter.setVertexStyle(new KMLDegreeStyle(kmlWriter.getVertexIconLink()));
			kmlWriter.setVertexDescriptor(new KMLVertexDescriptor(net));
			kmlWriter.setDrawEdges(false);
			kmlWriter.setCoordinateTransformation(new CH1903LV03toWGS84());
			kmlWriter.write(net, String.format("%1$sgraph.k.kml", currentOutputDir));
			/*
			 * Pajek
			 */
			PajekDegreeColorizer<SpatialSparseVertex, SpatialSparseEdge> colorizer1 = new PajekDegreeColorizer<SpatialSparseVertex, SpatialSparseEdge>(net, true);
			PajekClusteringColorizer<SpatialSparseVertex, SpatialSparseEdge> colorizer2 = new PajekClusteringColorizer<SpatialSparseVertex, SpatialSparseEdge>(net);
			PajekDistanceColorizer colorizer3 = new PajekDistanceColorizer(net, false);
			SpatialPajekWriter pwriter = new SpatialPajekWriter();
			pwriter.write(net, colorizer1, currentOutputDir + "graph.degree.net");
			pwriter.write(net, colorizer2, currentOutputDir+ "graph.clustering.net");
			pwriter.write(net, colorizer3, currentOutputDir + "graph.distance.net");
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return net;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * GraphAnalyzer.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Vertex;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialGraphMLReader;
import playground.johannes.socialnetworks.survey.ivt2009.spatial.SampledSpatialVertex;
import de.schlichtherle.io.File;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzer {

	private static final Logger logger = Logger.getLogger(GraphAnalyzer.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(args[0]);
		analyze(graph, args[1]);

		SpatialGrid<Double> grid = SpatialGrid.readFromFile(args[2]);
		analyze(graph, grid, args[1]);
		
		Population2SpatialGraph pop2graph = new Population2SpatialGraph();
		SpatialGraph g2 = pop2graph.read(args[3]);
		analyze(graph, g2, args[1]);
	}

	public static <V extends SampledSpatialGraph> void analyze(SampledSpatialGraph graph, String output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(output + "summary.txt"));
		/*
		 * degree
		 */
		Distribution kDistr = SampledGraphStatistics.degreeDistribution(graph);
		double kMean = kDistr.mean();
		logger.info("<k> = " + kMean);
		if(writer != null) {
			writer.write("k_mean = " + kMean);
			writer.newLine();
			Distribution.writeHistogram(kDistr.absoluteDistribution(), output + "degree.txt");
		}
		/*
		 * clustering
		 */
		Distribution cDistr = SampledGraphStatistics.localClusteringDistribution(graph);
		double cMean = cDistr.mean();
		logger.info("<C> = " + cMean);
		if(writer != null) {
			writer.write("c_mean = " + cMean);
			writer.newLine();
			Distribution.writeHistogram(cDistr.absoluteDistribution(0.1), output + "localClustering.txt");
		}
		/*
		 * edge length
		 */
		Distribution dDistr = SampledGraphStatistics.<SampledSpatialVertex>edgeLenghtDistribution(graph);
		double dMean = dDistr.mean();
		logger.info("<d> = " + dMean);
		if(writer != null) {
			writer.write("d_mean = " + dMean);
			writer.newLine();
			Distribution.writeHistogram(dDistr.absoluteDistribution(1000), output + "distance.txt");
			Distribution.writeHistogram(dDistr.absoluteDistributionLog2(1000), output + "distance.log2.txt");
			Distribution.writeHistogram(SampledGraphStatistics.edgeLengthDegreeCorrelation((Set)graph.getVertices()), output + "distance_k.txt");
		}
		
		writer.close();
	}
	
	public static <V extends SampledSpatialGraph> void analyze(SampledSpatialGraph graph, SpatialGrid<Double> grid, String output) throws IOException {
		Set partition = SnowballPartitions.createSampledPartition((Set)graph.getVertices());
		
		TDoubleObjectHashMap<Set<V>> partitions = SpatialGraphStatistics.createDensityPartitions(partition, grid, 2000);
		TDoubleObjectIterator<Set<V>> it = partitions.iterator();
		
		new File(output + "rhoPartitions").mkdirs();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			Distribution dDistr = SpatialGraphStatistics.edgeLengthDistribution((Set)it.value());
			Distribution.writeHistogram(dDistr.absoluteDistribution(1000), output + "rhoPartitions/distance." + it.key()+".txt");
			Distribution.writeHistogram(dDistr.absoluteDistributionLog2(1000), output + "rhoPartitions/distance.log2" + it.key()+".txt");
		}
		
//		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>();
//		for(Object v : partition) {
//			values.put((SpatialVertex)v, ((Vertex)v).getNeighbours().size());
//		}
//		Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(values, grid, 1000), output + "k_rho.txt", "rho", "<k>");
		Correlations.writeToFile(SpatialGraphStatistics.degreeDensityCorrelation(partition, grid), output + "k_rho.txt", "rho", "<k>");
		
		TDoubleDoubleHashMap correlations = SpatialGraphStatistics.densityCorrelation(SpatialGraphStatistics.meanEdgeLength(partition), grid, 1000);
		Correlations.writeToFile(correlations, output + "d_rho.txt", "rho", "d");
	}
	
	public static <V extends SampledSpatialGraph> void analyze(SampledSpatialGraph graph, SpatialGraph normGraph, String output) throws IOException {
		Set egos = SnowballPartitions.createSampledPartition(graph.getVertices());
		Distribution distr = SpatialGraphStatistics.normalizedEdgeLengthDistribution(egos, normGraph, 1000);
		Distribution.writeHistogram(distr.absoluteDistribution(1000), output + "distance.norm.txt");
		Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), output + "distance.norm.log2.txt");
	}
}

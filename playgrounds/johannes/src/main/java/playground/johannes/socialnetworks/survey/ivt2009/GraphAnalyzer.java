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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseVertex;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.spatial.Reachability;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.ZoneLegacy;
import playground.johannes.socialnetworks.spatial.ZoneLayerLegacy;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;
import playground.johannes.socialnetworks.statistics.Correlations;

import com.vividsolutions.jts.geom.Geometry;

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
		SampledSpatialSparseGraph graph = reader.readGraph(args[0]);
		
		ZoneLayerLegacy zones = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		ZoneLayerDouble density = ZoneLayerDouble.createFromFile(new HashSet<ZoneLegacy>(zones.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.txt");
		TravelTimeMatrix matrix = TravelTimeMatrix.createFromFile(new HashSet<ZoneLegacy>(zones.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.txt");
		
		ZoneLayerLegacy zonesCH = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/g1g08_shp_080606/G1L08.shp");
		analyze(graph, args[1], zonesCH);

		
		
//		SpatialGrid<Double> grid = SpatialGrid.readFromFile(args[2]);
		analyze(graph, density, matrix, args[1]);
		
		Population2SpatialGraph pop2graph = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph g2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.003.xml");
//		analyze(graph, g2, args[1], zonesCH.getZones().iterator().next().getBorder());
		analyze(graph, g2, args[1], zones);
	}

	public static <V extends SampledSpatialSparseGraph> void analyze(SampledSpatialSparseGraph graph, String output, ZoneLayerLegacy zones) throws IOException {
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
			Distribution.writeHistogram(kDistr.absoluteDistribution(3), output + "degree.txt");
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
		Distribution dDistr = SampledGraphStatistics.<SampledSpatialSparseVertex>edgeLenghtDistribution(graph, zones);
		double dMean = dDistr.mean();
		logger.info("<d> = " + dMean);
		if(writer != null) {
			writer.write("d_mean = " + dMean);
			writer.newLine();
			Distribution.writeHistogram(dDistr.absoluteDistribution(1000), output + "distance.txt");
			Distribution.writeHistogram(dDistr.absoluteDistributionLog2(1000), output + "distance.log2.txt");
			Distribution.writeHistogram(dDistr.normalizedDistribution(dDistr.absoluteDistributionLog2(1000)), output + "distance.log2.norm.txt");
			Distribution.writeHistogram(SampledGraphStatistics.edgeLengthDegreeCorrelation((Set)graph.getVertices()), output + "distance_k.txt");
		}
		
		writer.close();
	}
	
	public static <V extends SampledSpatialSparseGraph> void analyze(SampledSpatialSparseGraph graph, ZoneLayerDouble zones, TravelTimeMatrix matrix, String output) throws IOException {
		Set partition = SnowballPartitions.createSampledPartition((Set)graph.getVertices());
		
		TDoubleObjectHashMap<Set<V>> partitions = SpatialGraphStatistics.createDensityPartitions(partition, zones, 2000);
		TDoubleObjectIterator<Set<V>> it = partitions.iterator();
		
//		new File(output + "rhoPartitions").mkdirs();
//		for(int i = 0; i < partitions.size(); i++) {
//			it.advance();
//			Distribution dDistr = SpatialGraphStatistics.edgeLengthDistribution((Set)it.value());
//			Distribution.writeHistogram(dDistr.absoluteDistribution(1000), output + "rhoPartitions/distance." + it.key()+".txt");
//			Distribution.writeHistogram(dDistr.absoluteDistributionLog2(1000), output + "rhoPartitions/distance.log2" + it.key()+".txt");
//		}
		
//		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>();
//		for(Object v : partition) {
//			values.put((SpatialVertex)v, ((Vertex)v).getNeighbours().size());
//		}
//		Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(values, grid, 1000), output + "k_rho.txt", "rho", "<k>");
		Correlations.writeToFile(SpatialGraphStatistics.degreeDensityCorrelation(partition, zones, 500), output + "k_rho.txt", "rho", "<k>");
		logger.info("Calcualting distance reachability...");
		Correlations.writeToFile(Reachability.degreeDistanceReachabilityCorrelation(Reachability.distanceReachability(partition, Reachability.createDistanceReachabilityTable(zones))), output+"k_dreach.txt", "distance_reachability", "k");
		logger.info("Done.");
		TDoubleDoubleHashMap correlations = SpatialGraphStatistics.densityCorrelation(SpatialGraphStatistics.meanEdgeLength(partition), zones, 1000);
		Correlations.writeToFile(correlations, output + "d_rho.txt", "rho", "d");
		
		Distribution.writeHistogram(SpatialGraphStatistics.travelTimeDistribution(partition, zones, matrix).absoluteDistribution(60), output+"traveltime.txt");
		Distribution.writeHistogram(SpatialGraphStatistics.travelTimeDistribution(partition, zones, matrix).absoluteDistributionLog2(60), output+"traveltime.log2.txt");
		
		ZoneLayerDouble reachability = Reachability.createReachablityTable(matrix, zones);
		Correlations.writeToFile(Reachability.degreeCorrelation(partition, reachability), output+"k_reach.txt", "reachability", "k");
		
		TObjectDoubleHashMap<? extends SpatialSparseVertex> values = SpatialGraphStatistics.meanTravelTime(partition, zones, matrix);
		Correlations.writeToFile(GraphStatistics.degreeCorrelation(values), output+"reach_k.txt", "k", "reachability");
		Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(values, zones, 1000), output+"tt_rho.txt", "rho", "<tt>");
		Correlations.writeToFile(Reachability.correlation(values, reachability), output+"<tt>_tt.txt", "<traveltime>", "tt");
		
		Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(
				SpatialGraphStatistics.meanEdgeLength(partition, zones), zones, 1000), output + "distance_rho.txt", "rho", "<d>");
	}
	
	public static <V extends SampledSpatialSparseGraph> void analyze(SampledSpatialSparseGraph graph, SpatialSparseGraph normGraph, String output, ZoneLayerLegacy zones) throws IOException {
		Set egos = SnowballPartitions.createSampledPartition(graph.getVertices());
		Distribution distr = SpatialGraphStatistics.normalizedEdgeLengthDistribution(egos, normGraph, 1000, zones);
		Distribution.writeHistogram(distr.absoluteDistribution(1000), output + "distance.norm.txt");
		Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), output + "distance.norm.log2.txt");
		Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog2(1000)), output + "distance.norm.log2.norm.txt");
	}
}

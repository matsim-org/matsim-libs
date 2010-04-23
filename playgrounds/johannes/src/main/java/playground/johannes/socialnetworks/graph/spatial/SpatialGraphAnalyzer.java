/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkAnalyzer.java
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
package playground.johannes.socialnetworks.graph.spatial;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.GraphAnalyser;
import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.graph.spatial.analysis.Distance;
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
public class SpatialGraphAnalyzer {

	private static final Logger logger = Logger.getLogger(SpatialGraphAnalyzer.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String graphfile = args[0];
		String gridfile = null;
		String output = null;
		boolean extended = false;
		if(args.length > 1) {
			if(args[1].equals("-e"))
				extended = true;
			else
				gridfile = args[1];
			if(args.length > 2) {
				if(args[2].equals("-e"))
					extended = true;
				else
					output = args[2];
				
				if(args.length > 3) {
					if(args[3].equals("-e"))
						extended = true;	
				}
			}
		}
		
		logger.info(String.format("Loading graph %1$s...", graphfile));
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialSparseGraph g = reader.readGraph(graphfile);
		
		if(!output.endsWith("/"))
			output = output + "/";
		
//		ZoneLayer boundary = ZoneLayer.createFromShapeFile("");
//		Geometry geo = boundary.getZones().iterator().next().getBorder();
		Geometry geo = null;
		ZoneLayerLegacy zones = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		ZoneLayerDouble density = ZoneLayerDouble.createFromFile(new HashSet<ZoneLegacy>(zones.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.txt");
		TravelTimeMatrix matrix = null;//TravelTimeMatrix.createFromFile(new HashSet<Zone>(zones.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.txt");
//		if(gridfile != null)
//			grid = SpatialGrid.readFromFile(gridfile);
		
		analyze(g, output, extended, density, matrix, geo);
//		throw new RuntimeException("To be fixed...");
		
	}

	@SuppressWarnings("unchecked")
	public static void analyze(SpatialSparseGraph graph, String output, boolean extended, ZoneLayerDouble zones, TravelTimeMatrix matrix, Geometry boundary) {
		GraphAnalyser.analyze(graph, output, extended);

		double binsize = 1000.0;
//		if(densityGrid != null)
//			binsize = densityGrid.getResolution();
		
		try {
			/*
			 * edge length distribution
			 */
			Distribution edgeLengthDistr = SpatialGraphStatistics.edgeLengthDistribution(graph);
			double d_mean = edgeLengthDistr.mean();
			logger.info("Mean edge length is " + d_mean);	
		
			if(output != null) {
				/*
				 * edge length histogram
				 */
				Distribution.writeHistogram(edgeLengthDistr.absoluteDistribution(binsize), output + "distance.txt");
				Distribution.writeHistogram(edgeLengthDistr.normalizedDistribution(edgeLengthDistr.absoluteDistribution(binsize)), output + "distance.normalized.txt");
				Distribution.writeHistogram(edgeLengthDistr.normalizedDistribution(edgeLengthDistr.absoluteDistributionLog2(1000)), output + "distance.log2.norm.txt");
				
				Distribution.writeHistogram(new Distance().vertexAccumulatedDistribution(graph.getVertices()).absoluteDistribution(1000), output + "accDistance.txt");
				Distribution.writeHistogram(new Distance().vertexAccumulatedCostDistribution(graph.getVertices()).absoluteDistribution(1), output + "accCost.txt");
				/*
				 * degree correlation
				 */
				Correlations.writeToFile(SpatialGraphStatistics.edgeLengthDegreeCorrelation(graph), output + "distance_k.txt", "k", "distance");
				Correlations.writeToFile(GraphStatistics.clusteringDegreeCorrelation(graph), output + "c_k.txt", "k", "c");
				Correlations.writeToFile(SpatialGraphStatistics.degreeCenterDistanceCorrelation(graph.getVertices(), binsize), output + "k_center.txt", "dx", "k");
				Correlations.writeToFile(SpatialGraphStatistics.edgeLengthCenterDistanceCorrelation(graph.getVertices(), binsize), output + "d_center.txt", "dx", "c");
				Correlations.writeToFile(SpatialGraphStatistics.clusteringCenterDistanceCorrelation(graph.getVertices(), binsize), output + "c_center.txt", "dx", "d");
				Correlations.writeToFile(GraphStatistics.degreeCorrelation(SpatialGraphStatistics.meanEdgeLength(graph.getVertices())), output+"<d>_k.txt", "k", "<distance>");
				/*
				 * density correlations
				 */
				if(zones != null) {
					Correlations.writeToFile(SpatialGraphStatistics.degreeDensityCorrelation(
							graph.getVertices(), zones, binsize), output + "k_rho.txt", "density", "k");
					Correlations.writeToFile(SpatialGraphStatistics.clusteringDensityCorrelation(
							graph.getVertices(), zones, binsize), output + "c_rho.txt", "density", "c");
					Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(
							SpatialGraphStatistics.meanEdgeLength(graph), zones, binsize), output + "distance_rho.txt", "rho", "<d>");
				}
				/*
				 * reachablity
				 */
				if(zones != null) {
//					Distribution.writeHistogram(SpatialGraphStatistics.travelTimeDistribution(graph.getVertices(), zones, matrix).absoluteDistribution(60), output+"traveltime.txt");
//					
//					ZoneLayerDouble reachability = Reachability.createReachablityTable(matrix, zones);
//					Correlations.writeToFile(Reachability.degreeCorrelation(graph.getVertices(), reachability), output+"k_reach.txt", "reachability", "k");
//					
//					TObjectDoubleHashMap<? extends SpatialVertex> values = SpatialGraphStatistics.meanTravelTime(graph.getVertices(), zones, matrix);
//					Correlations.writeToFile(GraphStatistics.degreeCorrelation(values), output+"<tt>_k.txt", "k", "<tt>");
//					Correlations.writeToFile(SpatialGraphStatistics.densityCorrelation(values, zones, binsize), output+"<tt>_rho.txt", "rho", "<tt>");
//					Correlations.writeToFile(Reachability.correlation(values, reachability), output+"<tt>_reach.txt", "reachability", "<tt>");
//					
//					Correlations.writeToFile(
//									Reachability.degreeDistanceReachabilityCorrelation(Reachability
//													.distanceReachability(graph.getVertices(),
//															Reachability.createDistanceReachabilityTable(zones))),
//									output + "k_dreach.txt",
//									"distance_reachability", "k");
					
					Distribution distr = SpatialGraphStatistics.normalizedEdgeLengthDistribution(graph.getVertices(), graph, 1000, zones);
					Distribution.writeHistogram(distr.absoluteDistribution(1000), output + "distance.norm.txt");
					Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog2(1000)), output + "distance.norm.log2.norm.txt");
//					Distribution.writeHistogram(Reachability.normalizedTravelTimeDistribution(graph.getVertices(), zones, matrix).absoluteDistribution(60), output+"traveltime.norm.txt");
				}
				/*
				 * degree partitions
				 */
				TDoubleObjectHashMap<?> kPartitions = Partitions.createDegreePartitions(graph.getVertices());
				TDoubleObjectIterator<?> it = kPartitions.iterator();
				String patitionOutput = output + "/kPartitions"; 
				new File(patitionOutput).mkdirs();
				for(int i = 0; i < kPartitions.size(); i++) {
					it.advance();
					String filename = String.format("%1$s/edgelength.%2$s.hist.txt", patitionOutput, (int)it.key());
					Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution((Set<SpatialSparseVertex>)it.value()).absoluteDistribution(binsize), filename);
				}
				/*
				 * density partitions
				 */
				if(zones != null) {
				TDoubleObjectHashMap<?> rhoPartitions = SpatialGraphStatistics.createDensityPartitions(graph.getVertices(), zones, binsize);
				it = rhoPartitions.iterator();
				patitionOutput = output + "/rhoPartitions"; 
				new File(patitionOutput).mkdirs();
				for(int i = 0; i < rhoPartitions.size(); i++) {
					it.advance();
					String filename = String.format("%1$s/edgelength.%2$s.hist.txt", patitionOutput, (int)it.key());
					Distribution.writeHistogram(SpatialGraphStatistics.edgeLengthDistribution((Set<SpatialSparseVertex>)it.value()).absoluteDistribution(binsize), filename);
				}
				}
			}
			
			if (output != null) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(output + GraphAnalyser.SUMMARY_FILE, true));
				writer.write("mean edge length=");
				writer.write(Double.toString(d_mean));
				writer.newLine();
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

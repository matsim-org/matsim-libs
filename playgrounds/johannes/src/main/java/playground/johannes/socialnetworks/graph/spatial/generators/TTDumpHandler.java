/* *********************************************************************** *
 * project: org.matsim.*
 * TTDumpHandler.java
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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;
import playground.johannes.socialnetworks.statistics.Distribution;



/**
 * @author illenberger
 *
 */
//public class TTDumpHandler extends DumpHandler {
//
//	private TravelTimeMatrix matrix;
//	
//	
//	/**
//	 * @param filename
//	 * @param zones
//	 */
//	public TTDumpHandler(String filename, ZoneLayerDouble zones, TravelTimeMatrix matrix) {
//		super(filename, zones);
//		this.matrix = matrix;
//	}
//
//	@Override
//	protected SpatialGraph dump(AdjacencyMatrix y, long iteration, TravelTimeMatrix dummy) {
//		SpatialGraph net = super.dump(y, iteration, matrix);
//		
////		String currentOutputDir = String.format("%1$s%2$s/", outputDir, iteration);
////		Distribution distr = SpatialGraphStatistics.travelTimeDistribution(net.getVertices(), zones, matrix);
////		try {
////			Distribution.writeHistogram(distr.absoluteDistribution(60), currentOutputDir + "traveltime.txt");
////			Distribution.writeHistogram(distr.absoluteDistributionLog2(60), currentOutputDir + "traveltime.log2.txt");
////		} catch (FileNotFoundException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		} catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//		return net;
//	}

//}

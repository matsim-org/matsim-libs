/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.math.Distribution;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class TravelTimeTask extends AnalyzerTask {

	private final Network network;
	
	private final LeastCostPathCalculator router;
	
	private MathTransform transform;
	
	public TravelTimeTask(Network network) {
		this.network = network;
		
		FreespeedTravelTimeAndDisutility freeSpeedTT = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
		router = new Dijkstra(network, freeSpeedTT, freeSpeedTT);
		
		
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		SpatialGraph graph = (SpatialGraph) g;
		
		try {
			transform = CRS.findMathTransform(graph.getCoordinateReferenceSysten(), CRSUtils.getCRS(21781));
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Distribution distr = new Distribution();
		
		for(SpatialEdge edge : graph.getEdges()) {
			SpatialVertex v1 = edge.getVertices().getFirst();
			SpatialVertex v2 = edge.getVertices().getSecond();
			
			double[] points1 = new double[] { v1.getPoint().getX(), v1.getPoint().getY() };
			double[] points2 = new double[] { v2.getPoint().getX(), v2.getPoint().getY() };
			try {
				transform.transform(points1, 0, points1, 0, 1);
				transform.transform(points2, 0, points2, 0, 1);
			} catch (TransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Node n1 = NetworkUtils.getNearestNode(((Network)network),new Coord(points1[0], points1[1]));
			Node n2 = NetworkUtils.getNearestNode(((Network)network),new Coord(points2[0], points2[1]));
			
			Path path = router.calcLeastCostPath(n1, n2, 0, null, null);
			
			distr.add(path.travelCost);
		}
		
		try {
			Distribution.writeHistogram(distr.absoluteDistribution(60.0), getOutputDirectory() + "/traveltime.txt");
			Distribution.writeHistogram(distr.absoluteDistributionLog2(60.0), getOutputDirectory() + "/traveltime.log2.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

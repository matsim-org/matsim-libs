/* *********************************************************************** *
 * project: org.matsim.*
 * TraveTimeCostFunction.java
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
package playground.johannes.studies.gis;

import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.socnetgen.socialnetworks.gis.SpatialCostFunction;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import playground.johannes.studies.gis.SpanningTree.NodeData;

import java.util.Map;

/**
 * @author illenberger
 *
 */
public class TraveTimeCostFunction implements SpatialCostFunction {

	private NetworkImpl network;
	
	private SpanningTree spanningTree;
	
	private Node lastNode;
	
	private Map<Node, NodeData> nodeData;
	
//	private Discretizer discretizer;
	
	public TraveTimeCostFunction(NetworkImpl network, double beta) {
		this.network = network;
//		double binsize = 60.0;
		FreespeedTravelTimeAndDisutility cost = new FreespeedTravelTimeAndDisutility(beta, 0.0, 0.0);
		spanningTree = new SpanningTree(cost, cost);
//		discretizer = new LinearDiscretizer(binsize);
	}
	
	@Override
	public double costs(Point p1, Point p2) {
		Node start = network.getNearestNode(new Coord(p1.getX(), p1.getY()));
		Node target = network.getNearestNode(new Coord(p2.getX(), p2.getY()));
		
		if(start != lastNode) {
			spanningTree.setOrigin(start);
			spanningTree.setDepartureTime(0.0);
			spanningTree.run(network);
			nodeData = spanningTree.getTree();
			
			lastNode = start;
		}

		double cost = nodeData.get(target).getCost();
		return cost;
//		return discretizer.index(cost);
	}

}

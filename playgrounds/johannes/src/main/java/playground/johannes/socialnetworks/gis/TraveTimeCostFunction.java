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
package playground.johannes.socialnetworks.gis;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.studies.gis.SpanningTree;
import playground.johannes.studies.gis.SpanningTree.NodeData;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class TraveTimeCostFunction implements SpatialCostFunction {

	private NetworkImpl network;
	
	private SpanningTree spanningTree;
	
	private Node lastNode;
	
	private Map<Id, NodeData> nodeData;
	
//	private Discretizer discretizer;
	
	public TraveTimeCostFunction(NetworkImpl network, double beta) {
		this.network = network;
//		double binsize = 60.0;
		FreespeedTravelTimeCost cost = new FreespeedTravelTimeCost(beta, 0.0, 0.0);
		spanningTree = new SpanningTree(cost, cost);
//		discretizer = new LinearDiscretizer(binsize);
	}
	
	@Override
	public double costs(Point p1, Point p2) {
		Node start = network.getNearestNode(new CoordImpl(p1.getX(), p1.getY()));
		Node target = network.getNearestNode(new CoordImpl(p2.getX(), p2.getY()));
		
		if(start != lastNode) {
			spanningTree.setOrigin(start);
			spanningTree.setDepartureTime(0.0);
			spanningTree.run(network);
			nodeData = spanningTree.getTree();
			
			lastNode = start;
		}
		
		double cost = nodeData.get(target.getId()).getCost();
		return cost;
//		return discretizer.index(cost);
	}

}

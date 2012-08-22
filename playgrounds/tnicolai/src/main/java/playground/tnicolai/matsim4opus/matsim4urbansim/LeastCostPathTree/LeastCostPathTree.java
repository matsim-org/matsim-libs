/* *********************************************************************** *
 * project: org.matsim.*
 * SpanningTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.tnicolai.matsim4opus.matsim4urbansim.LeastCostPathTree;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.omg.CORBA.TCKind;

import com.vividsolutions.jts.geom.Point;

import playground.tnicolai.matsim4opus.utils.helperObjects.Distances;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

/**
 * Calculates a least-cost-path tree using Dijkstra's algorithm for calculating a shortest-path
 * tree, given a node as root of the tree.
 * 
 * @author balmermi, mrieser, kai, thomas
 */
public class LeastCostPathTree {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	private Node origin = null;
	private double dTime = Time.UNDEFINED_TIME;
	
	private final TravelTime ttFunction;
	private final TravelDisutility tcFunction;
	private HashMap<Id, NodeData> nodeData = null;
	private GeneralizedCostSumV2 gcs;

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	public LeastCostPathTree(TravelTime tt, TravelDisutility tc) {
		this.ttFunction = tt;
		this.tcFunction = tc;
		this.gcs 		= new GeneralizedCostSumV2();
	}

	public void calculate(final Network network, final Node origin, final double time) {
		this.origin = origin;
		this.dTime = time;
		this.gcs.reset();
		
		this.nodeData = new HashMap<Id, NodeData>((int) (network.getNodes().size() * 1.1), 0.95f);
		NodeData d = new NodeData();
		d.time = time;
		d.cost = 0;
		this.nodeData.put(origin.getId(), d);

		ComparatorCost comparator = new ComparatorCost(this.nodeData);
		PriorityQueue<Node> pendingNodes = new PriorityQueue<Node>(500, comparator);
		relaxNode(origin, pendingNodes);
		while (!pendingNodes.isEmpty()) {
			Node n = pendingNodes.poll();
			relaxNode(n, pendingNodes);
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// get methods
	// ////////////////////////////////////////////////////////////////////

	public final Map<Id, NodeData> getTree() {
		return this.nodeData;
	}

	/**
	 * @return Returns the root of the calculated tree, or <code>null</code> if no tree was calculated yet.
	 */
	public final Node getOrigin() {
		return this.origin;
	}

	public final double getDepartureTime() {
		return this.dTime;
	}

	// ////////////////////////////////////////////////////////////////////
	// private methods
	// ////////////////////////////////////////////////////////////////////

	private void relaxNode(final Node n, PriorityQueue<Node> pendingNodes) {
		NodeData nData = nodeData.get(n.getId());
		double currTime = nData.getTime();
		double currCost = nData.getCost();
		for (Link l : n.getOutLinks().values()) {
			Node nn = l.getToNode();
			NodeData nnData = nodeData.get(nn.getId());
			if (nnData == null) {
				nnData = new NodeData();
				this.nodeData.put(nn.getId(), nnData);
			}
			double visitCost = currCost + tcFunction.getLinkTravelDisutility(l, currTime, null, null);
			double visitTime = currTime + ttFunction.getLinkTravelTime(l, currTime);
			double visitGeneralizedCost = 0.; // tnicolai todo
			if (visitCost < nnData.getCost()) {
				pendingNodes.remove(nn);
				nnData.visit(n.getId(), visitCost, visitTime, visitGeneralizedCost);
				pendingNodes.add(nn);
			}
		}
	}
	
	// ////////////////////////////////////////////////////////////////////
	// inner classes
	// ////////////////////////////////////////////////////////////////////

	public static class NodeData {
		private Id prevId = null;
		private double cost = Double.MAX_VALUE;
		private double time = 0;
		private double generalizedCost = 0.;

		/*package*/ void reset() {
			this.prevId = null;
			this.cost = Double.MAX_VALUE;
			this.time = 0;
			this.generalizedCost = 0.;
		}

		/*package*/ void visit(final Id comingFromNodeId, final double cost, final double time, final double generalizedCost) {
			this.prevId = comingFromNodeId;
			this.cost = cost;
			this.time = time;
			this.generalizedCost = generalizedCost;
		}

		public double getCost() {
			return this.cost;
		}

		public double getTime() {
			return this.time;
		}

		public Id getPrevNodeId() {
			return this.prevId;
		}
		
		public double getGeneralizedCost() {
			return this.generalizedCost;
		}
	}

	
	
	/*package*/ static class ComparatorCost implements Comparator<Node> {
		protected Map<Id, ? extends NodeData> nodeData;

		ComparatorCost(final Map<Id, ? extends NodeData> nodeData) {
			this.nodeData = nodeData;
		}

		@Override
		public int compare(final Node n1, final Node n2) {
			double c1 = getCost(n1);
			double c2 = getCost(n2);
			if (c1 < c2)
				return -1;
			if (c1 > c2)
				return +1;
			return n1.getId().compareTo(n2.getId());
		}

		protected double getCost(final Node node) {
			return this.nodeData.get(node.getId()).getCost();
		}
	}


	
	
	public static class GeneralizedCostSumV2 {
		
		private double sumFREESPEED;
		private double sumCAR;
		private double sumBIKE;
		private double sumWALK;
		
		public GeneralizedCostSumV2(){
			
//			Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode);
//			
//			double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
//			double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
//			
//			double offsetWalkTime2Node_h 				= distanceMeasuringPoint2Road_meter / walkSpeedMeterPerHour;
//			double carTravelTime_meterpersec			= nearestLink.getLength() / tt.getLinkTravelTime(nearestLink, depatureTime);
//			
//			double freeSpeedTravelTime_meterpersec 		= nearestLink.getFreespeed();
//			
//			double offsetFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTime_meterpersec * 3600);
//			double offsetCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTime_meterpersec * 3600.);
//			double offsetBikeTime_h						= distanceRoad2Node_meter / bikeSpeedMeterPerHour;
		}
		
		public void reset() {
			this.sumFREESPEED = 0.;
			this.sumCAR		  = 0.;
			this.sumBIKE	  = 0.;
			this.sumWALK	  = 0.;
		}
		
		public void addFreeSpeedCost(double cost){
			this.sumFREESPEED += cost;
		}
		
		public void addCongestedCarCost(double cost){
			this.sumCAR += cost;
		}
		
		public void addBikeCost(double cost){
			this.sumBIKE += cost;
		}
		
		public void addWalkCost(double cost){
			this.sumWALK += cost;
		}
		
		public double getFreeSpeedSum(){
			return this.sumFREESPEED;
		}
		
		public double getCarSum(){
			return this.sumCAR;
		}
		
		public double getBikeSum(){
			return this.sumBIKE;
		}
		
		public double getWalkSum(){
			return this.sumWALK;
		}
	}
}

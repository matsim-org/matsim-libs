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

package org.matsim.utils.leastcostpathtree;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * Calculates a least-cost-path tree using Dijkstra's algorithm  for calculating a shortest-path
 * tree, given a node as root of the tree.
 *
 * 
 * @author balmermi, mrieser
 */
public class LeastCostPathTree {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	private Node origin1 = null;
	private double dTime = Double.NaN;
	
	private final TravelTime ttFunction;
	private final TravelDisutility tcFunction;
	private IdMap<Node, NodeData> nodeData = null;
	
	private final Vehicle VEHICLE = VehicleUtils.getFactory().createVehicle(Id.create("theVehicle", Vehicle.class), VehicleUtils.createDefaultVehicleType());
	private final Person PERSON = PopulationUtils.getFactory().createPerson(Id.create("thePerson", Person.class));

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	public LeastCostPathTree(TravelTime tt, TravelDisutility tc) {
		this.ttFunction = tt;
		this.tcFunction = tc;
	}

	public void calculate(final Network network, final Node origin, final double time) {
		this.origin1 = origin;
		this.dTime = time;
		
		this.nodeData = new IdMap<>(Node.class);
		NodeData d = new NodeData();
		d.time = time;
		d.cost = 0;
		this.nodeData.put(origin.getId(), d);

		ComparatorCost comparator = new ComparatorCost(this.nodeData);
		PriorityQueue<Node> pendingNodes = new PriorityQueue<>(500, comparator);
		relaxNode(origin, pendingNodes);
		while (!pendingNodes.isEmpty()) {
			Node n = pendingNodes.poll();
			relaxNode(n, pendingNodes);
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// inner classes
	// ////////////////////////////////////////////////////////////////////

	public static class NodeData {
		private Id<Node> prevId = null;
		private double cost = Double.MAX_VALUE;
		private double time = 0;

        /*package*/ void visit(final Id<Node> comingFromNodeId, final double cost1, final double time1) {
			this.prevId = comingFromNodeId;
			this.cost = cost1;
			this.time = time1;
		}

		public double getCost() {
			return this.cost;
		}

		public double getTime() {
			return this.time;
		}

		public Id<Node> getPrevNodeId() {
			return this.prevId;
		}
	}

	/*package*/ static class ComparatorCost implements Comparator<Node> {
		protected Map<Id<Node>, ? extends NodeData> nodeData;

		ComparatorCost(final IdMap<Node, ? extends NodeData> nodeData) {
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

	// ////////////////////////////////////////////////////////////////////
	// get methods
	// ////////////////////////////////////////////////////////////////////

	public final IdMap<Node, NodeData> getTree() {
		return this.nodeData;
	}

	/**
	 * @return Returns the root of the calculated tree, or <code>null</code> if no tree was calculated yet.
	 */
	public final Node getOrigin() {
		return this.origin1;
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
			double visitCost = currCost + tcFunction.getLinkTravelDisutility(l, currTime, PERSON, VEHICLE);
			double visitTime = currTime + ttFunction.getLinkTravelTime(l, currTime, PERSON, VEHICLE);
			
			
			if (visitCost < nnData.getCost()) {
				pendingNodes.remove(nn);
				nnData.visit(n.getId(), visitCost, visitTime);
				additionalComputationsHook( l, currTime ) ;
				pendingNodes.add(nn);
			}
		}
	}

	protected void additionalComputationsHook( Link link, double currTime ) {
		// left empty for inheritance
	}

	// ////////////////////////////////////////////////////////////////////
	// main method
	// ////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../input/network.xml");

		TravelTimeCalculator ttc = new TravelTimeCalculator(network, 60, 30 * 3600, scenario.getConfig().travelTimeCalculator());
		LeastCostPathTree st = new LeastCostPathTree(ttc.getLinkTravelTimes(), new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, scenario.getConfig() ).createTravelDisutility(ttc.getLinkTravelTimes()));
		Node origin = network.getNodes().get(Id.create(1, Node.class));
		st.calculate(network, origin, 8*3600);
		IdMap<Node, NodeData> tree = st.getTree();
		for (Map.Entry<Id<Node>, NodeData> e : tree.entrySet()) {
			Id<Node> id = e.getKey();
			NodeData d = e.getValue();
			if (d.getPrevNodeId() != null) {
				System.out.println(id + "\t" + d.getTime() + "\t" + d.getCost() + "\t" + d.getPrevNodeId());
			} else {
				System.out.println(id + "\t" + d.getTime() + "\t" + d.getCost() + "\t" + "0");
			}
		}
	}
}

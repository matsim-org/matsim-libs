/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.workbench;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.misc.Counter;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.*;

/**
 * A Class to run Tarjan's strongly connected
 * components algorithm on a rail network.
 *
 * https://en.wikipedia.org/wiki/Tarjan's_strongly_connected_components_algorithm
 *
 * Note: does not really work
 */
public class TarjanNetworkCleaner {

	protected static Logger log = Logger.getLogger(TarjanNetworkCleaner.class);

	public static void main(String[] args) {
		Network network = NetworkTools.loadNetwork("C:/Users/Flavio/Desktop/data/network/multimodal/zurich-plus-mm.xml.gz");

		network = NetworkTools.filterNetworkByLinkMode(network, Collections.singleton("rail"));
//		new NetworkCleaner().run(network);
//		NetworkTools.writeNetwork(network, "C:/Users/Flavio/Desktop/data/tarjan/rail_cleaned.xml.gz");

		new TarjanNetworkCleaner(network).run();

		NetworkTools.writeNetwork(network, "C:/Users/Flavio/Desktop/data/tarjan/final.xml.gz");
	}

	private final Network network;
	private int index = 0;
	private final Map<Id<Link>, Vertex> vertices = new HashMap<>();
	private final Stack<Vertex> stack = new Stack<>();
	private List<List<Vertex>> components = new ArrayList<>();
	private int maxLvl = Integer.MAX_VALUE;

	public TarjanNetworkCleaner(Network network) {
		this.network = network;

		for(Link link : network.getLinks().values()) {
			vertices.put(link.getId(), new Vertex(link));
		}
	}


	public void run() {
		Counter counter = new Counter("link # ");
		for(Vertex v : vertices.values()) {
			counter.incCounter();
			if(v.index == null) {
				strongconnect(v);
			}
		}

		int removed = 0;
		log.info("creating Network...");
		double visIndex = 0;
		for(List<Vertex> c : components) {
			for(Vertex vv : c) {
				this.network.getLinks().get(vv.link.getId()).setNumberOfLanes(visIndex);
			}
			visIndex++;
			/*
			Set<Node> nodesCompletelyInsideComponent = getNodesCompletelyInsideComponent(c);
			for(Vertex v : c) {
				if(nodesCompletelyInsideComponent.contains(v.link.getFromNode()) &&
						nodesCompletelyInsideComponent.contains(v.link.getToNode())) {
					network.removeLink(v.link.getId());
					removed++;
				}
			} */
		}
		log.info(removed + " links removed.");
	}

	public void strongconnect(Vertex v) {
		v.index = index;
		v.lowlink = index;
		index++;
		stack.push(v);
		v.onStack = true;

		// Consider successors of v
		for(Id<Link> wId : v.getSuccessors()) {
			Vertex w = vertices.get(wId);
			if(w.index == null) {
				strongconnect(w);
				v.lowlink = Integer.min(v.lowlink, w.lowlink);
			} else if(w.onStack) {
				v.lowlink = Integer.min(v.lowlink, w.index);
			}
		}

		// If v is a root node, pop the stack and generate an SCC
		if(v.lowlink.equals(v.index)) {
			// start a new strongly connected component
			List<Vertex> newComponent = new ArrayList<>();
			while(true) {
				Vertex x = stack.pop();
				newComponent.add(x);
				x.onStack = false;
				if(x == v)
					break;
			}
			components.add(newComponent);
		}
	}

	/**
	 * might not work
	 */
	private Set<Node> getNodesCompletelyInsideComponent(List<Vertex> list) {
		Set<Node> returnNodes = new HashSet<>();
		Set<Node> nodes = new HashSet<>();
		List<Link> linkList = new ArrayList<>();
		for(Vertex v : list) {
			nodes.add(v.link.getFromNode());
			linkList.add(v.link);
		}

		for(Node n : nodes) {
			boolean outsideConnection = false;
			for(Link l : n.getInLinks().values()) {
				if(!linkList.contains(l)) {
					outsideConnection = true;
					break;
				}
			}
			for(Link l : n.getOutLinks().values()) {
				if(!linkList.contains(l)) {
					outsideConnection = true;
					break;
				}
			}
			if(!outsideConnection) {
				returnNodes.add(n);
			}
		}
		return returnNodes;
	}

	/**
	 * helper container class
	 */
	private class Vertex {

		private final Link link;

		public Integer index = null;
		public Integer lowlink;
		public boolean onStack = false;

		public final Set<Id<Link>> successors = new HashSet<>();

		public Vertex(Link link) {
			this.link = link;

			for(Link outlink : link.getToNode().getOutLinks().values()) {
				if(!outlink.getToNode().equals(link.getFromNode())) {
					successors.add(outlink.getId());
				}
			}
		}

		public Set<Id<Link>> getSuccessors() {
			return successors;
		}
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * AStar.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2denvironment.approxdecomp;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2denvironment.approxdecomp.Graph.Link;
import playground.gregor.sim2denvironment.approxdecomp.Graph.Node;


public class ShortestPath {

	//straight forward Dijkstra implementation
	public Tuple<Node, Double> getFarestNode(Node from) {
		Set<Node> closed = new HashSet<Node>();

		Map<Node,Double> gScore = new HashMap<Node,Double>();

		gScore.put(from, 0.);

		Queue<Node> open = new PriorityQueue<Node>(10,new NodeComparator(gScore));
		open.add(from);

		while (open.size() > 0) {
			Node current = open.poll();
			if (closed.contains(current)) {
				continue;
			}
			if (open.peek() == null) {
				boolean done = true;
				for (Link l : current.outLinks) {
					Node neighbor = l.n1;
					if (!closed.contains(neighbor)) {
						done = false;
					}	
				}
				if (done) {
					return new Tuple<Node,Double>(current,gScore.get(current));
				}
			}
			closed.add(current);
			for (Link l : current.outLinks) {
				Node neighbor = l.n1;
				if (closed.contains(neighbor)) {
					continue;
				}
				double tentativeGScore = gScore.get(current) + l.length;
				Double last = gScore.get(neighbor);
				if (last == null || tentativeGScore < last) {
					gScore.put(neighbor, tentativeGScore);
					open.add(neighbor);
				}
			}
		}

		return null;
	}

	//straight forward AStar implementation
	public double getCost(Node from, Node to) {
		Set<Node> closed = new HashSet<Node>();

		Map<Node,Double> gScore = new HashMap<Node,Double>();
		HashMap<Node, Double> fScore = new HashMap<Node,Double>();

		gScore.put(from, 0.);
		fScore.put(from,from.c.distance(to.c));

		Queue<Node> open = new PriorityQueue<Node>(10,new NodeComparator(fScore));
		open.add(from);

		while (open.size() > 0) {
			Node current = open.poll();
			if (closed.contains(current)) {
				continue;
			}
			if (current == to) {
				return gScore.get(current);
			}
			closed.add(current);
			for (Link l : current.outLinks) {
				Node neighbor = l.n1;
				if (closed.contains(neighbor)) {
					continue;
				}
				double tentativeGScore = gScore.get(current) + l.length;
				Double last = gScore.get(neighbor);
				if (last == null || tentativeGScore < last) {
					gScore.put(neighbor, tentativeGScore);
					fScore.put(neighbor, tentativeGScore+neighbor.c.distance(to.c));
					open.add(neighbor);
				}
			}
		}

		return 0;
	}

	private static class NodeComparator implements Comparator<Node> {


		private final Map<Node, Double> fScore;

		public NodeComparator(Map<Node, Double> fScore) {
			this.fScore = fScore;
		}

		@Override
		public int compare(Node o1, Node o2) {
			double f1 = this.fScore.get(o1);
			double f2 = this.fScore.get(o2);
			if (f1 < f2) {
				return -1;
			} 
			if (f2 < f1) {
				return 1;
			}
			return 0;
		}

	}
}

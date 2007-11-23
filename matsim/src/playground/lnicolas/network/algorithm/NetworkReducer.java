/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReducer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.network.algorithm;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.basic.v01.IdSet;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.world.Coord;

public class NetworkReducer extends NetworkAlgorithm {

	Coord startCoord = null;

	int nOfLinks = -1;

	public NetworkReducer(double xCenter, double yCenter, int nOfLinks) {
		startCoord = new Coord(xCenter, yCenter);
		this.nOfLinks = nOfLinks;
	}

	private Node getStartNode(NetworkLayer network) {
		double dist = Double.MAX_VALUE;
		Node startNode = null;
		for (Object obj : network.getNodes()) {
			Node n = (Node) obj;
			double d = n.getCoord().calcDistance(startCoord);
			if (d < dist) {
				dist = d;
				startNode = n;
			}
		}

		return startNode;
	}

	@Override
	public void run(NetworkLayer network) {
		Node node = getStartNode(network);

		int roleIndex = network.requestNodeRole();

		markNodes(network, roleIndex, node);

		reduceNetwork(network, roleIndex);
	}

	private void reduceNetwork(NetworkLayer network, int roleIndex) {
		IdSet allNodesCopy = new IdSet();
		allNodesCopy.addAll(network.getNodes());
		Iterator it = allNodesCopy.iterator();
		while (it.hasNext()) {
			Node n = (Node) it.next();
			NetworkReducerRole r = getRole(n, roleIndex);

			if (r.isInResultingNetwork() == false) {
				network.removeNode(n);
			}
		}
	}

	private void markNodes(NetworkLayer network, int roleIndex, Node startNode) {
		ArrayList<Node> pendingNodes = new ArrayList<Node>();

		pendingNodes.add(startNode);

		int linkCount = 0;

		while (pendingNodes.isEmpty() == false && linkCount < nOfLinks) {
			Node node = pendingNodes.remove(0);
			linkCount = processLinks(node.getOutLinks(), roleIndex, linkCount, pendingNodes);
			linkCount = processLinks(node.getInLinks(), roleIndex, linkCount, pendingNodes);
		}
	}

	private int processLinks(BasicLinkSetI links, int roleIndex, int linkCount,
			ArrayList<Node> pendingNodes) {
		Iterator iter = links.iterator();
		while (iter.hasNext() && linkCount < nOfLinks) {
			Link l = (Link) iter.next();
			Node n = l.getToNode();
			NetworkReducerRole r = getRole(n, roleIndex);
			if (r.isInResultingNetwork() == false) {
				pendingNodes.add(n);
				r.setIsInResultingNetwork(true);
				linkCount++;
			}
		}
		return linkCount;
	}

	public Rectangle2D.Double getBoundingBox(NetworkLayer network, int roleIndex) {

		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		Iterator it = network.getNodes().iterator();
		while (it.hasNext()) {
			Node n = (Node) it.next();
			NetworkReducerRole r = getRole(n, roleIndex);

			if (r.isInResultingNetwork() == true) {
				Coord c = n.getCoord();
				if (c.getX() < minX) {
					minX = c.getX();
				}
				if (c.getX() > maxX) {
					maxX = c.getX();
				}
				if (c.getY() < minY) {
					minY = c.getY();
				}
				if (c.getY() > maxY) {
					maxY = c.getY();
				}
			}
		}

		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	/**
	 * Returns the role for the given Node. Creates a new Role if none exists
	 * yet.
	 *
	 * @param n
	 *            The Node for which to create a role.
	 * @return The role for the given Node
	 */
	NetworkReducerRole getRole(Node n, int roleIndex) {
		NetworkReducerRole r = (NetworkReducerRole) n.getRole(roleIndex);
		if (null == r) {
			r = new NetworkReducerRole();
			n.setRole(roleIndex, r);
		}
		return r;
	}

	class NetworkReducerRole {
		boolean isInResultingNetwork = false;

		/**
		 * @return the isInResultingNetwork
		 */
		public boolean isInResultingNetwork() {
			return isInResultingNetwork;
		}

		/**
		 * @param isInResultingNetwork
		 *            the isInResultingNetwork to set
		 */
		public void setIsInResultingNetwork(boolean isInResultingNetwork) {
			this.isInResultingNetwork = isInResultingNetwork;
		}
	}
}

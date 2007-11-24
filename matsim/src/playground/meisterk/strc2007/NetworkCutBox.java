/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCutBox.java
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

package playground.meisterk.strc2007;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;

public class NetworkCutBox extends NetworkAlgorithm {

	private NetworkLayer networkCut = null;
	private double north;
	private double south;
	private double west;
	private double east;

	public NetworkCutBox(double north, double south, double west, double east) {
		super();
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
		this.networkCut = new NetworkLayer();
	}

	@Override
	public void run(NetworkLayer network) {

		org.matsim.utils.geometry.shared.Coord nodeCoord = null;

		// add all nodes that are in the box to the box network
		for (Node node : network.getNodes().values()) {
			nodeCoord = node.getCoord();
			if (
					(nodeCoord.getX() > this.west) &&
					(nodeCoord.getX() < this.east) &&
					(nodeCoord.getY() > this.south) &&
					(nodeCoord.getY() < this.north)) {

				this.networkCut.createNode(
						node.getId().toString(),
						Double.toString(nodeCoord.getX()),
						Double.toString(nodeCoord.getY()),
						node.getType());

			}
		}

		// add all adjacent links to the box network
		for (Link link : network.getLinks().values()) {
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
			if (
					(this.networkCut.getNode(fromNode.getId().toString()) != null) ||
					(this.networkCut.getNode(toNode.getId().toString()) != null)) {

				// add nodes if they are not yet in it
				if (this.networkCut.getNode(fromNode.getId().toString()) == null) {

					nodeCoord = fromNode.getCoord();
					this.networkCut.createNode(
							fromNode.getId().toString(),
							Double.toString(nodeCoord.getX()),
							Double.toString(nodeCoord.getY()),
							fromNode.getType());

				}

				// add nodes if they are not yet in it
				if (this.networkCut.getNode(toNode.getId().toString()) == null) {

					nodeCoord = toNode.getCoord();
					this.networkCut.createNode(
							toNode.getId().toString(),
							Double.toString(nodeCoord.getX()),
							Double.toString(nodeCoord.getY()),
							toNode.getType());

				}

				// add link
				this.networkCut.createLink(
						link.getId().toString(),
						fromNode.getId().toString(),
						toNode.getId().toString(),
						Double.toString(link.getLength()),
						Double.toString(link.getFreespeed()),
						Double.toString(link.getCapacity()),
						Integer.toString(link.getLanes()),
						link.getOrigId(),
						link.getType());

			}
		}
	}

	public NetworkLayer getNetworkCut() {
		return networkCut;
	}

}

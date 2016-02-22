/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.map2mapmatching.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainterManager;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkTwoNodesPainterManager extends NetworkPainterManager {

	private static final String SEPARATOR = " & ";
	//Attributes
	private final List<Id<Node>> selectedNodesId;
	private final List<Link> selectedLinks;
	
	//Methods
	public NetworkTwoNodesPainterManager(Network network) {
		super(network);
		selectedNodesId = new ArrayList<Id<Node>>();
		selectedLinks = new ArrayList<Link>();
	}
	public Set<Node> getSelectedNodes() {
		Set<Node> selectedNodes = new HashSet<Node>();
		for(Id<Node> selectedNodeId:selectedNodesId)
			selectedNodes.add(network.getNodes().get(selectedNodeId));
		return selectedNodes;
	}
	public List<Link> getSelectedLinks() {
		return selectedLinks;
	}
	public void selectLinks(Dijkstra dijkstra) {
		selectedLinks.clear();
		if(selectedNodesId.size()==2) {
			Path path=dijkstra.calcLeastCostPath(network.getNodes().get(selectedNodesId.get(0)), network.getNodes().get(selectedNodesId.get(1)), 0, null, null);
			for(Link link:path.links)
				selectedLinks.add(link);
		}
	}
	public void selectNearestNode(double x, double y) {
		if(selectedNodesId.size()<2)
			selectedNodesId.add(getIdNearestNode(x, y));
	}
	public void unselectNearestNode(double x, double y) {
		if(selectedNodesId.size()>0)
			selectedNodesId.remove(getIdNearestSelectedNode(x, y));
	}
	private Id<Node> getIdNearestSelectedNode(double x, double y) {
		Coord coord = new Coord(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Id<Node> nodeId:selectedNodesId) {
			Node node = network.getNodes().get(nodeId);
			double distance = CoordUtils.calcEuclideanDistance(coord, node.getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = node;
			}
		}
		return nearest.getId();
	}
	public String refreshNodes() {
		String text = "";
		for(Id<Node> selectedNodeId:selectedNodesId)
			text += selectedNodeId.toString()+SEPARATOR;
		return text;
	}
	public void clearNodesSelection() {
		selectedNodesId.clear();
	}
	public void clearLinksSelection() {
		selectedLinks.clear();
	}

}

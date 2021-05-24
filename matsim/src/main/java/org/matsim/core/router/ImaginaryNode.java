/* *********************************************************************** *
 * project: org.matsim.*
 * ImaginaryNode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * Used by the MultiNodeDijkstra for backwards compatibility with default Dijkstra. 
 * 
 * @see org.matsim.core.router.Dijkstra
 * @see org.matsim.core.router.MultiNodeDijkstra
 * @author cdobler
 */
public class ImaginaryNode implements Node {

	/*package*/ final Collection<? extends InitialNode> initialNodes;
	/*package*/ final Coord coord;
	
	 ImaginaryNode(Collection<? extends InitialNode> initialNodes, Coord coord) {
		this.initialNodes = initialNodes;
		this.coord = coord;
	}
	
	 ImaginaryNode(Collection<? extends InitialNode> initialNodes) {
		this.initialNodes = initialNodes;
		
		double sumX = 0.0;
		double sumY = 0.0;
		
		for (InitialNode initialNode : initialNodes) {
			sumX += initialNode.node.getCoord().getX();
			sumY += initialNode.node.getCoord().getY();
		}
		
		sumX /= initialNodes.size();
		sumY /= initialNodes.size();

		this.coord = new Coord(sumX, sumY);
	}
	
	@Override
	public Coord getCoord() {
		return this.coord;
	}

	@Override
	public Id<Node> getId() {
		return null;
	}

	@Override
	public boolean addInLink(Link link) {
		return false;
	}

	@Override
	public boolean addOutLink(Link link) {
		return false;
	}

	@Override
	public Map<Id<Link>, ? extends Link> getInLinks() {
		return null;
	}

	@Override
	public Map<Id<Link>, ? extends Link> getOutLinks() {
		return null;
	}

	@Override
	public Link removeInLink(Id<Link> linkId) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Link removeOutLink(Id<Link> outLinkId) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setCoord(Coord coord) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
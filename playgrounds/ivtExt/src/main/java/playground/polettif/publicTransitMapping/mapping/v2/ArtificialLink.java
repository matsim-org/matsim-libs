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

package playground.polettif.publicTransitMapping.mapping.v2;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import playground.polettif.publicTransitMapping.mapping.pseudoRouter.LinkCandidate;

/**
 * Container class for artificial links
 */
public class ArtificialLink {

	private Id<Node> fromNodeId;
	private Id<Node> toNodeId;
	private Coord fromNodeCoord;
	private Coord toNodeCoord;

	public ArtificialLink(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate) {
		this.fromNodeId = fromLinkCandidate.getToNodeId();
		this.toNodeId = toLinkCandidate.getFromNodeId();
		this.fromNodeCoord = fromLinkCandidate.getToNodeCoord();
		this.toNodeCoord = toLinkCandidate.getFromNodeCoord();
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;

		ArtificialLink other = (ArtificialLink) obj;
		return fromNodeId.equals(other.getFromNodeId()) && toNodeId.equals(other.getToNodeId());
	}

	public Id<Node> getToNodeId() {
		return toNodeId;
	}

	public Id<Node> getFromNodeId() {
		return fromNodeId;
	}

	public Coord getFromNodeCoord() {
		return fromNodeCoord;
	}

	public Coord getToNodeCoord() {
		return toNodeCoord;
	}
}

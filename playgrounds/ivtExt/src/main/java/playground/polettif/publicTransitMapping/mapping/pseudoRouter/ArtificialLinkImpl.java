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

package playground.polettif.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingStrings;
import playground.polettif.publicTransitMapping.mapping.linkCandidateCreation.LinkCandidate;

import java.util.Set;

/**
 * Container class for artificial links
 */
public class ArtificialLinkImpl implements ArtificialLink {

	private final Id<Node> fromNodeId;
	private final Id<Node> toNodeId;
	private final Coord fromNodeCoord;
	private final Coord toNodeCoord;
	private final double freespeed;
	private final double linkLength;
	private final double capacity = 9999;
	private final Set<String> transportModes = PublicTransitMappingStrings.ARTIFICIAL_LINK_MODE_AS_SET;

	public ArtificialLinkImpl(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate, double freespeed, double linklength) {
		this.fromNodeId = fromLinkCandidate.getToNodeId();
		this.toNodeId = toLinkCandidate.getFromNodeId();
		this.fromNodeCoord = fromLinkCandidate.getToNodeCoord();
		this.toNodeCoord = toLinkCandidate.getFromNodeCoord();
		this.freespeed = freespeed;
		this.linkLength = linklength;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;

		ArtificialLinkImpl other = (ArtificialLinkImpl) obj;
		return (fromNodeId.equals(other.getFromNodeId()) &&
				toNodeId.equals(other.getToNodeId()) &&
				freespeed == other.getFreespeed() &&
				linkLength == other.getLength());
	}

	@Override
	public double getCapacity() {
		return capacity;
	}

	@Override
	public Set<String> getAllowedModes() {
		return transportModes;
	}

	@Override
	public Id<Node> getToNodeId() {
		return toNodeId;
	}

	@Override
	public Id<Node> getFromNodeId() {
		return fromNodeId;
	}

	@Override
	public Coord getFromNodeCoord() {
		return fromNodeCoord;
	}

	@Override
	public Coord getToNodeCoord() {
		return toNodeCoord;
	}

	@Override
	public double getFreespeed() {
		return freespeed;
	}

	@Override
	public double getLength() {
		return linkLength;
	}
}

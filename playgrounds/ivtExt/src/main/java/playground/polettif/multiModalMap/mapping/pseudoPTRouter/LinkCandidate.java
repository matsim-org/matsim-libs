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


package playground.polettif.multiModalMap.mapping.pseudoPTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * A possible link for a stop facility. A LinkCandidate contains a parent stop facility (the
 * one used in the original schedule), an actual Link and a child stop facility which
 * is referenced to the link.
 *
 * @author polettif
 */
public class LinkCandidate  {

	final private String id;
	private double linkLength = 0.0;

	private TransitStopFacility parentStopFacility;
	private TransitStopFacility childStopFacility;
	private Id<Node> fromNodeId;
	private Id<Node> toNodeId;
	private Coord fromNodeCoord;
	private Coord toNodeCoord;
	private Id<Link> linkId;

	public LinkCandidate(Link link, TransitStopFacility parentStopFacility) {
		this.id = parentStopFacility.getId().toString() + ".link:" + link.getId().toString();

		this.linkId = link.getId();
		this.linkLength = link.getLength();
		this.fromNodeId = link.getFromNode().getId();
		this.toNodeId = link.getToNode().getId();
		this.parentStopFacility = parentStopFacility;

		this.fromNodeCoord = link.getFromNode().getCoord();
		this.toNodeCoord = link.getToNode().getCoord();
	}

	public TransitStopFacility getParentStop() {
		return parentStopFacility;
	}

	public Id<TransitStopFacility> getParentStopId() {
		return parentStopFacility.getId();
	}

	public double getLinkLength() {
		return linkLength;
	}

	public void setChildStop(TransitStopFacility childStopFacility) {
		this.childStopFacility = childStopFacility;
	}

	public TransitStopFacility getChildStop() {
		return childStopFacility;
	}

	public String getId() {
		return id;
	}

	public double getStopDistance() {
		return CoordUtils.distancePointLinesegment(fromNodeCoord, toNodeCoord, parentStopFacility.getCoord());
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;

		LinkCandidate other = (LinkCandidate) obj;
		if(id == null) {
			if(other.id != null)
				return false;
		} else if(!id.equals(other.id))
			return false;
		return true;
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

	public Id<Link> getLinkId() {
		return linkId;
	}

	@Deprecated
	public Link getLink() {
		return null;
	}
}
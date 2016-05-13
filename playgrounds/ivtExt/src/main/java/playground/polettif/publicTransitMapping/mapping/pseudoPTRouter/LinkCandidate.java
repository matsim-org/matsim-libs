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


package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * A possible link for a stop facility. A LinkCandidate contains
 * theoretically a link and the parent stop facility. However, all
 * values besides Coord are stored as primitive/String since one might
 * be working with multiple mode separated networks.
 *
 * @author polettif
 */
public class LinkCandidate  {

	private final String id;
	private final String parentStopFacilityId;
	private final double stopFacilityDistance;
	private final double linkLength;
	private final double linkTravelTime;

	private final String linkId;
	private final String fromNodeId;
	private final String toNodeId;

	private final Coord stopFacilityCoord;
	private final Coord fromNodeCoord;
	private final Coord toNodeCoord;

	public LinkCandidate(Link link, TransitStopFacility parentStopFacility) {
		this.id = parentStopFacility.getId().toString() + ".link:" + link.getId().toString();
		this.parentStopFacilityId = parentStopFacility.getId().toString();

		this.linkId = link.getId().toString();
		this.linkLength = link.getLength();
		this.linkTravelTime = linkLength / link.getFreespeed();

		this.fromNodeId = link.getFromNode().getId().toString();
		this.toNodeId = link.getToNode().getId().toString();
		this.stopFacilityCoord = parentStopFacility.getCoord();

		this.fromNodeCoord = link.getFromNode().getCoord();
		this.toNodeCoord = link.getToNode().getCoord();

		this.stopFacilityDistance = CoordUtils.distancePointLinesegment(fromNodeCoord, toNodeCoord, stopFacilityCoord);
	}

	public double getLinkLength() {
		return linkLength;
	}

	public double getLinkTravelTime() {
		return linkTravelTime;
	}

	public String getId() {
		return id;
	}

	public double getStopFacilityDistance() {
		return stopFacilityDistance;
	}

	public String getToNodeIdStr() {
		return toNodeId;
	}

	public String getFromNodeIdStr() {
		return fromNodeId;
	}

	public String getLinkIdStr() {
		return linkId;
	}

	public Coord getFromNodeCoord() {
		return fromNodeCoord;
	}

	public Coord getToNodeCoord() {
		return toNodeCoord;
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


	/**
	 * @deprecated Should not be used since we work with different networks
	 * during pseudoRouting
	 */
	@Deprecated
	public Link getLink() {
		return null;
	}
}
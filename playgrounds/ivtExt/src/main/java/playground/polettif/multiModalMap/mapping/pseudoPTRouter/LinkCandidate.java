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

import org.matsim.api.core.v01.network.Link;
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

	private TransitStopFacility parentStopFacility;
	private Link link = null;
	private TransitStopFacility childStopFacility;

	public LinkCandidate(Link link, TransitStopFacility parentStopFacility) {
		this.id = parentStopFacility.getName() + ": " + parentStopFacility.getId() + ".link:" + link.getId();
		this.link = link;
		this.parentStopFacility = parentStopFacility;
	}

	public LinkCandidate(String id) {
		this.id = id;
	}

	public TransitStopFacility getParentStop() {
		return parentStopFacility;
	}

	public Link getLink() {
		return link;
	}

	public double getLinkLength() {
		if(link == null) {
			return 0.0;
		} else {
			return link.getLength();
		}
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
		return CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), parentStopFacility.getCoord());
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		LinkCandidate other = (LinkCandidate) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}


}
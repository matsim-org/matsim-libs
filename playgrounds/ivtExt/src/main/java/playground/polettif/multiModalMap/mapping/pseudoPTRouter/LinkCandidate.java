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

public class LinkCandidate  {

	final private String id;

	private TransitStopFacility parentStopFacility;
	private Link link = null;
	private TransitStopFacility childStopFacility;

	public LinkCandidate(Link link, TransitStopFacility parentStopFacility) {
		this.id = "ParentStopFacility: " + parentStopFacility.getName() + " (" + parentStopFacility.getId() + ") -> Link: " + link.getId();
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

	public double getLinkTravelTime() {
		if(link == null) {
			return 0.0;
		} else {
			return link.getLength()/link.getFreespeed();
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

	@Override
	public String toString() {
		return id;
	}

	public double getStopDistance() {
		return CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), parentStopFacility.getCoord());
	}
}
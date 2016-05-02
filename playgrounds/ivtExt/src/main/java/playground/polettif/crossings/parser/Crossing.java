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

package playground.polettif.crossings.parser;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class Crossing {

	private Id<Link> refLinkId = null;
	private Coord coord = null;

	public Crossing(Id<Link> refLinkId) {
		this.refLinkId = refLinkId;
	}
	public Crossing(Coord coord) {
		this.coord = coord;
	}
	public Crossing(double x, double y) {
		this.coord = new Coord(x, y);
	}

	public void setRefLinkId(Id<Link> refLinkId) {
		this.refLinkId = refLinkId;
	}

	public Id<Link> getRefLinkId() {
		return refLinkId;
	}

	public Coord getCoord() {
		return coord;
	}
}

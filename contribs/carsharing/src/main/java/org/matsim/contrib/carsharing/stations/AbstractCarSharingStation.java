/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.carsharing.stations;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

/**
 * @author nagel
 *
 */
abstract class AbstractCarSharingStation implements Facility {
	private Coord coord;
	private Id<Link> linkId;
	private Id<Link> id;
	AbstractCarSharingStation( Link link ) {
		this.coord = link.getCoord() ;  // found it with this specification. kai, feb'16
		this.linkId = link.getId();
		this.id = link.getId() ; // found it with this specification. kai, feb'16
	}
	@Override
	public Coord getCoord() {
		return this.coord ;
	}
	@Override
	public Id getId() {
		return this.id ;
	}
	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	@Override
	public Id getLinkId() {
		return this.linkId ;
	}
}

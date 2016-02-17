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
	Link link ;
	AbstractCarSharingStation( Link link ) {
		this.link = link ;
	}
	@Override
	public Coord getCoord() {
		return this.link.getCoord() ; // this is how I found it. kai, feb'16
	}
	@Override
	public Id getId() {
		return this.link.getId() ; // this is how I found it.  kai, feb'16
	}
	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	@Override
	public Id getLinkId() {
		return this.link.getId();
	}
}

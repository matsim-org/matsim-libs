/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.moneyTravelDisutility.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
* Stores time-specific data for each link.
* 
* @author ikaddoura
*/

public class LinkInfo {
	
	@Override
	public String toString() {
		return "LinkInfo [id=" + id + ", nr2timeBin=" + nr2timeBin + "]";
	}

	private final Id<Link> id;
	private final Map<Integer, TimeBin> nr2timeBin;
	
	public LinkInfo(Id<Link> id) {
		this.id = id;
		this.nr2timeBin = new HashMap<>();
	}

	public Id<Link> getId() {
		return id;
	}

	public Map<Integer, TimeBin> getTimeBinNr2timeBin() {
		return nr2timeBin;
	}
	
}


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

package playground.polettif.crossings;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class LinkChangeEvent {
	
	private Id<Link> linkId;
	private String starttime;
	private String stoptime;
	private String capacity;
     
	public LinkChangeEvent(Id<Link> linkId, String starttime, String stoptime, String capacity) {
		this.linkId = linkId;
		this.starttime = starttime;
		this.stoptime = stoptime;
		this.capacity = capacity;
	}
	
	public Id<Link> getLinkId() {
		return linkId;
	}
	
	public String getStarttime() {
		return starttime;
	}
	
	public String getStoptime() {
		return stoptime;
	}
	
	public String getCapacity() {
		return capacity;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * DgLaneSensor
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.lanes.Lane;


/**
 * @author dgrether
 *
 */
public class DgLaneSensor {

	private Link link;
	private Lane lane;
	private int agentsOnLink = 0;

	public DgLaneSensor(Link link, Lane lane) {
		this.link = link;
		this.lane = lane;
	}

	public void handleEvent(LaneLeaveEvent event) {
		this.agentsOnLink--;
	}

	public void handleEvent(LaneEnterEvent event) {
		this.agentsOnLink++;
	}

	public int getNumberOfCarsOnLink() {
		return this.agentsOnLink;
	}
	
	

}

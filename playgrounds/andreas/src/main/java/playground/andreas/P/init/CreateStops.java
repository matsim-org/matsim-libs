/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.P.init;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.osmBB.extended.TransitScheduleImpl;

public class CreateStops {
	
	private final NetworkImpl net;
	private final double gridDistance;
	private final Coord minXY;
	private final Coord maxXY;
	
	private TransitSchedule transitSchedule;
	
	public static TransitSchedule createStops(NetworkImpl network, double gridDistance, Coord minXY, Coord maxXY){
		CreateStops cS = new CreateStops(network, gridDistance, minXY, maxXY);
		cS.run();
		return cS.getTransitSchedule();
	}

	public CreateStops(NetworkImpl net, double gridDistance, Coord minXY, Coord maxXY) {
		this.net = net;
		this.gridDistance = gridDistance;
		this.minXY = minXY;
		this.maxXY = maxXY;		
	}

	private void run(){
		this.transitSchedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		int stopsAdded = 0;
		
		for (int i = (int) this.minXY.getX(); i < this.maxXY.getX(); i += this.gridDistance) {
			for (int j = (int) this.minXY.getY(); j < this.maxXY.getY(); j += this.gridDistance) {

				// point to add a stop
				Link link = this.net.getNearestLink(new CoordImpl(i, j));
				Link backLink = getBackLink(link);
				
				stopsAdded += addStopOnLink(link);
				stopsAdded += addStopOnLink(backLink);						
			}
		}
	}
	
	private int addStopOnLink(Link link) {
		if(link == null){
			return 0;
		}

		for (TransitStopFacility stop : this.transitSchedule.getFacilities().values()) {
			if(stop.getLinkId().toString().equalsIgnoreCase(link.getId().toString())){
				// link has already a stop
				return 0;
			}
		}
		
		TransitStopFacility stop = this.transitSchedule.getFactory().createTransitStopFacility(link.getId(), link.getCoord(), false);
		stop.setLinkId(link.getId());
		this.transitSchedule.addStopFacility(stop);
		return 1;		
	}

	private Link getBackLink(Link link){
		for (Link tempLink : link.getToNode().getOutLinks().values()) {
			if(tempLink.getToNode().getId().toString().equalsIgnoreCase(link.getFromNode().getId().toString())){
				return tempLink;
			}
		}
		return null;
	}

	private TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}
}

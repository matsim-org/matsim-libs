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

package playground.andreas.P2.pbox;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.schedule.PTransitSchedule;
import playground.andreas.osmBB.extended.TransitScheduleImpl;

/**
 * Create one TransitStopFacility for each car mode link of the network
 * 
 * @author aneumann
 *
 */
public class CreateStopsForAllCarLinks {
	
	private final static Logger log = Logger.getLogger(CreateStopsForAllCarLinks.class);
	
	private final NetworkImpl net;
	private TransitSchedule transitSchedule;
	
	public static TransitSchedule createStopsForAllCarLinks(NetworkImpl network){
		CreateStopsForAllCarLinks cS = new CreateStopsForAllCarLinks(network);
		cS.run();
		return cS.getTransitSchedule();
	}

	public CreateStopsForAllCarLinks(NetworkImpl net) {
		this.net = net;
	}

	private void run(){
		this.transitSchedule = new PTransitSchedule(new TransitScheduleImpl(new TransitScheduleFactoryImpl()));
		int stopsAdded = 0;
		
		for (Link link : this.net.getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.car)){
				stopsAdded += addStopOnLink(link);
			}
		}		
	}
	
	private int addStopOnLink(Link link) {
		if(link == null){
			return 0;
		}

		for (TransitStopFacility stop : this.transitSchedule.getFacilities().values()) {
			if(stop.getLinkId().toString().equalsIgnoreCase(link.getId().toString())){
				log.warn("Link " + link.getId() + " has already a stop. This should not happen. Check code.");
				return 0;
			}
		}
		
		TransitStopFacility stop = this.transitSchedule.getFactory().createTransitStopFacility(new IdImpl("p_" + link.getId()), link.getCoord(), false);
		stop.setLinkId(link.getId());
		this.transitSchedule.addStopFacility(stop);
		return 1;		
	}

	private TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}
}
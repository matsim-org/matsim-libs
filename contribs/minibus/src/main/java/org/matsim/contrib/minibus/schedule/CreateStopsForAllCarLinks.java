/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.schedule;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Create one TransitStopFacility for each car mode link of the network
 * 
 * @author aneumann
 *
 */
public final class CreateStopsForAllCarLinks {
	
	private final static Logger log = Logger.getLogger(CreateStopsForAllCarLinks.class);
	
	private final Network net;
	private final PConfigGroup pConfigGroup;
	private TransitSchedule transitSchedule;

	private final LinkedHashMap<Id<Link>, TransitStopFacility> linkId2StopFacilityMap;
	
	public static TransitSchedule createStopsForAllCarLinks(Network network, PConfigGroup pConfigGroup){
		return createStopsForAllCarLinks(network, pConfigGroup, null);
	}

	public static TransitSchedule createStopsForAllCarLinks(Network network, PConfigGroup pConfigGroup, TransitSchedule realTransitSchedule) {
		CreateStopsForAllCarLinks cS = new CreateStopsForAllCarLinks(network, pConfigGroup, realTransitSchedule);
		cS.run();
		return cS.getTransitSchedule();
	}

	private CreateStopsForAllCarLinks(Network net, PConfigGroup pConfigGroup, TransitSchedule realTransitSchedule) {
		this.net = net;
		this.pConfigGroup = pConfigGroup;
		
		this.linkId2StopFacilityMap = new LinkedHashMap<>();
		
		Set<Id<org.matsim.facilities.Facility>> stopsWithoutLinkIds = new TreeSet<>();
		
		if (realTransitSchedule != null) {
			for (TransitStopFacility stopFacility : realTransitSchedule.getFacilities().values()) {
				if (stopFacility.getLinkId() != null) {
					if (this.linkId2StopFacilityMap.get(stopFacility.getLinkId()) != null) {
						log.error("There is more than one stop registered on link " + stopFacility.getLinkId() + ". "
								+ this.linkId2StopFacilityMap.get(stopFacility.getLinkId()).getId() + " stays registered as paratransit stop. Will ignore stop " + stopFacility.getId());
					} else {
						this.linkId2StopFacilityMap.put(stopFacility.getLinkId(), stopFacility);
					}
				} else {
					stopsWithoutLinkIds.add(stopFacility.getId());
				}
			}
		}
		
		if (stopsWithoutLinkIds.size() > 0) {
			log.warn("There are " + stopsWithoutLinkIds.size() + " stop facilities without link id, namely: " + stopsWithoutLinkIds.toString());
		}			
	}

	private void run(){
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		int stopsAdded = 0;
		
		for (Link link : this.net.getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.car)){
				stopsAdded += addStopOnLink(link);
			}
		}
		
		log.info("Added " + stopsAdded + " additional stops for paratransit services");
	}
	
	private int addStopOnLink(Link link) {
		if(link == null){
			return 0;
		}
		
		if(linkToNodeNotInServiceArea(link)){
			return 0;
		}
		
		if (linkHasAlreadyAFormalPTStopFromTheGivenSchedule(link)) {
			return 0;
		}
		
		if (link.getFreespeed() >= this.pConfigGroup.getSpeedLimitForStops()) {
			return 0;
		}
		
		if (this.linkId2StopFacilityMap.get(link.getId()) != null) {
			log.warn("Link " + link.getId() + " has already a stop. This should not happen. Check code.");
			return 0;
		}

		Id<org.matsim.facilities.Facility> stopId = Id.create(this.pConfigGroup.getPIdentifier() + link.getId(), org.matsim.facilities.Facility.class);
		TransitStopFacility stop = this.transitSchedule.getFactory().createTransitStopFacility(stopId, link.getToNode().getCoord(), false);
		stop.setLinkId(link.getId());
		this.transitSchedule.addStopFacility(stop);
		return 1;		
	}

	private boolean linkToNodeNotInServiceArea(Link link) {
		Coord toNodeCoord = link.getToNode().getCoord();
		
		if(toNodeCoord.getX() < this.pConfigGroup.getMinX()){
			return true;
		}
		if(toNodeCoord.getX() > this.pConfigGroup.getMaxX()){
			return true;
		}
		if(toNodeCoord.getY() < this.pConfigGroup.getMinY()){
			return true;
		}
		if(toNodeCoord.getY() > this.pConfigGroup.getMaxY()){
			return true;
		}
		return false;
	}

	private boolean linkHasAlreadyAFormalPTStopFromTheGivenSchedule(Link link) {
		if (this.linkId2StopFacilityMap.containsKey(link.getId())) {
			// There is already a stop at this link, used by formal public transport - Use this one instead
			this.transitSchedule.addStopFacility(this.linkId2StopFacilityMap.get(link.getId()));
			return true;
		} else {
			return false;
		}
	}

	private TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}
}

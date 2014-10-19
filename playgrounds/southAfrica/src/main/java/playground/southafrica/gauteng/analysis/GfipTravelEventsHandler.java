/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.gauteng.analysis;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class GfipTravelEventsHandler implements LinkEnterEventHandler{
	private final static Logger LOG = Logger.getLogger(GfipTravelEventsHandler.class);
	private final Scenario sc;
	private final List<Id<Link>> links;
	private Map<Id<VehicleType>, Map<Id<Vehicle>, Double>> map = new TreeMap<>();
	
	public GfipTravelEventsHandler(Scenario sc, List<Id<Link>> links) {
		this.sc = sc;
		this.links = links;
	}

	@Override
	public void reset(int iteration) {
		map = new TreeMap<>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		
		if(links.contains(linkId)){
			double linkDistance = this.sc.getNetwork().getLinks().get(linkId).getLength();
			VehicleType vehicleType = this.sc.getVehicles().getVehicles().get(vehicleId).getType();
			if(!map.containsKey(vehicleType.getId())){
				map.put(vehicleType.getId(), new TreeMap<Id<Vehicle>, Double>());
			}
			Map<Id<Vehicle>, Double> vehicleMap = map.get(vehicleType.getId());
			if(vehicleMap.containsKey(vehicleId)){
				double oldValue = vehicleMap.get(vehicleId);
				vehicleMap.put(vehicleId, oldValue + linkDistance);
			} else{
				vehicleMap.put(vehicleId, linkDistance);
			}
		} else{
			/* Ignore the event. */
		}
	}
	
	public Map<Id<VehicleType>, Map<Id<Vehicle>, Double>> getMap(){
		return this.map;
	}
	
	public void reportAggregateStatistics(){
		LOG.info("Statistics of GFIP travel (by vehicle type);");
		for(Id<VehicleType> id : map.keySet()){
			Double sum = 0.0;
			for(Double d : map.get(id).values()){
				sum += d;
			}
			LOG.info(String.format("%6s: %6.0f km", id.toString(), sum/1000.));
		}
	}

}

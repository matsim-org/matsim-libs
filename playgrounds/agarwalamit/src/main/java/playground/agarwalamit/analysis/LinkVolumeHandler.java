/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author amit
 */
public class LinkVolumeHandler implements LinkEnterEventHandler {

	private final Logger logger = Logger.getLogger(LinkVolumeHandler.class);
	private Map<Id<Link>, Map<Integer,Double>> linkId2Time2Volume = new HashMap<Id<Link>, Map<Integer,Double>>();
	private Map<Id<Link>, Map<Integer,List<Id<Vehicle>>>> linkId2Time2Persons = new HashMap<Id<Link>, Map<Integer,List<Id<Vehicle>>>>();

	public LinkVolumeHandler () {
		this.logger.info("Starting volume count on links.");
		reset(0);
	}
	@Override
	public void reset(int iteration) {
		this.linkId2Time2Volume.clear();
		this.linkId2Time2Persons.clear();
	}

	private int getSlot(double time){
		return (int)time/3600;
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		int slotInt = getSlot(event.getTime());
		Map<Integer, Double> volsTime = new HashMap<Integer, Double>();
		Map<Integer, List<Id<Vehicle>>> time2persons = new HashMap<Integer, List<Id<Vehicle>>>();

		Id<Link> linkId = event.getLinkId();
		if(this.linkId2Time2Volume.containsKey(linkId)){
			volsTime =	this.linkId2Time2Volume.get(linkId);
			
			time2persons = this.linkId2Time2Persons.get(linkId);
			List<Id<Vehicle>> vehicles = new ArrayList<>();
			
			if(volsTime.containsKey(slotInt)) {
				
				vehicles = time2persons.get(slotInt);
				vehicles.add(event.getVehicleId());
				
				double counter = (volsTime.get(slotInt));
				double newCounter = counter+1;
				volsTime.put(slotInt, newCounter);
				this.linkId2Time2Volume.put(linkId, volsTime);
				
			}else {
				
				vehicles.add(event.getVehicleId());
				time2persons.put(slotInt, vehicles);
				this.linkId2Time2Persons.put(linkId, time2persons);
				volsTime.put(slotInt, 1.0);
				this.linkId2Time2Volume.put(linkId, volsTime);
			} 
		}else {
			List<Id<Vehicle>> vehicles = new ArrayList<>();
			vehicles.add(event.getVehicleId());
			time2persons.put(slotInt,vehicles);
			this.linkId2Time2Persons.put(linkId, time2persons);
			
			volsTime.put(slotInt, 1.0);
			this.linkId2Time2Volume.put(linkId, volsTime);
		}
	}

	public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkVolume(){
		return this.linkId2Time2Volume;
	}
	
	public Map<Id<Link>, Map<Integer, List<Id<Vehicle>>>> getLinkId2TimeSlot2VehicleIds(){
		return this.linkId2Time2Persons;
	}
}
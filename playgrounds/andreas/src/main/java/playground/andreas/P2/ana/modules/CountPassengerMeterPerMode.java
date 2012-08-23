/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.P2.ana.modules;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;


/**
 * Count the number of passenger-meter per ptModes specified.
 * 
 * @author aneumann
 *
 */
public class CountPassengerMeterPerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler{
	
	private final static Logger log = Logger.getLogger(CountPassengerMeterPerMode.class);
	
	private Network network;
	private HashMap<Id, String> vehId2ptModeMap;
	private HashMap<String, Double> ptMode2CountMap;
	private HashMap<Id, Integer> vehId2NumberOfPassengers = new HashMap<Id, Integer>();

	
	public CountPassengerMeterPerMode(String ptDriverPrefix, Network network){
		super("CountPassengerMeterPerMode",ptDriverPrefix);
		this.network = network;
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + this.ptMode2CountMap.get(ptMode));
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2ptModeMap = new HashMap<Id, String>();
		this.ptMode2CountMap = new HashMap<String, Double>();
		this.vehId2NumberOfPassengers = new HashMap<Id, Integer>();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		String ptMode = this.lineIds2ptModeMap.get(event.getTransitLineId());
		if (ptMode == null) {
			log.warn("Should not happen");
			ptMode = "no valid pt mode found";
		}
		this.vehId2ptModeMap.put(event.getVehicleId(), ptMode);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehId2NumberOfPassengers.get(event.getVehicleId()) == null) {
			this.vehId2NumberOfPassengers.put(event.getVehicleId(), new Integer(0));
		}
		
		if(!event.getPersonId().toString().startsWith(ptDriverPrefix)){
			this.vehId2NumberOfPassengers.put(event.getVehicleId(), new Integer(this.vehId2NumberOfPassengers.get(event.getVehicleId()).intValue() + 1));
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(!event.getPersonId().toString().startsWith(ptDriverPrefix)){
			this.vehId2NumberOfPassengers.put(event.getVehicleId(), new Integer(this.vehId2NumberOfPassengers.get(event.getVehicleId()).intValue() - 1));
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
		if (ptMode2CountMap.get(ptMode) == null) {
			ptMode2CountMap.put(ptMode, new Double(0.0));
		}

		ptMode2CountMap.put(ptMode, new Double(ptMode2CountMap.get(ptMode) + this.network.getLinks().get(event.getLinkId()).getLength() * this.vehId2NumberOfPassengers.get(event.getVehicleId()).intValue()));
	}

}

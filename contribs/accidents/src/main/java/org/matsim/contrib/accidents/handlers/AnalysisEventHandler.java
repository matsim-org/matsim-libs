/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class AnalysisEventHandler implements EventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler {

	private static final Logger log = Logger.getLogger(AnalysisEventHandler.class);	
	private final Map<Id<Link>, Map<Integer, Integer>> linkId2time2leavingAgents = new HashMap<>();
	private final Map<Id<Link>, Map<Integer, List<Id<Person>>>> linkId2time2personIds = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();
	
	@Inject
	private Scenario scenario;
	
	@Override
	public void reset(int arg0) {
		// reset temporary information at the beginning of each iteration
		
		linkId2time2leavingAgents.clear();
		linkId2time2personIds.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		double timeBinSize = this.scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(); 
		int timeBinNr = (int) (event.getTime() / timeBinSize);
		
		// TODO: Remove once this is tested.
//		log.info("-----------");
//		log.info("time: " + event.getTime());
//		log.info("time bin nr: " + timeBinNr);
//		log.info("-----------");

		Id<Link> linkId = event.getLinkId();
		
		if (linkId2time2leavingAgents.get(linkId) != null) {
			
			if (linkId2time2leavingAgents.get(linkId).get(timeBinNr) != null) {
				int leavingAgents = linkId2time2leavingAgents.get(linkId).get(timeBinNr) + 1;
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, leavingAgents);
				
			} else {
				linkId2time2leavingAgents.get(linkId).put(timeBinNr, 1);
			}
			
		} else {
			Map<Integer, Integer> time2leavingAgents = new HashMap<>();
			time2leavingAgents.put(timeBinNr, 1);
			linkId2time2leavingAgents.put(linkId, time2leavingAgents);
		}
		
		if (linkId2time2personIds.get(linkId) != null) {
			
			if (linkId2time2personIds.get(linkId).get(timeBinNr) != null) {
				linkId2time2personIds.get(linkId).get(timeBinNr).add(getDriverId(event.getVehicleId()));
				
			} else {
				List<Id<Person>> personIds = new ArrayList<>();
				personIds.add(getDriverId(event.getVehicleId()));
				linkId2time2personIds.get(linkId).put(timeBinNr, personIds);
			}
			
		} else {
			Map<Integer, List<Id<Person>>> time2leavingAgents = new HashMap<>();
			List<Id<Person>> personIds = new ArrayList<>();
			personIds.add(getDriverId(event.getVehicleId()));
			time2leavingAgents.put(timeBinNr, personIds);
			linkId2time2personIds.put(linkId, time2leavingAgents);
		}
		
		// TODO: Remove once this is tested.
//		log.info("-----------");
//		log.info(event.toString());
//		log.info(linkId2time2leavingAgents.toString());
//		log.info("-----------");
	}

	private Id<Person> getDriverId(Id<Vehicle> vehicleId) {
		return this.vehicleId2personId.get(vehicleId);
	}

	public double getDemand(Id<Link> linkId, int intervalNr) {
		double demand = 0.;
		if (this.linkId2time2leavingAgents.get(linkId) != null && this.linkId2time2leavingAgents.get(linkId).get(intervalNr) != null) {
			demand = this.linkId2time2leavingAgents.get(linkId).get(intervalNr);
		}
		return demand;
	}

	public Map<Id<Link>, Map<Integer, List<Id<Person>>>> getLinkId2time2personIds() {
		return linkId2time2personIds;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleId2personId.put(event.getVehicleId(), event.getPersonId());
	}
	
}


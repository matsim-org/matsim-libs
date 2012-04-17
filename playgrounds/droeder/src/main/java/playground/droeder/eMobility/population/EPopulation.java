/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.population;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;

import playground.droeder.eMobility.fleet.EFleet;

/**
 * @author droeder
 *
 */
public class EPopulation {
	
	public static final String IDENTIFIER = "emob_";
	
	private Map<Id, EPerson> persons;
	private EFleet fleet;
	
	public EPopulation(){
		this.persons = new HashMap<Id, EPerson>();
	}
	
	public void add(EPerson p){
		this.persons.put(p.getId(), p);
	}
	
	public void init(EFleet fleet){
		this.fleet =  fleet;
		System.err.println("Fleet in Population " + fleet);
	}
	
	public EPerson getPerson(Id id){
		return this.persons.get(id);
	}

	/**
	 * @param event
	 */
	public void processEvent(PersonLeavesVehicleEvent event) {
		this.fleet.processEvent(event);
	}

	/**
	 * @param event
	 */
	public void processEvent(PersonEntersVehicleEvent event) {
		this.fleet.processEvent(event);
	}

}

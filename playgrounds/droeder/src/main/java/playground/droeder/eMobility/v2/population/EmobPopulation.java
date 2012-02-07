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
package playground.droeder.eMobility.v2.population;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;

/**
 * @author droeder
 *
 */
public class EmobPopulation {
	private static final Logger log = Logger.getLogger(EmobPopulation.class);
	
	private HashMap<Id, EmobPerson> population;

	public EmobPopulation(){
		this.population = new HashMap<Id, EmobPerson>();
	}
	
	public void addPerson(EmobPerson p){
		if(!this.population.containsKey(p.getId())){
			this.population.put(p.getId(), p);
		}else{
			log.error("a person with id " + p.getId() + "already exists...");
		}
	}
	public EmobPerson getPerson(Id id){
		return this.population.get(id);
	}

	/**
	 * @param e
	 */
	public void processEvent(AgentArrivalEvent e) {
		if(this.containsPerson(e.getPersonId())){
			this.population.get(e.getPersonId()).processEvent(e);
		}
	}

	/**
	 * @param e
	 */
	public void processEvent(AgentDepartureEvent e) {
		if(this.containsPerson(e.getPersonId())){
			this.population.get(e.getPersonId()).processEvent(e);
		}
	}

	public boolean containsPerson(Id id){
		if(this.population.containsKey(id)) {
			return true;
		}
		return false;
	}

//	public Population getMATSimPopulation(Population population){
//		for(EmobPerson p : this.population.values()){
//			if(!population.getPersons().containsKey(p.getId())){
//				this.createNewMATSimPerson(p, population);
//			}
//		}
//		return this.matsim;
//	}
//
//	private void createNewMATSimPerson(EmobPerson p, Population population) {
//		
//		// TODO Auto-generated method stub
//		
//	}
}

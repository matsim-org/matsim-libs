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

package playground.vsp.analysis.modules.bvgAna.anaLevel0;

import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * 
 * @author ikaddoura, aneumann
 *
 */
public class AgentId2PersonMap {

	private static final Logger log = Logger.getLogger(AgentId2PersonMap.class);
	private static final Level logLevel = Level.DEBUG;
	
	/**
	 * @param pop The given population
	 * @param agentIds Set of agent ids of interest
	 * @return Returns a map holding the populations person for each given agent id
	 */
	public static TreeMap<Id, Person> getAgentId2PersonMap(Population pop){
		AgentId2PersonMap.log.setLevel(AgentId2PersonMap.logLevel);
		
		TreeMap<Id, Person> agentId2PersonMap = new TreeMap<Id, Person>();
		
		log.debug("Parsing population...");
		
		for (Person person : pop.getPersons().values()) {
			if(person instanceof Person){
				agentId2PersonMap.put(person.getId(), person);
			} else {
				log.debug(person + " is not of type PersonImpl, but of type " + person.getClass() + ". Don't know how to handle that one.");
			}
		}
		
		log.debug("Finished. Added " + agentId2PersonMap.size() + " persons of " + pop.getPersons().size() + " persons given by population to the map.");
		
		return agentId2PersonMap;
	}
}

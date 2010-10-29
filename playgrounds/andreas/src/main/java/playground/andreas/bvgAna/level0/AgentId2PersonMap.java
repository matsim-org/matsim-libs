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

package playground.andreas.bvgAna.level0;

import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;

/**
 * 
 * @author aneumann
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
	public static TreeMap<Id, PersonImpl> getAgentId2PersonMap(Population pop, Set<Id> agentIds){
		AgentId2PersonMap.log.setLevel(AgentId2PersonMap.logLevel);
		
		TreeMap<Id, PersonImpl> agentId2PersonMap = new TreeMap<Id, PersonImpl>();
		
		log.debug("Parsing population...");
		
		for (Person person : pop.getPersons().values()) {
			if(agentIds.contains(person.getId())){
				// person is of interest
				if(person instanceof PersonImpl){
					agentId2PersonMap.put(person.getId(), (PersonImpl) person);
				} else {
					log.debug(person + " is not of type PersonImpl, but of type " + person.getClass() + ". Don't know how to handle that one.");
				}
			}
		}
		
		log.debug("Finished. Added " + agentId2PersonMap.size() + " persons of " + agentIds.size() + " given ids to the map.");
		
		return agentId2PersonMap;
	}
}

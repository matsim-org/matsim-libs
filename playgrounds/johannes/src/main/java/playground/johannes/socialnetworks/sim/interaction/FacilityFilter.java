/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityFilter.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.xml.sax.SAXException;

/**
 * @author illenberger
 *
 */
public class FacilityFilter {

	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		String netFile = args[0];
		String facFile = args[1];
		String popFile = args[2];
		String facOutFile = args[3];
		
		ScenarioImpl scenario = new ScenarioImpl();

		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse(netFile);

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
		facReader.parse(facFile);
		
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);
		
		Set<Id> toRemove = new HashSet<Id>(scenario.getActivityFacilities().getFacilities().keySet());
		
		for(Person person : scenario.getPopulation().getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
					Id facId = ((Activity)plan.getPlanElements().get(i)).getFacilityId();
					if(toRemove.contains(facId)) {
						toRemove.remove(facId);
					}
				}
			}
		}
		
		for(Id id : toRemove) {
			scenario.getActivityFacilities().getFacilities().remove(id);
		}
		
		FacilitiesWriter writer = new FacilitiesWriter(scenario.getActivityFacilities());
		writer.write(facOutFile);
	}
}

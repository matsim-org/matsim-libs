/* *********************************************************************** *
 * project: org.matsim.*
 * MiDPersonFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.filter;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author benjamin
 *
 */
public class PersonFilter {
	
	public Population getMiDPopulation(Population population) {
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonFromMID(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	public boolean isPersonFromMID(Person person) {
		boolean isFromMID = false;
		if(!person.getId().toString().contains("gv_") && !person.getId().toString().contains("pv_")){
			isFromMID = true;
		}
		return isFromMID;
	}
	
	public boolean isPersonFreight(Person person) {
		boolean isFreight = false;
		if(person.getId().toString().contains("gv_")){
			isFreight = true;
		}
		return isFreight;
	}
}
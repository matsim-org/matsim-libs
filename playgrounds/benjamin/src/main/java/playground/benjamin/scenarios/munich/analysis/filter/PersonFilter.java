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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author benjamin
 *
 */
public class PersonFilter {
	private static final Logger logger = Logger.getLogger(PersonFilter.class);
	
	public Population getPopulation(Population population, UserGroup userGroup) {
		Population filteredPopulation = null;
		if(userGroup.equals(UserGroup.URBAN)) filteredPopulation = getMiDPopulation(population);
		else if(userGroup.equals(UserGroup.COMMUTER)) filteredPopulation = getInnCommuter(population);
		else if(userGroup.equals(UserGroup.REV_COMMUTER)) filteredPopulation = getOutCommuter(population);
		else if(userGroup.equals(UserGroup.FREIGHT)) filteredPopulation = getFreightPopulation(population);
		return filteredPopulation;
	}
	
	public boolean isPersonIdFromUserGroup(Id personId, UserGroup userGroup) {
		boolean isFromUserGroup = false;
		
		if(isPersonFromMID(personId)){
			if(userGroup.equals(UserGroup.URBAN)) isFromUserGroup = true ;
		}
		else if(isPersonInnCommuter(personId)){
			if(userGroup.equals(UserGroup.COMMUTER)) isFromUserGroup = true;
		}
		else if(isPersonOutCommuter(personId)){
			if(userGroup.equals(UserGroup.REV_COMMUTER)) isFromUserGroup = true;
		}
		else if(isPersonFreight(personId)){
			if(userGroup.equals(UserGroup.FREIGHT)) isFromUserGroup = true;
		}
		else{
			logger.warn("Cannot match person " + personId + " to any user group defined in " + PersonFilter.class);
		}
		return isFromUserGroup;
	}

	public Population getMiDPopulation(Population population) {
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFromMID(person.getId())){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getMunichPopulation(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFromMunich(person.getId())){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	public Population getInnCommuter(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonInnCommuter(person.getId())){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getOutCommuter(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonOutCommuter(person.getId())){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getFreightPopulation(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFreight(person.getId())){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	public Population getNonFreightPopulation(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(!isPersonFreight(person.getId())){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public boolean isPersonFromMID(Id personId) {
		boolean isFromMID = false;
		if(personId.toString().startsWith("gv_")); //do nothing
		else if(personId.toString().startsWith("pv_")); //do nothing
		else isFromMID = true;
		return isFromMID;
	}
	
	public boolean isPersonFromMunich(Id personId) {
		boolean isFromMunich = false;
		if(isPersonFromMID(personId) || isPersonOutCommuter(personId)){
			isFromMunich = true;
		}
		return isFromMunich;
	}

	public boolean isPersonInnCommuter(Id personId) {
		boolean isInnCommuter = false;
		if(personId.toString().startsWith("pv_")){
			if(personId.toString().startsWith("pv_car_9162")); //do nothing
			else if(personId.toString().startsWith("pv_pt_9162")); //do nothing
			else isInnCommuter = true;
		}
		return isInnCommuter;
	}

	public boolean isPersonOutCommuter(Id personId) {
		boolean isOutCommuter = false;
		if(personId.toString().startsWith("pv_car_9162") || personId.toString().startsWith("pv_pt_9162")){
			isOutCommuter = true;
		}
		return isOutCommuter;
	}

	public boolean isPersonFreight(Id personId) {
		boolean isFreight = false;
		if(personId.toString().startsWith("gv_")){
			isFreight = true;
		}
		return isFreight;
	}
}
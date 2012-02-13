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
	
	public Population getPopulation(Population population, UserGroup userGroup) {
		Population filteredPopulation = null;
		
		if(userGroup.equals(UserGroup.MID)) filteredPopulation = getMiDPopulation(population);
		else if(userGroup.equals(UserGroup.INN_COMMUTER)) filteredPopulation = getInnCommuter(population);
		else if(userGroup.equals(UserGroup.OUT_COMMUTER)) filteredPopulation = getOutCommuter(population);
		else if(userGroup.equals(UserGroup.FREIGHT)) filteredPopulation = getFreightPopulation(population);
		
		return filteredPopulation;
	}
	
	public boolean isPersonFromUserGroup(Person person, UserGroup userGroup) {
		boolean isFromUserGroup = false;
		
		if(isPersonFromMID(person) && userGroup.equals(UserGroup.MID)) isFromUserGroup = true;
		else if(isPersonInnCommuter(person) && userGroup.equals(UserGroup.INN_COMMUTER)) isFromUserGroup = true;
		else if(isPersonOutCommuter(person) && userGroup.equals(UserGroup.OUT_COMMUTER)) isFromUserGroup = true;
		else if(isPersonFreight(person) && userGroup.equals(UserGroup.FREIGHT)) isFromUserGroup = true;
		
		return isFromUserGroup;
	}
	
	public boolean isPersonIdFromUserGroup(Id personId, UserGroup userGroup) {
		boolean isFromUserGroup = false;
		
		if(isPersonFromMID(personId) && userGroup.equals(UserGroup.MID)) isFromUserGroup = true;
		else if(isPersonInnCommuter(personId) && userGroup.equals(UserGroup.INN_COMMUTER)) isFromUserGroup = true;
		else if(isPersonOutCommuter(personId) && userGroup.equals(UserGroup.OUT_COMMUTER)) isFromUserGroup = true;
		else if(isPersonFreight(personId) && userGroup.equals(UserGroup.FREIGHT)) isFromUserGroup = true;
		
		return isFromUserGroup;
	}

	public Population getMiDPopulation(Population population) {
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFromMID(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getMunichPopulation(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFromMunich(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	public Population getInnCommuter(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonInnCommuter(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getOutCommuter(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonOutCommuter(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getFreightPopulation(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFreight(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}

	public Population getNonFreightPopulation(Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(!isPersonFreight(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public boolean isPersonFromMID(Person person) {
		boolean isFromMID = false;
		if(!person.getId().toString().startsWith("gv_") && !person.getId().toString().startsWith("pv_")){
			isFromMID = true;
		}
		return isFromMID;
	}

	public boolean isPersonFromMID(Id personId) {
		boolean isFromMID = false;
		if(!personId.toString().startsWith("gv_") && !personId.toString().startsWith("pv_")){
			isFromMID = true;
		}
		return isFromMID;
	}
	
	public boolean isPersonFromMunich(Person person) {
		boolean isFromMunich = false;
		if(isPersonFromMID(person) || isPersonOutCommuter(person)){
			isFromMunich = true;
		}
		return isFromMunich;
	}
	
	public boolean isPersonFromMunich(Id personId) {
		boolean isFromMunich = false;
		if(isPersonFromMID(personId) || isPersonOutCommuter(personId)){
			isFromMunich = true;
		}
		return isFromMunich;
	}

	public boolean isPersonInnCommuter(Person person) {
		boolean isInnCommuter = false;
		if(person.getId().toString().startsWith("pv_")){
			if(!person.getId().toString().startsWith("pv_car_9162") || !person.getId().toString().startsWith("pv_pt_9162")){
				isInnCommuter = true;
			}
		}
		return isInnCommuter;
	}
	
	public boolean isPersonInnCommuter(Id personId) {
		boolean isInnCommuter = false;
		if(personId.toString().startsWith("pv_")){
			if(!personId.toString().startsWith("pv_car_9162") || !personId.toString().startsWith("pv_pt_9162")){
				isInnCommuter = true;
			}
		}
		return isInnCommuter;
	}

	public boolean isPersonOutCommuter(Person person) {
		boolean isOutCommuter = false;
		if(person.getId().toString().startsWith("pv_car_9162") || person.getId().toString().startsWith("pv_pt_9162")){
			isOutCommuter = true;
		}
		return isOutCommuter;
	}

	public boolean isPersonOutCommuter(Id personId) {
		boolean isOutCommuter = false;
		if(personId.toString().startsWith("pv_car_9162") || personId.toString().startsWith("pv_pt_9162")){
			isOutCommuter = true;
		}
		return isOutCommuter;
	}

	public boolean isPersonFreight(Person person) {
		boolean isFreight = false;
		if(person.getId().toString().startsWith("gv_")){
			isFreight = true;
		}
		return isFreight;
	}

	public boolean isPersonFreight(Id personId) {
		boolean isFreight = false;
		if(personId.toString().startsWith("gv_")){
			isFreight = true;
		}
		return isFreight;
	}
}
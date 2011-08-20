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
	
	public Population getInnCommuter(Population population){
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonInnCommuter(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getOutCommuter(Population population){
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonOutCommuter(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getMunichPopulation(Population population){
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(isPersonFromMID(person) || isPersonOutCommuter(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public Population getNonFreightPopulation(Population population){
		ScenarioImpl emptyScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = new PopulationImpl(emptyScenario);
		for(Person person : population.getPersons().values()){
			if(!isPersonFreight(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
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
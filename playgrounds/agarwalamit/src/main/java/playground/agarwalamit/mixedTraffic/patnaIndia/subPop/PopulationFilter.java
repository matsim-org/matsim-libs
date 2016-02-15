/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.subPop;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit based on PersonFilter of BK.
 */

public final class PopulationFilter {
	
	private PopulationFilter() {}
	
	private enum PatnaSubPopulations {slum, nonSlum};
	
	public static Population getSubPopulation(final Population population, final PatnaSubPopulations subPopulationType){
		Population filteredPopulation = null;
		if(subPopulationType.equals(PatnaSubPopulations.slum)) filteredPopulation = getSlumPopulation(population);
		else if(subPopulationType.equals(PatnaSubPopulations.nonSlum)) filteredPopulation = getNonSlumPopulation(population);
		return filteredPopulation;
	}
	
	public static Population getSlumPopulation(final Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFromSlum(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public static Population getNonSlumPopulation(final Population population){
		Scenario emptyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population filteredPopulation = emptyScenario.getPopulation();
		for(Person person : population.getPersons().values()){
			if(isPersonFromNonSlum(person)){
				filteredPopulation.addPerson(person);
			}
		}
		return filteredPopulation;
	}
	
	public static boolean isPersonFromSlum(final Person person){
		return Arrays.asList( person.getId().toString().split("_") ).contains("slum");
	}
	
	public static boolean isPersonFromNonSlum(final Person person){
		return Arrays.asList( person.getId().toString().split("_") ).contains("nonSlum");
	}
}
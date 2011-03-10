/* *********************************************************************** *
 * project: org.matsim.*
 * TTAStrategyManager.java
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

package playground.telaviv.replanning;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scenario.ScenarioImpl;

/*
 * @cdobler
 * An extended Version of a StrategyManager that handles only
 * non TTA Agents. The implementation is quite a hack - in the near
 * future MATSim should support sub-populations - then it should
 * be possible so implement the functionality of this class much
 * better...
 */
public class TTAStrategyManager extends StrategyManager {

	private Population TTAPopulation;
	
	public TTAStrategyManager(Scenario scenario) {
		super();
		
		createNonTTAPopulation(scenario);
	}
	
	@Override
	protected void beforePopulationRunHook(Population population) {
		for (Person person : TTAPopulation.getPersons().values()) {
			population.getPersons().remove(person.getId());			
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void afterRunHook(Population population) {
		Map<Id, Person> map = (Map<Id, Person>) population.getPersons();
		map.putAll(TTAPopulation.getPersons());
	}
	
	/*
	 * Create a new Population that contains only non Transit Traffic Agents (TTAs).
	 * Therefore TTAs are not replanned.
	 */
	private void createNonTTAPopulation(Scenario scenario) {
		Population fullPopulation = scenario.getPopulation();
		TTAPopulation = new PopulationImpl((ScenarioImpl)scenario);
		
		for (Person person : fullPopulation.getPersons().values()) {			
			// if it is a TTA Agent
			if (person.getId().toString().toLowerCase().contains("tta")) TTAPopulation.addPerson(person);
		}
	}
}

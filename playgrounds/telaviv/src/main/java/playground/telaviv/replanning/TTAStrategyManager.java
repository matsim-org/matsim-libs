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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.StrategyManagerImpl;

public class TTAStrategyManager extends StrategyManagerImpl {

	private Population nonTTAPopulation;
	
	public TTAStrategyManager(Scenario scenario)
	{
		super();
		
		createNonTTAPopulation(scenario);
	}
	
	/*
	 * Run it with the nonTTAPopulation
	 */
	public void run(final Population population) {
		super.run(nonTTAPopulation);
	}
	
	/*
	 * Create a new Population that contains only non Transit Traffic Agents (TTAs).
	 * Therefore TTAs are not replanned.
	 */
	private void createNonTTAPopulation(Scenario scenario)
	{
		Population fullPopulation = scenario.getPopulation();
		nonTTAPopulation = new PopulationImpl((ScenarioImpl)scenario);
		
		for (Person person : fullPopulation.getPersons().values())
		{
			// if it is not a tta Agent
			if (!person.getId().toString().toLowerCase().contains("tta")) nonTTAPopulation.addPerson(person);
		}
	}
}

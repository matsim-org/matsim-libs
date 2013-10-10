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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoutePlanStrategyFactory;
import org.matsim.core.replanning.modules.SelectExpBetaPlanStrategyFactory;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * An extended Version of a StrategyManager that handles only non TTA Agents. 
 * The implementation is quite a hack - in the near future MATSim should support 
 * sub-populations - then it should be possible so implement the functionality of 
 * this class much better...
 * 
 * @cdobler
 */
public class TTAStrategyManager extends StrategyManager {

	private Population TTAPopulation;
	private StrategyManager ttaStrategyManager;
	
	private final List<PlanStrategy> planStrategies;
	
	public TTAStrategyManager(Scenario scenario) {
		super();
		
		this.planStrategies = new ArrayList<PlanStrategy>();
				
		createNonTTAPopulation(scenario);
		createTTAStrategyManager(scenario);
	}
	
	@Override
	protected void beforePopulationRunHook(Population population, ReplanningContext replanningContext) {
				
		// run plan strategies for all TTA agents
		this.ttaStrategyManager.run(this.TTAPopulation, replanningContext);
		
		// remove tta agents from population
		for (Person person : TTAPopulation.getPersons().values()) {
			population.getPersons().remove(person.getId());
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void afterRunHook(Population population) {
		
		// add tta agents again to the population
		Map<Id, Person> map = (Map<Id, Person>) population.getPersons();
		map.putAll(TTAPopulation.getPersons());
	}
	
	/*
	 * Create a new Population that contains only non Transit Traffic Agents (TTAs).
	 * Therefore TTAs are not replanned.
	 */
	private void createNonTTAPopulation(Scenario scenario) {
		Population fullPopulation = scenario.getPopulation();
		TTAPopulation = new PopulationImpl((ScenarioImpl) scenario);
		
		for (Person person : fullPopulation.getPersons().values()) {			
			// if it is a TTA Agent
			if (person.getId().toString().toLowerCase().contains("tta")) TTAPopulation.addPerson(person);
		}
	}
	
	/*
	 * Create a StrategyManager that is used to handle TTA agents.
	 * 10% of them update their routes, 90% select another plan.
	 */
	private void createTTAStrategyManager(Scenario scenario) {
				
		this.ttaStrategyManager = new StrategyManager();
		
		Config config = scenario.getConfig();
		this.ttaStrategyManager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());
		
		EventsManager eventsManager = null;	// not needed for this plan strategies
		PlanStrategy planStrategy;
		
		planStrategy = new ReRoutePlanStrategyFactory().createPlanStrategy(scenario, eventsManager);
		this.ttaStrategyManager.addStrategyForDefaultSubpopulation(planStrategy, 0.1);
		this.planStrategies.add(planStrategy);

		planStrategy = new SelectExpBetaPlanStrategyFactory().createPlanStrategy(scenario, eventsManager);
		this.ttaStrategyManager.addStrategyForDefaultSubpopulation(planStrategy, 0.9);
		this.planStrategies.add(planStrategy);
	}
}

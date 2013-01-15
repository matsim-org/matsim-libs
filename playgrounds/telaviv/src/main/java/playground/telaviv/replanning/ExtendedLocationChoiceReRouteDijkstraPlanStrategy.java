/* *********************************************************************** *
 * project: org.matsim.*
 * ExtendedLocationChoiceReRouteDijkstraPlanStrategy.java
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.telaviv.locationchoice.ExtendedLocationChoicePlanModule;

/*
 * PlanStrategy can be selected by using the full path to this Class in
 * the config file, e.g.:
 * <param name="ModuleProbability_1" value="0.1" />
 * <param name="Module_1" value="playground.telaviv.replanning.ExtendedLocationChoiceReRouteDijkstraPlanStrategy" /> 
 */
public class ExtendedLocationChoiceReRouteDijkstraPlanStrategy implements PlanStrategy {

	private PlanStrategyImpl planStrategyDelegate = null;
	
	public ExtendedLocationChoiceReRouteDijkstraPlanStrategy(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		Network network = controler.getNetwork();
		TravelDisutility travelCostCalc = controler.createTravelCostCalculator();
		TravelTime travelTimeCalc = controler.getLinkTravelTimes();
		Config config = controler.getConfig();
		
		planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());
		planStrategyDelegate.addStrategyModule(new ExtendedLocationChoicePlanModule(scenario, travelTimeCalc));
		planStrategyDelegate.addStrategyModule(new ReRouteDijkstra(controler.getScenario()));
	}
	
	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}

}
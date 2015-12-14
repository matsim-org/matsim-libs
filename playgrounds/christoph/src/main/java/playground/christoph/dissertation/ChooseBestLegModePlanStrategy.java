/* *********************************************************************** *
 * project: org.matsim.*
 * ChooseBestLegModePlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.christoph.dissertation;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import playground.christoph.dissertation.MultiModalDemo.ChooseBestLegModeModule;

/*
 * PlanStrategy can be selected by using the full path to this Class in the config file, e.g.:
 * <param name="ModuleProbability_1" value="0.1" />
 * <param name="Module_1" value="playground.christoph.dissertation.ChooseBestLegModePlanStrategy" /> 
 */
public class ChooseBestLegModePlanStrategy implements PlanStrategy {

	private final PlanStrategyImpl planStrategyDelegate;
	private final ChooseBestLegModeModule chooseBestLegModeModule;
	
	public ChooseBestLegModePlanStrategy(Scenario scenario) {
		
		planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());
		chooseBestLegModeModule = new ChooseBestLegModeModule(scenario, 
				CollectionUtils.stringToSet(MultiModalDemo.legModes));
		
		planStrategyDelegate.addStrategyModule(chooseBestLegModeModule);
		planStrategyDelegate.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
	}
	
	public void setTravelTimes(Map<String, TravelTime> travelTimes) {
		chooseBestLegModeModule.setTravelTimes(travelTimes);
	}
	
	public void setWaitToLinkCalculator(WaitToLinkCalculator waitToLinkCalculator) {
		chooseBestLegModeModule.setWaitToLinkCalculator(waitToLinkCalculator);
	}
	
	public void setTravelDisutilityFactory(TravelDisutilityFactory travelDisutilityFactory) {
		chooseBestLegModeModule.setTravelDisutilityFactory(travelDisutilityFactory);
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
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}

}

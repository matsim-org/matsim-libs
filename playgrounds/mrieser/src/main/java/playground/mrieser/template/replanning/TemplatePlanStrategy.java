/* *********************************************************************** *
 * project: org.matsim.*
 * TemplatePlanStrategy.java
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

package playground.mrieser.template.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class TemplatePlanStrategy implements PlanStrategy {
	
	PlanStrategy planStrategyDelegate = null ;

	/**
	 * @param scenario needed because of the way the class loader works 
	 */
	public TemplatePlanStrategy(Scenario scenario) {
		this.planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());
		this.addStrategyModule(new TemplateStrategyModule());
	}

	public void addStrategyModule(PlanStrategyModule module) {
		this.planStrategyDelegate.addStrategyModule(module);
	}

	public void finish() {
		this.planStrategyDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return this.planStrategyDelegate.getNumberOfStrategyModules();
	}

	public PlanSelector getPlanSelector() {
		return this.planStrategyDelegate.getPlanSelector();
	}

	public void init() {
		this.planStrategyDelegate.init();
	}

	public void run(Person person) {
		this.planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return this.planStrategyDelegate.toString();
	}
}

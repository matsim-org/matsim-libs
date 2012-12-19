/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DReRoutePlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class Sim2DReRoutePlanStrategy implements PlanStrategy {
	
	PlanStrategyImpl planStrategyDelegate = null ;
	
	public Sim2DReRoutePlanStrategy(Controler controler){
		PlanSelector selector = new RandomPlanSelector();
		this.planStrategyDelegate = new PlanStrategyImpl(selector);
		Sim2DReRoutePlanStrategyModule module = new Sim2DReRoutePlanStrategyModule(controler);
		this.planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void run(Person person) {
		this.planStrategyDelegate.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void finish() {
		this.planStrategyDelegate.finish();
	}

}

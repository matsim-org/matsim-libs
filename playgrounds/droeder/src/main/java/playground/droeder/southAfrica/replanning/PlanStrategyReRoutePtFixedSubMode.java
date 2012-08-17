/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.southAfrica.replanning;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * @author droeder
 *
 */
public class PlanStrategyReRoutePtFixedSubMode implements PlanStrategy {
	private static final Logger log = Logger
			.getLogger(PlanStrategyReRoutePtFixedSubMode.class);
	
	private Controler c;
	private RandomPlanSelector selector;
	private List<Plan> plans;
	private List<PlanStrategyModule> modules;
	
	public static final String ORIGINALLEGMODES = "originalLegModes";

	/**
	 * This Strategy reroutes every single leg, as <code>ReRoute</code> would do, but with
	 * a special behavior for pt. As pt consists of many submodes (e.g. bus, train, ...) the 
	 * rerouting is done within the submode defined in a leg.
	 * @param c
	 */
	public PlanStrategyReRoutePtFixedSubMode(Controler c){
		this.c = c;
		this.selector = new RandomPlanSelector();
	}


	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		throw new UnsupportedOperationException("this PlanStrategy is set up with the necessary PlanStrategyModules. Thus it is not allowed to modify them! Abort!");
	}

	@Override
	public int getNumberOfStrategyModules() {
		return this.modules.size();
	}

	@Override
	public void run(Person person) {
		// try to score unscored plans anyway
		Plan p = ((PersonImpl) person).getRandomUnscoredPlan();
		//otherwise get random plan
		if(p == null){
			p = this.selector.selectPlan(person);
		}
		// maybe not necessary, check anyway
		if(p == null){
			log.error("Person " + person.getId() + ". can not select a plan for replanning. this should NEVER happen...");
			return;
		}
		//make the chosen Plan selected and create a deep copy. The copied plan will be selected automatically.
		((PersonImpl)person).setSelectedPlan(p);
		this.plans.add(((PersonImpl)person).copySelectedPlan());
	}

	@Override
	public void init() {
		this.plans = new ArrayList<Plan>();
		this.modules = new ArrayList<PlanStrategyModule>();
		this.modules.add(new FixedSubModePtInteractionRemoverStrategy(this.c));
		this.modules.add(new ReRouteFixedPtSubModeStrategy(this.c));
		// TODO[dr] 'return to old modes'-strategy
	}

	@Override
	public void finish() {
		//run every module for every plan in the order the modules are added to the list
		for(PlanStrategyModule module : this.modules){
			module.prepareReplanning();
			for(Plan plan : this.plans){
				module.handlePlan(plan);
			}
			module.finishReplanning();
		}
		log.info("handled " + this.plans.size() + " plans...");
		this.plans.clear();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return this.selector;
	}

}

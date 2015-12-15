/* *********************************************************************** *
q * project: org.matsim.*
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
package playground.droeder.ptSubModes.replanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;


/**
 * @author droeder
 *
 */
public class PlanStrategyReRoutePtFixedSubMode implements PlanStrategy {
	private static final Logger log = Logger
			.getLogger(PlanStrategyReRoutePtFixedSubMode.class);
	
	private RandomPlanSelector<Plan, Person> selector;
	private List<Plan> plans;
	private List<PlanStrategyModule> modules;
	private Scenario sc;
	private Map<Id, List<String>> originalModes;

	private Provider<TripRouter> tripRouterProvider;

	public static final String ORIGINALLEGMODES = "originalLegModes";

	/**
	 * This Strategy reroutes every single leg, as <code>ReRoute</code> would do, but with
	 * a special behavior for pt. As pt consists of many submodes (e.g. bus, train, ...) the 
	 * rerouting is done within the submode defined in a leg.
	 * @param sc
	 * @param tripRouterProvider
	 */
	public PlanStrategyReRoutePtFixedSubMode(Scenario sc, Provider<TripRouter> tripRouterProvider){
		this.sc = sc;
		this.tripRouterProvider = tripRouterProvider;
		this.selector = new RandomPlanSelector();
		// call in constructor, because should be done only once...
		this.storeOriginalLegModes();
	}


	public void addStrategyModule(PlanStrategyModule module) {
		throw new UnsupportedOperationException("this PlanStrategy is set up with the necessary PlanStrategyModules. Thus it is not allowed to modify them! Abort!");
	}

	public int getNumberOfStrategyModules() {
		return this.modules.size();
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		// try to score unscored plans anyway
		Plan p = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan((person));
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
		((Person)person).setSelectedPlan(p);
		this.plans.add(((Person)person).createCopyOfSelectedPlanAndMakeSelected());
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.plans = new ArrayList<Plan>();
		this.modules = new ArrayList<PlanStrategyModule>();
		// TODO this module is maybe no longer necessary as the pt-routing infrastructure has changed
		this.modules.add(new PtSubModePtInteractionRemoverStrategy(this.sc));
		this.modules.add(new ReturnToOldModesStrategy(this.sc, this.originalModes));
		this.modules.add(new ReRoutePtSubModeStrategy(this.sc, tripRouterProvider));
	}
	
	private void storeOriginalLegModes() {
		this.originalModes = new HashMap<Id, List<String>>();
		for(Person p: this.sc.getPopulation().getPersons().values()){
			List<String> legModes = new ArrayList<String>();
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Leg){
					legModes.add(new String(((Leg) pe).getMode()));
				}
			}
			this.originalModes.put(p.getId(), legModes);
		}
	}

	@Override
	public void finish() {
		//run every module for every plan in the order the modules are added to the list
		for(PlanStrategyModule module : this.modules){
			module.prepareReplanning(null);
			for(Plan plan : this.plans){
				module.handlePlan(plan);
			}
			module.finishReplanning();
		}
		log.info("handled " + this.plans.size() + " plans...");
		this.plans.clear();
	}

	public GenericPlanSelector<Plan, Person> getPlanSelector() {
		return this.selector;
	}

}

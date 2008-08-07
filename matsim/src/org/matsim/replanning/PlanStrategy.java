/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.replanning.modules.StrategyModuleI;
import org.matsim.replanning.selectors.PlanSelectorI;

/**
 * A strategy defines how an agent can be modified during re-planning.
 *
 * @author mrieser
 * @see org.matsim.replanning
 */
public class PlanStrategy {

	private PlanSelectorI planSelector = null;
	private StrategyModuleI firstModule = null;
	private final ArrayList<StrategyModuleI> modules = new ArrayList<StrategyModuleI>();
	private final ArrayList<Plan> plans = new ArrayList<Plan>();
	private long counter = 0;
	private final static Logger log = Logger.getLogger(PlanStrategy.class);

	/**
	 * Creates a new strategy using the specified planSelector.
	 *
	 * @param planSelector
	 */
	public PlanStrategy(final PlanSelectorI planSelector) {
		this.planSelector = planSelector;
	}

	/**
	 * Adds a strategy module to this strategy.
	 *
	 * @param module
	 */
	public void addStrategyModule(final StrategyModuleI module) {
		if (this.firstModule == null) {
			this.firstModule = module;
		} else {
			this.modules.add(module);
		}
	}

	/**
	 * Adds a person to this strategy to be handled. It is not required that
	 * the person is immediately handled during this method-call (e.g. when using
	 * multi-threaded strategy-modules).  This method ensures that an unscored
	 * plan is selected if the person has such a plan ("optimistic behavior").
	 *
	 * @param person
	 * @see #finish()
	 */
	public void run(final Person person) {
		this.counter++;
		Plan plan = person.getRandomUnscoredPlan();
		if (plan == null) {
			plan = this.planSelector.selectPlan(person);
		}
		person.setSelectedPlan(plan);
		if (this.firstModule != null) {
			plan = person.copySelectedPlan();
			this.plans.add(plan);
			// start working on this plan already
			this.firstModule.handlePlan(plan);
		}
	}

	/**
	 * Tells this strategy to initialize its modules. Called before a bunch of
	 * person are handed to this strategy.
	 */
	public void init() {
		if (this.firstModule != null) {
			this.firstModule.init();
		}
	}

	/**
	 * Indicates that no additional persons will be handed to this module and
	 * waits until this strategy has finished handling all persons.
	 *
	 * @see #run()
	 */
	public void finish() {
		if (this.firstModule != null) {
			// finish the first module
			this.firstModule.finish();
			// now work through the others
			for (StrategyModuleI module : this.modules) {
				module.init();
				for (Plan plan : this.plans) {
					module.handlePlan(plan);
				}
				module.finish();
			}
		}
		this.plans.clear();
		log.info("Plan-Strategy finished, " + this.counter + " plans handled. Strategy: " + this.toString());
		this.counter = 0;
	}

	/** Returns a descriptive name for this strategy, based on the class names on the used
	 * {@link PlanSelectorI plan selector} and {@link StrategyModuleI strategy modules}.
	 *
	 * @return An automatically generated name for this strategy.
	 */
	@Override
	public String toString() {
		StringBuffer name = new StringBuffer(20);
		name.append(this.planSelector.getClass().getSimpleName());
		if (this.firstModule != null) {
			name.append('_');
			name.append(this.firstModule.getClass().getSimpleName());
			for (StrategyModuleI module : this.modules) {
				name.append('_');
				name.append(module.getClass().getSimpleName());
			}
		}
		return name.toString();
	}

}

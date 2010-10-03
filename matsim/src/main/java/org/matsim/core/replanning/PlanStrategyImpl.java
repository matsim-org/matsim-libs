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

package org.matsim.core.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * A strategy defines how an agent can be modified during re-planning.
 *
 * @author mrieser
 * @see org.matsim.core.replanning
 */
public final class PlanStrategyImpl implements PlanStrategy {

	private PlanSelector planSelector = null;
	private PlanStrategyModule firstModule = null;
	private final ArrayList<PlanStrategyModule> modules = new ArrayList<PlanStrategyModule>();
	private final ArrayList<Plan> plans = new ArrayList<Plan>();
	private long counter = 0;
	private final static Logger log = Logger.getLogger(PlanStrategyImpl.class);

	/**
	 * Creates a new strategy using the specified planSelector.
	 *
	 * @param planSelector
	 */
	public PlanStrategyImpl(final PlanSelector planSelector) {
		this.planSelector = planSelector;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#addStrategyModule(org.matsim.api.core.v01.replanning.PlanStrategyModule)
	 */
	@Override
	public final void addStrategyModule(final PlanStrategyModule module) {
		if (this.firstModule == null) {
			this.firstModule = module;
		} else {
			this.modules.add(module);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#getNumberOfStrategyModules()
	 */
	@Override
	public final int getNumberOfStrategyModules() {
		if (this.firstModule == null) {
			return 0;
		}
		return this.modules.size() + 1; // we also have to count "firstModule", thus +1
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#run(org.matsim.api.core.v01.population.Person)
	 */
	@Override
	public void run(final Person person) {
		// yyyy "Ablaufsteuerung" should be final (in my view).  kai, sep'10
		this.counter++;
		
		// if there is at least one unscored plan, find that one:
		Plan plan = ((PersonImpl) person).getRandomUnscoredPlan();
		
		// otherwise, find one according to selector (often defined in PlanStrategy ctor):
		if (plan == null) {
			plan = this.planSelector.selectPlan(person);
		}
		
		// "select" that plan:
		((PersonImpl) person).setSelectedPlan(plan);
		
		// if there is a "module" (i.e. "innovation"):
		if (this.firstModule != null) {
			
			// set the working plan to a copy of the selected plan:
			plan = ((PersonImpl) person).copySelectedPlan();
			// (this makes, as a side effect, the _new_ plan selected)
			
			// add that new plan to the agent's plans:
			this.plans.add(plan);

			// start working on this new plan:
			this.firstModule.handlePlan(plan);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#init()
	 */
	@Override
	public final void init() {
		if (this.firstModule != null) {
			this.firstModule.prepareReplanning();
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#finish()
	 */
	@Override
	public final void finish() {
		// yyyy I don't think this needs to be public once StrategyManager.run is final.  kai, sep'10
		if (this.firstModule != null) {
			// finish the first module
				this.firstModule.finishReplanning();
			// now work through the others
			for (PlanStrategyModule module : this.modules) {
				module.prepareReplanning();
				for (Plan plan : this.plans) {
					module.handlePlan(plan);
				}
				module.finishReplanning();
			}
		}
		this.plans.clear();
		log.info("Plan-Strategy finished, " + this.counter + " plans handled. Strategy: " + this.toString());
		this.counter = 0;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#toString()
	 */
	@Override
	public String toString() {
		StringBuffer name = new StringBuffer(20);
		name.append(this.planSelector.getClass().getSimpleName());
		if (this.firstModule != null) {
			name.append('_');
			name.append(this.firstModule.getClass().getSimpleName());
			for (Object module : this.modules) {
				name.append('_');
				name.append(module.getClass().getSimpleName());
			}
		}
		return name.toString();
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.PlanStrategy#getPlanSelector()
	 */
	@Override
	public final PlanSelector getPlanSelector() {
		return planSelector;
	}
	
}

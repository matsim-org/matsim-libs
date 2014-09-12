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

package playground.sergioo.passivePlanning2012.core.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;

/**
 * A strategy defines how an agent can be modified during re-planning.
 *
 * @author mrieser
 * @see org.matsim.core.replanning
 */
public final class PlanStrategyImpl implements PlanStrategy {

	private PlanStrategyModule firstModule = null;
	private final ArrayList<PlanStrategyModule> modules = new ArrayList<PlanStrategyModule>();
	private final ArrayList<Plan> plans = new ArrayList<Plan>();
	private long counter = 0;
	private ReplanningContext replanningContext;
	private final static Logger log = Logger.getLogger(PlanStrategyImpl.class);

	public void addStrategyModule(final PlanStrategyModule module) {
		if (this.firstModule == null) {
			this.firstModule = module;
		} else {
			this.modules.add(module);
		}
	}
	
	public int getNumberOfStrategyModules() {
		if (this.firstModule == null) {
			return 0;
		}
		return this.modules.size() + 1; // we also have to count "firstModule", thus +1
	}
	
	@Override
	public void run(final HasPlansAndId<Plan, Person> person) {
		this.counter++;
		
		// if there is at least one unscored plan, find that one:
		Plan plan = ((BasePerson) person).getBasePlan();
		
		// if there is a "module" (i.e. "innovation"):
		if (this.firstModule != null) {
			
			// set the working plan to a copy of the selected plan:
			plan = person.createCopyOfSelectedPlanAndMakeSelected();
			// (this makes, as a side effect, the _new_ plan selected)
			
			// add new plan to container that contains the plans that are handled by this PlanStrategy:
			this.plans.add(plan);

			// start working on this new plan:
			this.firstModule.handlePlan(plan);
		}
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.replanningContext = replanningContext;
		if (this.firstModule != null) {
			this.firstModule.prepareReplanning(replanningContext);
		}
	}

	@Override
	public void finish() {
		if (this.firstModule != null) {
			// finish the first module
				this.firstModule.finishReplanning();
			// now work through the others
			for (PlanStrategyModule module : this.modules) {
				module.prepareReplanning(replanningContext);
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

	@Override
	public String toString() {
		StringBuilder name = new StringBuilder(20);
		name.append("Base plan");
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
	
}

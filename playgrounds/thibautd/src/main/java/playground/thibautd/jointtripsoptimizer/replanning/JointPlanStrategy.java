/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerModule;
import playground.thibautd.jointtripsoptimizer.scoring.HomogeneousJointScoringFunctionFactory;

/**
 * PlanStrategy aimed at manipulating joint plans and cliques.
 * It mainly reimplements PlanStrategyImpl, but using Cliques instead of
 * PersonImpl agents.
 *
 * To implement a strategy, subclass this class and load modules in the
 * constructor. Then, define the resulting strategy as a module in the
 * config file.
 * This is a workaround, as the {@link StrategyManagerConfigLoader} creates
 * PlanStrategyImpl instances, which do not work with cliques.
 *
 * @author thibautd
 */
public abstract class JointPlanStrategy implements PlanStrategy  {
	private static final Logger log =
		Logger.getLogger(JointPlanStrategy.class);

	protected PlanSelector planSelector;
	private ArrayList<PlanStrategyModule> strategyModules = new ArrayList<PlanStrategyModule>();
	private final ArrayList<Plan> plans = new ArrayList<Plan>();
	private int counter = 0;

	/*
	 * =========================================================================
	 * Interface methods
	 * =========================================================================
	 */
	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		this.strategyModules.add(module);
	}

	@Override
	public int getNumberOfStrategyModules() {
		return this.strategyModules.size();
	}

	@Override
	public void run(Person person) {
		//code taken from the PlanStrategyImpl class, modified to handle cliques
		Clique clique = (Clique) person;
		this.counter++;
		
		// if there is at least one unscored plan, find that one:
		Plan plan = clique.getRandomUnscoredPlan();
		
		// otherwise, find one according to selector (often defined in PlanStrategy ctor):
		if (plan == null) {
			plan = this.planSelector.selectPlan(person);
		}
		
		// "select" that plan:
		clique.setSelectedPlan(plan);
		
		// if there is a "module" (i.e. "innovation"):
		if (this.strategyModules.size() > 0) {
			
			// set the working plan to a copy of the selected plan:
			plan = clique.copySelectedPlan();
			// (this makes, as a side effect, the _new_ plan selected)
			
			// add new plan to container that contains the plans that are handled by this PlanStrategy:
			this.plans.add(plan);
		}
	}

	@Override
	public void init() {
	}

	@Override
	public void finish() {
		for (PlanStrategyModule module : this.strategyModules) {
			module.prepareReplanning();
			for (Plan plan : this.plans) {
				module.handlePlan(plan);
			}
			module.finishReplanning();
		}
		this.plans.clear();
		log.info("Plan-Strategy finished, " + this.counter + " plans handled. Strategy: " + this.toString());
		this.counter = 0;
	}

	@Override
	public String toString() {
		StringBuffer name = new StringBuffer(20);
		name.append(this.planSelector.getClass().getSimpleName());
		for (Object module : this.strategyModules) {
			name.append('_');
			name.append(module.getClass().getSimpleName());
		}
		return name.toString();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return this.planSelector;
	}
}


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

import org.matsim.api.core.v01.Scenario;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerModule;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

import playground.thibautd.jointtripsoptimizer.scoring.HomogeneousJointScoringFunctionFactory;

/**
 * PlanStrategy aimed at manipulating joint plans and cliques.
 * It mainly reimplements PlanStrategyImpl, but using Cliques instead of
 * PersonImpl agents.
 * @author thibautd
 */
public class JointPlanStrategy implements PlanStrategy  {
	private static final Logger log =
		Logger.getLogger(JointPlanStrategy.class);

	//FIXME: requires PersonImpl agents: reimplement everithing.
	PlanStrategy planStrategyDelegate;

	private PlanSelector planSelector;
	private ArrayList<PlanStrategyModule> strategyModules = new ArrayList<PlanStrategyModule>();
	private final ArrayList<Plan> plans = new ArrayList<Plan>();
	private int counter = 0;

	/**
	 * constructor called by the controller.
	 * TODO: pass the plan selector to the scenario by the config file.
	 */
	public JointPlanStrategy(Scenario sc) {
		log.debug("JointPlanStrategy initialized from a scenario");
		//planStrategyDelegate = new PlanStrategyImpl(new BestPlanSelector());
		this.planSelector = new BestPlanSelector();
		//TODO: less hard-coded scoring function factory?
		this.addStrategyModule(new JointPlanOptimizerModule(
					sc.getConfig().global(),
					(JointReplanningConfigGroup) sc.getConfig().getModule(JointReplanningConfigGroup.GROUP_NAME),
					new HomogeneousJointScoringFunctionFactory(
						sc.getConfig().planCalcScore())));
	}

	/*
	 * =========================================================================
	 * Delegate methods
	 * =========================================================================
	 */
	public void addStrategyModule(PlanStrategyModule module) {
		//planStrategyDelegate.addStrategyModule(module);
		this.strategyModules.add(module);
	}

	public int getNumberOfStrategyModules() {
		//return planStrategyDelegate.getNumberOfStrategyModules();
		return this.strategyModules.size();
	}

	public void run(Person person) {
		//code taken from the PlanStrategyImpl class, modified to handle cliques
		//planStrategyDelegate.run(person);
		Clique clique = (Clique) person;
		this.counter++;
		
		// if there is at least one unscored plan, find that one:
		//Plan plan = ((PersonImpl) person).getRandomUnscoredPlan();
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

	public void init() {
		//planStrategyDelegate.init();
	}

	public void finish() {
		//planStrategyDelegate.finish();
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

	public String toString() {
		//return planStrategyDelegate.toString();
		StringBuffer name = new StringBuffer(20);
		name.append(this.planSelector.getClass().getSimpleName());
		for (Object module : this.strategyModules) {
			name.append('_');
			name.append(module.getClass().getSimpleName());
		}
		return name.toString();
	}

	public PlanSelector getPlanSelector() {
		//return planStrategyDelegate.getPlanSelector();
		return this.planSelector;
	}
}


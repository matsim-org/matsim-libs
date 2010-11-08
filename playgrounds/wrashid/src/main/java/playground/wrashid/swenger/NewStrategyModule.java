/* *********************************************************************** *
 * project: org.matsim.*
 * TemplateStrategyModule.java
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

package playground.wrashid.swenger;

import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

public class NewStrategyModule implements PlanStrategyModule {

	HashMap<Person, Double> lastIterationScore = new HashMap<Person, Double>();
	HashMap<Person, String> lastIterationStrategy = new HashMap<Person, String>();

	private final static Logger log = Logger.getLogger(NewStrategyModule.class);

	private final PlanMutateTimeAllocation planMutateTimeAllocation;

	private int counterPlanMutator = 0;

	public static Controler controler;

	private TimeAllocationMutator timeAllocationMutator;

	private ReRoute reRoute;

	private ExpBetaPlanChanger betaExp;

	private RandomPlanSelector randomSelector;

//	private double alphaScale = 0.0; // 0.0
//	private double alphaOffset = 0.8; // 0.8
//	private double betaScale = 0.00; // 0.0
//	private double betaOffset = 0.9; // 0.9
//	private int nbIteration = 50;
	private Double routing0;
	private Double routing1;
	private Double routing2;
	private Double timeMutator0;
	private Double timeMutator1;
	private Double timeMutator2;
	private Double firstStrategyChangeAtIteration;
	private Double secondStrategyChangeAtIteration;

	public NewStrategyModule() {
		this.planMutateTimeAllocation = new PlanMutateTimeAllocation(7200, new Random());
		this.timeAllocationMutator = new TimeAllocationMutator(controler.getConfig(), 7200);
		this.betaExp = new ExpBetaPlanChanger(controler.getConfig().charyparNagelScoring().getBrainExpBeta());
		this.reRoute = new ReRoute(controler);
		this.randomSelector = new RandomPlanSelector();

		Config config = controler.getScenario().getConfig();
		String tempString = null;
		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "routing0");
		routing0 = new Double(tempString);
		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "routing1");
		routing1 = new Double(tempString);
		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "routing2");
		routing2 = new Double(tempString);

		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "timeMutator0");
		timeMutator0 = new Double(tempString);
		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "timeMutator1");
		timeMutator1 = new Double(tempString);
		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "timeMutator2");
		timeMutator2 = new Double(tempString);

		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "firstStrategyChangeAtIteration");
		firstStrategyChangeAtIteration = new Double(tempString);
		tempString = config.getParam("changeStrategyProbabilitiesOverTime", "secondStrategyChangeAtIteration");
		secondStrategyChangeAtIteration = new Double(tempString);

	}

	public void prepareReplanning() {
		this.counterPlanMutator = 0;
	}

	public void handlePlan(final Plan plan) {

		if (controler.getIterationNumber() < firstStrategyChangeAtIteration) {
			double rand = new Random().nextDouble();
			if (rand < (routing0 * (1 - controler.getIterationNumber() / firstStrategyChangeAtIteration) + routing1
					* controler.getIterationNumber() / firstStrategyChangeAtIteration)) {
				betaExp.selectPlan(plan.getPerson());
			} else if (rand < (timeMutator0 * (1 - controler.getIterationNumber() / firstStrategyChangeAtIteration) + timeMutator1
					* controler.getIterationNumber() / firstStrategyChangeAtIteration)) {
				this.randomSelector.selectPlan(plan.getPerson());
				reRoute.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
			} else {
				counterPlanMutator++;
				this.randomSelector.selectPlan(plan.getPerson());
				timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
			}
		} else if (controler.getIterationNumber() < secondStrategyChangeAtIteration) {
			double rand = new Random().nextDouble();
			if (rand < (routing1
					* (1 - controler.getIterationNumber() / (secondStrategyChangeAtIteration - firstStrategyChangeAtIteration)) + routing2
					* controler.getIterationNumber() / (secondStrategyChangeAtIteration - firstStrategyChangeAtIteration))) {
				betaExp.selectPlan(plan.getPerson());
			} else if (rand < (timeMutator1
					* (1 - controler.getIterationNumber() / (secondStrategyChangeAtIteration - firstStrategyChangeAtIteration)) + timeMutator2
					* controler.getIterationNumber() / (secondStrategyChangeAtIteration - firstStrategyChangeAtIteration))) {
				this.randomSelector.selectPlan(plan.getPerson());
				reRoute.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
			} else {
				counterPlanMutator++;
				this.randomSelector.selectPlan(plan.getPerson());
				timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
			}
		} else {
			double rand = new Random().nextDouble();
			if (rand < routing2) {
				betaExp.selectPlan(plan.getPerson());
			} else if (rand < timeMutator2) {
				this.randomSelector.selectPlan(plan.getPerson());
				reRoute.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
			} else {
				counterPlanMutator++;
				this.randomSelector.selectPlan(plan.getPerson());
				timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
			}
		}

		/*
		 * if (controler.getIterationNumber() % 10 < 6) {
		 * betaExp.selectPlan(plan.getPerson()); } else if
		 * (controler.getIterationNumber() % 10 < 8) {
		 * reRoute.getPlanAlgoInstance().run(plan);
		 * 
		 * } else { counterPlanMutator++;
		 * timeAllocationMutator.getPlanAlgoInstance().run(plan); }
		 */

		// this.randomSelector.selectPlan(plan.getPerson());
		// timeAllocationMutator.getPlanAlgoInstance().run(plan);

		//

//		if (controler.getIterationNumber() < nbIteration) {
//			if (new Random().nextDouble() < (alphaOffset + alphaScale * controler.getIterationNumber() / nbIteration)) {
//				betaExp.selectPlan(plan.getPerson());
//			} else if (new Random().nextDouble() < (betaOffset + betaScale * controler.getIterationNumber() / nbIteration)) { // Problem
//																																// :
//																																// random
//																																// number
//																																// dependent
//																																// of
//																																// the
//																																// previous
//																																// number
//				this.randomSelector.selectPlan(plan.getPerson());
//				reRoute.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
//			} else {
//				counterPlanMutator++;
//				this.randomSelector.selectPlan(plan.getPerson());
//				timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
//			}
//		} else {
//			if (new Random().nextDouble() < alphaOffset + alphaScale) {
//				betaExp.selectPlan(plan.getPerson());
//			} else if (new Random().nextDouble() < betaOffset + betaScale) {
//				this.randomSelector.selectPlan(plan.getPerson());
//				reRoute.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
//			} else {
//				counterPlanMutator++;
//				this.randomSelector.selectPlan(plan.getPerson());
//				timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
//			}
//		}

		/*
		 * if (lastIterationScore.get(plan.getPerson())>plan.getScore()){
		 * 
		 * }
		 * 
		 * if
		 * (lastIterationStrategy.get(plan.getPerson()).equalsIgnoreCase("mutator"
		 * )){
		 * 
		 * }
		 * 
		 * 
		 * lastIterationStrategy.put(plan.getPerson(), "mutator");
		 * 
		 * lastIterationScore.put(plan.getPerson(), plan.getScore());
		 */
		// timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());

	}

	public void finishReplanning() {
//		log.info("number of handled plans (PlanMutateTimeAllocation): " + this.counterPlanMutator);
	}

}

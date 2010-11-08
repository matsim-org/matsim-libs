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

	private int counterPlanMutator = 0;

	public static Controler controler;

	private TimeAllocationMutator timeAllocationMutator;

	private ReRoute reRoute;

	private ExpBetaPlanChanger betaExp;

	private RandomPlanSelector randomSelector;

	private Double routing0;
	private Double routing1;
	private Double routing2;
	private Double timeMutator0;
	private Double timeMutator1;
	private Double timeMutator2;
	private Double firstStrategyChangeAtIteration;
	private Double secondStrategyChangeAtIteration;

	public NewStrategyModule() {
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
		
		double probabilityOfRouting;
		double probabilityOfTimeMutator;
		double lambdaParameter; 
		if (controler.getIterationNumber() < firstStrategyChangeAtIteration){
			lambdaParameter = controler.getIterationNumber() / firstStrategyChangeAtIteration;
			probabilityOfRouting = routing0 * (1 - lambdaParameter) + routing1 * lambdaParameter;
			probabilityOfTimeMutator = timeMutator0 * (1 - lambdaParameter) + timeMutator1 * lambdaParameter;
		} else if (controler.getIterationNumber() < secondStrategyChangeAtIteration){
			lambdaParameter = (controler.getIterationNumber() - firstStrategyChangeAtIteration) / (secondStrategyChangeAtIteration - firstStrategyChangeAtIteration);
			probabilityOfRouting = routing1	* (1 - lambdaParameter) + routing2 * lambdaParameter;
			probabilityOfTimeMutator = timeMutator1	* (1 - lambdaParameter) + timeMutator2 * lambdaParameter;
		} else {
			probabilityOfRouting = routing2;
			probabilityOfTimeMutator = timeMutator2;
		}
		
		double rand = new Random().nextDouble();
		if (rand < probabilityOfRouting) {
			this.randomSelector.selectPlan(plan.getPerson());
			reRoute.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
		} else if (rand < probabilityOfTimeMutator + probabilityOfRouting) {
			counterPlanMutator++;
			this.randomSelector.selectPlan(plan.getPerson());
			timeAllocationMutator.getPlanAlgoInstance().run(plan.getPerson().getSelectedPlan());
		} else {
			betaExp.selectPlan(plan.getPerson());
		}
	}

	public void finishReplanning() {

	}

}

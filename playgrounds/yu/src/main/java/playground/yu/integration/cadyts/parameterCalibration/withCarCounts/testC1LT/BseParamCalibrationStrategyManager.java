/* *********************************************************************** *
 * project: org.matsim.*
 * DummyBseParamCalibrationStrategyManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.PlanToPlanStep;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;

@Deprecated // use material in contrib.cadytsintegration instead
public abstract class BseParamCalibrationStrategyManager extends
		StrategyManager implements BseStrategyManager {

	protected CPC1<Link> calibrator = null;

	protected PlanToPlanStep planConverter = null;
	protected CadytsChoice chooser;
	protected int iter;
	protected final int firstIter;
	// protected RemoveWorstPlanSelector worstPlanSelector;
	protected Network net;
	protected double[] statistics;
	protected TravelTime tt;

	private final List<Tuple<Id, Plan>> removeds = new ArrayList<Tuple<Id, Plan>>();

	public BseParamCalibrationStrategyManager(int firstIteration) {
		firstIter = firstIteration;
		iter = firstIteration;
		throw new RuntimeException("this won't work any more since the afterRemovePlanHook is no longer there. kai, nov'13") ;
	}

	// @Override
	// protected void removePlans(PersonImpl person, int maxNumberOfPlans) {
	// while (person.getPlans().size() > maxNumberOfPlans) {
	// Plan plan = this.worstPlanSelector.selectPlan(person);
	// person.getPlans().remove(plan);
	// if (plan == person.getSelectedPlan()) {
	// person.setSelectedPlan(person.getRandomPlan());
	// }
	// }
	// }
//	@Override
//	protected void afterRemovePlanHook(Plan plan) {
//		removeds.add(new Tuple<Id, Plan>(plan.getPerson().getId(), plan));
//	}

	@Override
	protected void beforePopulationRunHook(Population population, ReplanningContext replanningContext) {
		iter++;
	}

	/**
	 * only for the Agent with number of plans == choiceSetSize or
	 * maxPlansPerAgent
	 * 
	 * @param person
	 * @return
	 */
	protected List<cadyts.demand.Plan<Link>> getPlanChoiceSet(PersonImpl person) {
		List<cadyts.demand.Plan<Link>> choiceSet = new ArrayList<cadyts.demand.Plan<Link>>();
		List<Plan> plans = person.getPlans();
		for (int i = 0; i < plans.size(); i++) {
			planConverter.convert((PlanImpl) plans.get(i));
			choiceSet.add(i, planConverter.getPlanSteps());
		}
		return choiceSet;
	}

	/**
	 * creates a {@code cadyts.demand.Plan<Link>} choice set, which contains
	 * only one {@code cadyts.demand.Plan<Link>}
	 * 
	 * @param plan
	 * @return
	 */
	protected List<cadyts.demand.Plan<Link>> getSinglePlanChoiceSet(Plan plan) {
		List<cadyts.demand.Plan<Link>> choiceSet = new ArrayList<cadyts.demand.Plan<Link>>();
		planConverter.convert((PlanImpl) plan);
		choiceSet.add(planConverter.getPlanSteps());
		return choiceSet;
	}

	public double[] getStatistics() {
		return statistics;
	}

	@Override
	public void init(final CPC1<Link> calibrator, final TravelTime travelTimes) {
		this.calibrator = calibrator;
		planConverter = new PlanToPlanStep(travelTimes, net);
		tt = travelTimes;
		// this.worstPlanSelector = new RemoveWorstPlanSelector();
	}

	protected void resetChooser() {
		// chooser.reset(iter);
		chooser.reset(removeds);
		removeds.clear();
	}

	public void setChooser(CadytsChoice chooser) {
		this.chooser = chooser;
	}
}
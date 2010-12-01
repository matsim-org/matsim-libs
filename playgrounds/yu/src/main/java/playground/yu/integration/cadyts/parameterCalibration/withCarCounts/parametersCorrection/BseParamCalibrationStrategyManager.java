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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.PlanToPlanStep;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
import cadyts.calibrators.Calibrator;
import cadyts.calibrators.analytical.AnalyticalCalibrator;

public abstract class BseParamCalibrationStrategyManager extends
		StrategyManager implements BseStrategyManager {

	protected AnalyticalCalibrator<Link> calibrator = null;

	protected PlanToPlanStep planConverter = null;
	protected CadytsChoice chooser;
	protected int iter;
	protected final int firstIter;
	// protected RemoveWorstPlanSelector worstPlanSelector;
	protected NetworkImpl net;
	protected double[] statistics;
	protected TravelTime tt;

	private List<Tuple<Id, Plan>> removeds = new ArrayList<Tuple<Id, Plan>>();

	public BseParamCalibrationStrategyManager(int firstIteration) {
		firstIter = firstIteration;
		iter = firstIteration;
	}

	public void init(final Calibrator<Link> calibrator,
			final TravelTime travelTimes) {
		this.calibrator = (AnalyticalCalibrator<Link>) calibrator;
		planConverter = new PlanToPlanStep(travelTimes, net);
		tt = travelTimes;
		// this.worstPlanSelector = new RemoveWorstPlanSelector();
	}

	public void setChooser(CadytsChoice chooser) {
		this.chooser = chooser;
	}

	protected void beforePopulationRunHook(Population population) {
		iter++;
	}

	public double[] getStatistics() {
		return statistics;
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
	protected void afterRemovePlanHook(Plan plan) {
		removeds.add(new Tuple<Id, Plan>(plan.getPerson().getId(), plan));
	}

	protected void resetChooser() {
		chooser.reset(iter);
		chooser.reset(removeds);
		removeds.clear();
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
}
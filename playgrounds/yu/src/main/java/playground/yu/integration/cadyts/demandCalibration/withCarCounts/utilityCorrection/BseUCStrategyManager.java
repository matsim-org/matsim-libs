/* *********************************************************************** *
 * project: org.matsim.*
 * BseARStrategyManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utilityCorrection;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseStrategyManager;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.PlanToPlanStep;
import playground.yu.utils.io.SimpleWriter;
import cadyts.calibrators.Calibrator;
import cadyts.demand.PlanStep;
import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;

public class BseUCStrategyManager extends StrategyManager implements
		BseStrategyManager {
	private MATSimUtilityModificationCalibrator<Link> calibrator = null;
	private PlanToPlanStep planConverter = null;
	// private int maxDraws = 100;
	// private final PlanSelector worstPlanSelector = new
	// WorstPlanForRemovalSelector();
	// private int correctCnt = 0, correctMileStone = 0;
	private double beta;
	/* container for the old "corrections" */
	// private final Map<PlanImpl, Double> oldCorrections = new
	// HashMap<PlanImpl, Double>();
	private Network net;
	// #####################DEPRECATED################################
	// private Map<Id, ChoiceSampler<Link>> samplers = null;
	// #####################DEPRECATED################################
	private Map<Id, Double> personUtilityOffsets = null;
	// private String scoreModificationFilename = null;
	// ////////////////////////////////////
	private SimpleWriter writer = null;
	private String line = null;
	protected int iter;

	// ////////////////////////////////////////
	// public void setScoreModificationFilename(String
	// scoreModificationFilename) {
	// this.scoreModificationFilename = scoreModificationFilename;
	// }

	public Map<Id, Double> getPersonUtilityOffsets() {
		return personUtilityOffsets;
	}

	public BseUCStrategyManager(Network net, int iteration) {
		this.net = net;
		iter = iteration;
	}

	public void init(final Calibrator<Link> calibrator,
			final TravelTime travelTimes, final double brainExpBeta) {
		this.calibrator = (MATSimUtilityModificationCalibrator<Link>) calibrator;
		planConverter = new PlanToPlanStep(travelTimes, net);
		beta = brainExpBeta;
	}

	/*
	 * public void setMaxDraws(final int maxDraws) { // CHECK if (maxDraws < 1)
	 * throw new IllegalArgumentException(
	 * "maximum number of draws must be at least 1"); // CONTINUE this.maxDraws
	 * = maxDraws; }
	 * 
	 * public int getMaxDraws() { return maxDraws; }
	 */

	// /**
	// * this is a copy of void
	// * org.matsim.core.replanning.StrategyManager.removePlans(Person person,
	// int
	// * maxNumberOfPlans), except:{@code oldCorrections.remove(plan);}
	// *
	// * @param person
	// * @param maxNumberOfPlans
	// */
	// protected void removePlans(final PersonImpl person,
	// final int maxNumberOfPlans) {
	// while (person.getPlans().size() > maxNumberOfPlans) {
	// PlanImpl plan = (PlanImpl) worstPlanSelector.selectPlan(person);
	// person.getPlans().remove(plan);
	// if (plan == person.getSelectedPlan())
	// person.setSelectedPlan(person.getRandomPlan());
	// // remove oldCorrection of the removed plan
	//			
	// }
	// }

	// @Override
	// protected void afterRemovePlanHook(Plan plan) {
	// oldCorrections.remove(plan);
	// }

	@Override
	protected void beforePopulationRunHook(Population population) {
		iter++;

		// ////////////////////////////////
		writer = new SimpleWriter(
				"../integration-parameterCalibration/test/cali3/C3Step0TestLocal_DC2/corrections.it"
						+ iter + ".log");
		writer
				.writeln("personId\tplanIndex\toldPlanScore\tnewPlanScore\tcurrentCorrection\tplanStep");
		// /////////////////////////////////
		// #####################DEPRECATED################################
		// this.samplers = new HashMap<Id, ChoiceSampler<Link>>();
		// #####################DEPRECATED################################
		// then go through the population and assign each person to a strategy
		for (Person person : population.getPersons().values()) {
			// #####################DEPRECATED################################
			// save ChoiceSampler<Link>
			// this.samplers.put(person.getId(), calibrator.getSampler(person));
			// #####################DEPRECATED################################
			// before removePlans and plan choice, make utilityCorrection
			correctPlansUtilities((PersonImpl) person);
		}
	}

	// @Override
	// public void
	// playground.yu.integration.cadyts.demandCalibration.withCarCounts.run(final
	// Population population) {
	// // initialize all strategies
	// for (PlanStrategy strategy : getStrategies())
	// strategy.init();
	// int maxPlansPerAgent = getMaxPlansPerAgent();
	// // TODO shift to beforePopulationRunHook
	// Map<Id, ChoiceSampler<Link>> samplers = new HashMap<Id,
	// ChoiceSampler<Link>>();
	// // then go through the population and assign each person to a strategy
	// for (Person person : population.getPersons().values()) {
	// // save ChoiceSampler<Link>
	// samplers.put(person.getId(), calibrator.getSampler(person));
	// // before removePlans and plan choice, make utilityCorrection
	// correctPlansUtilities((PersonImpl) person);
	// // ///////////////////////////////////////////////////
	// // removePlans
	// if (maxPlansPerAgent > 0
	// && person.getPlans().size() > maxPlansPerAgent)
	// removePlans((PersonImpl) person, maxPlansPerAgent);
	// //
	// strategy-playground.yu.integration.cadyts.demandCalibration.withCarCounts.run
	// PlanStrategy strategy = chooseStrategy();
	// if (strategy != null)
	// strategy.run(person);
	// else
	// Gbl.errorMsg("No strategy found!");
	// }
	// // finally make sure all strategies have finished there work
	// for (PlanStrategy strategy : getStrategies())
	// strategy.finish();
	// // TODO shift to afterRunHook
	// // now do the "enforcAccept" for every person
	// for (Person person : population.getPersons().values()) {
	// // convert the newly selected plan
	// planConverter.convert((PlanImpl) person.getSelectedPlan());
	// ChoiceSampler<Link> cs = samplers.remove(person.getId());
	// if (cs != null) {
	// cs.enforceNextAccept();
	// cs.isAccepted(planConverter.getPlanSteps());
	// }
	// }
	// }

	@Override
	protected void afterRunHook(Population population) {
		writer.close();
		// now do the "enforcAccept" for every person
		for (Person person : population.getPersons().values()) {
			// convert the newly selected plan
			planConverter.convert((PlanImpl) person.getSelectedPlan());
			// #####################DEPRECATED################################
			// ChoiceSampler<Link> cs = samplers.remove(person.getId());
			// if (cs != null) {
			// cs.enforceNextAccept();
			// cs.isAccepted(planConverter.getPlanSteps());
			// #####################DEPRECATED################################
			calibrator.registerChoice(planConverter.getPlanSteps());
			// }
		}
		// this.samplers = null;
	}

	/**
	 * corrects the scores of all the plans except the one selected in the
	 * choice set of a Person
	 * 
	 * @param person
	 */
	public void correctPlansUtilities(final PersonImpl person) {
		for (Plan plan : person.getPlans()) {
			// /////////////////////////////////////////
			line = person.getId() + "\t" + person.getPlans().indexOf(plan)
					+ "\t";
			// /////////////////////////////////////////
			correctPlanScore(plan);
		}
	}

	// private void correctPlanScore(Plan plan) {
	// planConverter.convert((PlanImpl) plan);
	// cadyts.demand.Plan<Link> planSteps = planConverter.getPlanSteps();
	// // ///////////////////////////////////////////////
	// Double oldCorrection = oldCorrections.get(plan);
	// if (oldCorrection == null || plan.isSelected()) {
	// oldCorrection = 0.0;
	// }
	// // revert the score of plans, those were not selected, because the
	// // plan selected was already newly scored
	// double oldPlanScore = plan.getScore() - oldCorrection;
	//
	// // /////////////////////////////////////////////////
	// double correction = calibrator.getUtilityCorrection(planSteps) / beta;
	//
	// double newPlanScore = oldPlanScore + correction;
	// plan.setScore(newPlanScore);
	// // //////////////////////////////////////////
	//
	// // update correction in oldCorrections
	// oldCorrections.put((PlanImpl) plan, correction);
	//
	// // output log
	// if (correction != 0.0) {
	// correctCnt++;
	// if (correctCnt == correctMileStone) {
	// correctMileStone = correctCnt * 2;
	// System.out.println("UtilityCorrection : handled person\t"
	// + correctCnt);
	// System.out.println("UtilityCorrection : old plan score\t"
	// + oldPlanScore);
	// System.out
	// .println("UtilityCorrection : utility correction (offset)\t"
	// + correction);
	// System.out.println("UtilityCorrection : new plan score\t"
	// + newPlanScore);
	// }
	// }
	// }
	private void correctPlanScore(Plan plan) {
		planConverter.convert((PlanImpl) plan);
		cadyts.demand.Plan<Link> planSteps = planConverter.getPlanSteps();
		double scoreCorrection = calibrator
				.getUtilityCorrection(planSteps)
				/ beta;

		// ################only for Dummy Tests################
		// ##
		// revert the score of plans, those were not selected, because the
		// plan selected was already newly scored
		Double oldCorrection = (Double) plan.getCustomAttributes().get(
				"oldCorrection");
		if (oldCorrection == null || plan.isSelected()) {
			oldCorrection = 0.0;
		}
		double oldPlanScore = plan.getScore() - oldCorrection;
		// ##
		// ################only for Dummy Tests################

		// plan.setScore(plan.getScore() + scoreCorrection);//for not dummy
		// tests, score should have been set by
		// this.chooser.setPersonScore(person), that means that every score has
		// been newly calculated, so don't need to reduce oldCorrection;

		plan.setScore(oldPlanScore + scoreCorrection);
		// update correction in oldCorrections
		plan.getCustomAttributes().put("oldCorrection", scoreCorrection);

		writer.writeln(line + "\t" + oldPlanScore + "\t"
				+ (oldPlanScore + scoreCorrection) + "\t" + scoreCorrection
				+ planStepsToString(planSteps));
	}

	private static String planStepsToString(cadyts.demand.Plan<Link> planSteps) {
		StringBuffer stringBuffer = new StringBuffer();
		int size = planSteps.size();
		for (int i = 0; i < size; i++) {
			stringBuffer.append("\t" + planStepToString(planSteps.getStep(i)));
		}
		return stringBuffer.toString();
	}

	private static String planStepToString(PlanStep<Link> planStep) {
		return "[" + planStep.getEntryTime_s() + "\t,\t"
				+ planStep.getLink().getId() + "]";

	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * PCStrMn.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.paramCorrection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.router.util.TravelTime;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.PlanToPlanStep;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalNormal.scoring.Events2Score4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationStrategyManager;
import cadyts.calibrators.analytical.ChoiceParameterCalibrator;
import cadyts.interfaces.matsim.MATSimChoiceParameterCalibrator;
import cadyts.utilities.math.BasicStatistics;
import cadyts.utilities.math.Matrix;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

public class PCStrMn extends BseParamCalibrationStrategyManager implements
		BseStrategyManager {
	private final static Logger log = Logger.getLogger(PCStrMn.class);
	private double delta;
	private final double brainExpBeta;
	// private ChoiceParameterCalibrator3<Link> calibrator = null;
	private final int paramDimension;
	private Plan oldSelected = null;
	private BasicStatistics travelingCarStats = null, performingStats = null;

	public PCStrMn(NetworkImpl net, int firstIteration, double brainExpBeta,
			int paramDimension) {
		super(firstIteration);
		this.net = net;
		this.brainExpBeta = brainExpBeta;
		this.paramDimension = paramDimension;
	}

	public void init(ChoiceParameterCalibrator<Link> calibrator,
			TravelTime travelTimeCalculator, MultinomialLogitChoice chooser,
			double delta) {
		// init(calibrator, travelTimeCalculator);
		this.calibrator = calibrator;
		planConverter = new PlanToPlanStep(travelTimeCalculator, net);
		tt = travelTimeCalculator;
		// this.worstPlanSelector = new RemoveWorstPlanSelector();
		// /////////////////////////////////////////////////////////////////////
		this.chooser = chooser;
		this.delta = delta;
	}

	@Override
	protected void beforePopulationRunHook(Population population) {
		// the most things before "removePlans"
		super.beforePopulationRunHook(population);// iter++
		// cadyts class - create new BasicStatistics Objects
		travelingCarStats = new BasicStatistics();
		performingStats = new BasicStatistics();
		for (Person person : population.getPersons().values()) {
			/* ***********************************************************
			 * scoringCfg has been done, but they should be newly defined
			 * because of new calibrated parameters and -- WITHOUT
			 * utilityCorrections--
			 * *******************************************************
			 */
			chooser.setPersonScore(person);
			// now there could be #maxPlansPerAgent+?# Plans in choice set
			// *********************UTILITY CORRECTION********************
			// ***before removePlans and plan choice, correct utility***
			correctPersonPlansScores(person);
			/* ******************************************************** */
		}
	}

	@Override
	protected void afterRemovePlanHook(Plan plan) {
		super.afterRemovePlanHook(plan);
		// something could be done here.
	}

	@Override
	protected void beforeStrategyRunHook(Person person, PlanStrategy strategy) {
		// choose reset because of removeWorstPlan
		resetChooser();
		// ******************************************************
		super.beforeStrategyRunHook(person, strategy);

		if (strategy != null) {
			int maxPlansPerAgent = getMaxPlansPerAgent();
			if (iter - firstIter > maxPlansPerAgent) {
				// ENSURE THAT EVERY PLAN IN CHOICE SET HAS BEEN SIMULATED
				// ATLEAST ONE TIME
				oldSelected = person.getSelectedPlan();

				if (strategy.getNumberOfStrategyModules() <= 0) {
					// only with planSelector/-Changer, no new plan will be
					// created
					// **************WRITE ATTR.S INTO MNL******************
					chooser.setPersonAttrs(person);
					// now there are only #maxPlansPerAgent# Plans in choice set

					/* ***********************************************************
					 * set the last chosen plan to cadyts, only works with
					 * {@code cadyts.interfaces.matsim.ExpBetaPlanChanger},it's
					 * not to be done with MNL, but "ChangeExpBeta" as well as
					 * "SelectExpBeta" can still be written in configfile
					 */
				}
			} else {// ***********iter-firstIter<=maxPlanPerAgent************
				if (iter - firstIter == 1) {
					/*
					 * shuffle the Plan Choice Set, to avoid chaotic network
					 * situation
					 */
					Collections.shuffle(person.getPlans(), MatsimRandom
							.getRandom()/*
										 * Random-Objekt is not generated by
										 * every calling of shuffle(List)
										 */);
				}
				// ENSURE THAT EVERY PLANS IN CHOICE SET WILL BE EXECUTED ONE
				// TIME
				((PersonImpl) person).setSelectedPlan(person.getPlans().get(
						iter % maxPlansPerAgent));
				// ****************************************************
			}
		} else { // strategy==null
			Gbl.errorMsg("No strategy found!");
		}
	}

	@Override
	protected void afterStrategyRunHook(Person person, PlanStrategy strategy) {
		super.afterStrategyRunHook(person, strategy);
		if (strategy != null) {
			if (iter - firstIter > getMaxPlansPerAgent()) {
				// ENSURE THAT EVERY PLAN IN CHOICE SET HAS BEEN SIMULATED
				// ATLEAST ONE TIME
				if (strategy.getNumberOfStrategyModules() > 0) {
					/*
					 * New plan has been created by e.g. ReRoute,
					 * TimeAllocationMutator etc. Only the old score of last
					 * selected Plan will set to the new created Plan.
					 */
					Plan selectedPlan = person.getSelectedPlan();
					selectedPlan.setScore(oldSelected.getScore());
					oldSelected = null;

					Vector p = new Vector(1/* (single-)choiceSetSize */);
					p.set(0, 1d/* 100% */);

					Matrix d = new Matrix(1/* n-choiceSetSize */,
							paramDimension
					// m-size of parameters that has to be calibrated
					);
					for (int i = 0; i < paramDimension; i++) {
						d.setColumn(i, new Vector(0d));
					}

					// ******************************************************
					((ChoiceParameterCalibrator<Link>) calibrator)
							.selectPlan(0,
									getSinglePlanChoiceSet(selectedPlan), p, d,
									null);
					// **********************************************************
				} else {// Change-/SelectExpBeta has been done.
					int selectIdx = person.getPlans().indexOf(
							person.getSelectedPlan());
					// ********************************************************
					MultinomialLogit mnl = ((MultinomialLogitChoice) chooser)
							.getMultinomialLogit();
					Vector probs = mnl.getProbs();

					if (Double.isNaN(probs.sum())) {
						log.fatal("mnl/probs/NaN");
						System.out
								.println("selecteIdx from ChangeExpBeta (MATSim)\t"
										+ selectIdx
										+ "\nprobs\n"
										+ probs
										+ "\nperson\t"
										+ person.getId()
										+ "\nplans:");
						List<? extends Plan> plans = person.getPlans();
						for (int i = 0; i < plans.size(); i++) {
							System.out.print(i + ". plan with score\t"
									+ plans.get(i).getScore());
							if (plans.get(i).isSelected()) {
								System.out.println("\tselected");
							} else {
								System.out.println();
							}
						}
					}

					List<Integer> attrIndices = new ArrayList<Integer>();
					for (String paramName : PCCtlListener.paramNames) {
						attrIndices.add(Events2Score4PC.attrNameList
								.indexOf(paramName));
					}

					Matrix dProb_dParameters = mnl.get_dProb_dParameters(
							attrIndices, false/* without ASC */);

					List<? extends Matrix> d2ChoiceProb_dParam2 = mnl
							.get_d2P_dbdb(delta, attrIndices, false);
					/* UPDATE PARAMETERS (OBSERVE THE PLAN CHOOSING IN MATSIM) */

					/* int selectedIdx= */((ChoiceParameterCalibrator<Link>) calibrator)
							.selectPlan(selectIdx,
									getPlanChoiceSet((PersonImpl) person),
									probs, dProb_dParameters,
									d2ChoiceProb_dParam2);
					// ***************************************************
				}
			}
		} else {// strategy==null
			Gbl.errorMsg("No strategy found!");
		}
	}

	// @Override
	// public void
	// playground.yu.integration.cadyts.demandCalibration.withCarCounts.run(final
	// Population population) {
	// this.beforePopulationRunHook(population);
	// for (PlanStrategy strategy : getStrategies())
	// strategy.init();
	//
	// int maxPlansPerAgent = this.getMaxPlansPerAgent();
	//
	// // cadyts class - create new BasicStatistics Objects
	// BasicStatistics travelingCarStats = new BasicStatistics(),
	// performingStats = new BasicStatistics();
	//
	// // then go through the population and assign each person to a strategy
	// for (Person person : population.getPersons().values()) {
	// /* ***********************************************************
	// * scoringCfg has been done, but they should be newly defined
	// * because of new calibrated parameters and -- WITHOUT
	// * utilityCorrections--
	// * *******************************************************
	// */
	// this.chooser.setPersonScore(person);
	// // *********************UTILITY CORRECTION********************
	// // ***before removePlans and plan choice, correct utility***
	// correctPersonPlansScores(person);
	// /* ******************************************************** */
	// // remove worst plans
	// if ((maxPlansPerAgent > 0)
	// && (person.getPlans().size() > maxPlansPerAgent)) {
	// // todo
	// removePlans((PersonImpl) person, maxPlansPerAgent);
	// // todo
	// }
	// // choose reset because of removeWorstPlan
	// this.resetChooser();
	//
	// List<? extends Plan> plans = person.getPlans();
	//
	// PlanStrategy strategy = this.chooseStrategy();
	// if (strategy != null) {
	// if (this.iter - this.firstIter > maxPlansPerAgent) {
	// // ENSURE THAT EVERY PLAN IN CHOICE SET HAS BEEN SIMULATED
	// // ATLEAST ONE TIME
	// Plan oldSelected = person.getSelectedPlan();
	//
	// if (strategy.getNumberOfStrategyModules() > 0) {
	// /*
	// * New plan will be created by e.g. ReRoute,
	// * TimeAllocationMutator etc. It's not needed to use
	// * MultinomialLogit or cadyts.....ExpBetaPlanChanger.
	// * Only a tough estimate about score of new plan is
	// * necessary
	// */
	// strategy.run(person);
	// Plan selectedPlan = person.getSelectedPlan();
	// selectedPlan.setScore(oldSelected.getScore());
	//
	// Vector p = new Vector(1/* (single-)choiceSetSize */);
	// p.set(0, 1d/* 100% */);
	//
	// Matrix d = new Matrix(1/* n-choiceSetSize */,
	// this.paramDimension/*
	// * m-size of parameters that
	// * has to be calibrated
	// */);
	// for (int i = 0; i < this.paramDimension; i++)
	// d.setColumn(i, new Vector(0d));
	//
	// // ******************************************************
	// ((ChoiceParameterCalibrator3<Link>) this.calibrator)
	// .selectPlan(0, this
	// .getSinglePlanChoiceSet(selectedPlan),
	// p, d, null);
	// // **********************************************************
	// } else {// only with planSelector/-Changer, no new plan will
	// // be created
	// // **************WRITE ATTR.S INTO MNL******************
	// this.chooser.setPersonAttrs(person);
	//
	// /* ***********************************************************
	// * set the last chosen plan to cadyts, only works with
	// * {@code cadyts.interfaces.matsim.ExpBetaPlanChanger},
	// * but "ChangeExpBeta" as well as "SelectExpBeta" can
	// * still be written in configfile
	// */
	// // ***PLAN CHOOSING --WITH UTILITY CORRECTION--***
	//
	// strategy.run(person);
	// int selectIdx = plans.indexOf(person.getSelectedPlan());
	// // ********************************************************
	// MultinomialLogit mnl = ((MultinomialLogitChoice) this.chooser)
	// .getMultinomialLogit();
	// Vector probs = mnl.getProbs();
	// Matrix dProb_dParameters = mnl
	// .get_dProb_dParameters(Arrays.asList(
	// 0/* traveling */, 2/* performing */),
	// false/* without ASC */);
	// if (Double.isNaN(probs.get(0))
	// || Double.isNaN(probs.get(1))) {
	// log.fatal(
	// "mnl/probs/NaN");
	// System.out
	// .println("selecteIdx from ExpBetaPlanChanger\t"
	// + selectIdx
	// + "\nprobs\n"
	// + probs
	// + "\ndProb_dParameters\n"
	// + dProb_dParameters
	// + "\nperson\t"
	// + person.getId() + "\nplans:");
	// int n = 0;
	// for (Plan plan : person.getPlans()) {
	// System.out.println(n + "\t" + plan);
	// n++;
	// }
	// System.out.println("mnl_attrs:\n"
	// + mnl.getAttrCount());
	// // System.exit(185);
	// }
	//
	// /*
	// * UPDATE PARAMETERS (OBSERVE THE PLAN CHOOSING IN
	// * MATSIM)
	// */
	// /* int selectedIdx= */((ChoiceParameterCalibrator3<Link>)
	// this.calibrator)
	// .selectPlan(
	// selectIdx,
	// getPlanChoiceSet((PersonImpl) person),
	// probs,
	// dProb_dParameters,
	// mnl
	// .get_d2P_dbdb(
	// delta,
	// Arrays
	// .asList(
	// 0/* traveling */,
	// 2/* performing */),
	// false)/* d2ChoiceProb_dParam2 */);
	// // ***************************************************
	// }
	// } else {// ***********iter<=maxPlanPerAgent+1************
	// if (this.iter - this.firstIter == 1) {
	// /*
	// * shuffle the Plan Choice Set, to avoid chaotic network
	// * situation
	// */
	// Collections.shuffle(person.getPlans(), MatsimRandom
	// .getRandom()/*
	// * Random-Objekt is not generated by
	// * every calling of shuffle(List)
	// */);
	// }
	// // ENSURE THAT EVERY PLANS IN CHOICE SET WILL BE
	// // SIMULATED ONE TIME
	// ((PersonImpl) person).setSelectedPlan(plans.get(iter
	// % maxPlansPerAgent));
	// // ****************************************************
	// }
	// } else
	// // strategy==null
	// Gbl.errorMsg("No strategy found!");
	// }
	// // output stats and variabilities
	// this.statistics = new double[] { travelingCarStats.getAvg(),
	// travelingCarStats.getVar(), performingStats.getAvg(),
	// performingStats.getVar() };
	// System.out.println("Statistics\t" + this.statistics[0]/* travCarAvg */
	// + "\t" + this.statistics[1]/* travCarVar */+ "\t"
	// + this.statistics[2]/* perfAttrAvg */+ "\t"
	// + this.statistics[3]/* perfAttrVar */);
	//
	// // finally make sure all strategies have finished there work
	// for (PlanStrategy strategy : getStrategies())
	// strategy.finish();
	// }

	@Override
	protected void afterRunHook(Population population) {
		super.afterRunHook(population);
		// output stats and variabilities
		statistics = new double[] { travelingCarStats.getAvg(),
				travelingCarStats.getVar(), performingStats.getAvg(),
				performingStats.getVar() };
		System.out.println("Statistics\t" + statistics[0]/* travCarAvg */
				+ "\t" + statistics[1]/* travCarVar */+ "\t" + statistics[2]/* perfAttrAvg */
				+ "\t" + statistics[3]/* perfAttrVar */);
	}

	private void correctPersonPlansScores(Person person) {
		for (Plan plan : person.getPlans()) {
			correctPlanScore(plan);
		}
	}

	private void correctPlanScore(Plan plan) {
		planConverter.convert((PlanImpl) plan);
		cadyts.demand.Plan<Link> planSteps = planConverter.getPlanSteps();
		double scoreCorrection = ((MATSimChoiceParameterCalibrator<Link>) calibrator)
				.getUtilityCorrection(planSteps)
				/ brainExpBeta;
		Double oldScore = plan.getScore();
		if (oldScore == null) {
			oldScore = 0d;// dummy setting, the score of plans will be
							// calculated between firstIter+1 and firstIter+
		}
		plan.setScore(oldScore + scoreCorrection);
	}
}

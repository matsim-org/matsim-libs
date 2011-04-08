/* *********************************************************************** *
 * project: org.matsim.*
 * PCCtl2.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalFrejingerScoring;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.StringUtils;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.paramCorrection.BseParamCalibrationControlerListener;

/**
 * "traveling", "travelingPt", "travelingWalk","performing", "constantCar",
 * "constantPt", "constantWalk", "monetaryDistanceCostRateCar",
 * "monetaryDistanceCostRatePt", "marginalUtlOfDistanceWalk"can be calibrated.
 * 
 * @author yu
 * 
 */
public class PCCtl2 extends BseParamCalibrationControler2 {

	@Deprecated
	public PCCtl2(final String[] args) {
		super(args);
		// Config config;
		// config = ConfigUtils.loadConfig(args[0]);
		extension = new PCCtlListener2();
		addControlerListener(extension);
	}

	public PCCtl2(Config config) {
		super(config);
		extension = new PCCtlListener2();
		addControlerListener(extension);
	}

	/**
	 * please check the method in super class, when the super class {@code
	 * org.matsim.core.controler.Controler} is changed sometimes
	 */
	@Override
	protected void loadCoreListeners() {
		addCoreControlerListener(new CoreControlerListener());

		// ******DEACTIVATE SCORING & ROADPRICING IN MATSIM******
		// the default handling of plans
		plansScoring4PC = new PlansScoring4PC_mnl2();
		addCoreControlerListener(plansScoring4PC);

		// load road pricing, if requested
		// if (this.config.roadpricing().getTollLinksFile() != null) {
		// this.areaToll = new RoadPricing();
		// this.addCoreControlerListener(areaToll);
		// }
		// ******************************************************

		addCoreControlerListener(new PlansReplanning());
		addCoreControlerListener(new PlansDumping());
		// EventsHanding ... very important
		addCoreControlerListener(new EventsHandling(events));
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new PCStrMn2(
				network,
				getFirstIteration(),
				config.planCalcScore().getBrainExpBeta(),
				Integer
						.parseInt(config
								.findParam(
										BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
										"parameterDimension"/*
															 * 2, traveling ,
															 * performing
															 */)));
		StrategyManagerConfigLoader.load(this, manager);

		// deactivate generating of new Plans by plan innovation
		String disablePlanGeneratingAfterIterStr = config.findParam("bse",
				"disablePlanGeneratingAfterIter");
		int disablePlanGeneratingAfterIter;
		if (disablePlanGeneratingAfterIterStr == null) {
			disablePlanGeneratingAfterIter = getLastIteration() + 1;
		} else {
			disablePlanGeneratingAfterIter = Integer
					.parseInt(disablePlanGeneratingAfterIterStr);
		}

		String[] modules = StringUtils.explode(config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"strategyModules"), ',');
		String[] moduleProbs = StringUtils.explode(config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"strategyModuleProbabilities"), ',');

		if (modules.length != moduleProbs.length) {
			throw new RuntimeException(
					"Length of Parameter :\tstrategyModules and Parameter :\tstrategyModuleProbabilities should be the same.");
		}

		for (int i = 0; i < modules.length; i++) {
			String module = modules[i].trim();
			double prob = Double.parseDouble(moduleProbs[i].trim());

			if (module.equals("ChangeExpBeta")) {
				// ChangeExpBeta
				PlanStrategy changeExpBeta = new PlanStrategyImpl(
						new ExpBetaPlanChanger(config.planCalcScore()
								.getBrainExpBeta()));
				manager.addStrategy(changeExpBeta, 0.0);
				manager.addChangeRequest(getFirstIteration()
						+ manager.getMaxPlansPerAgent() + 1/* 505 */,
						changeExpBeta, prob);
			} else if (module.equals("SelectExpBeta")) {
				// SelectExpBeta
				PlanStrategy selectExpBeta = new PlanStrategyImpl(
						new ExpBetaPlanSelector(config.planCalcScore()));
				manager.addStrategy(selectExpBeta, 0.0);
				manager.addChangeRequest(getFirstIteration()
						+ manager.getMaxPlansPerAgent() + 1/* 505 */,
						selectExpBeta, prob);

			} else if (module.equals("ReRoute")) {
				// ReRoute
				PlanStrategy reRoute = new PlanStrategyImpl(
						new RandomPlanSelector());
				reRoute.addStrategyModule(new ReRoute(this));
				manager.addStrategy(reRoute, 0.0);
				manager.addChangeRequest(getFirstIteration()
						+ manager.getMaxPlansPerAgent() + 1, reRoute, prob);
				manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
						reRoute, 0);
			} else if (module.equals("TimeAllocationMutator")) {
				// TimeAllocationMutator
				PlanStrategy timeAllocationMutator = new PlanStrategyImpl(
						new RandomPlanSelector());
				timeAllocationMutator
						.addStrategyModule(new TimeAllocationMutator(config));
				manager.addStrategy(timeAllocationMutator, 0.0);
				manager.addChangeRequest(getFirstIteration()
						+ manager.getMaxPlansPerAgent() + 1,
						timeAllocationMutator, prob);
				manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
						timeAllocationMutator, 0);
			}
		}

		return manager;
	}

	/** @param args */
	public static void main(final String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		Controler ctl = new PCCtl2(config);
		ctl.setCreateGraphs(false);
		ctl.setOverwriteFiles(true);
		ctl.run();
	}
}

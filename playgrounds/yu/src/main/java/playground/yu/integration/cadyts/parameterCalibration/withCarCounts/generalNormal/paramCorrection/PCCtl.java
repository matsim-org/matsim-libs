///* *********************************************************************** *
// * project: org.matsim.*
// * PCCtl.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.paramCorrection;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.corelisteners.EventsHandling;
//import org.matsim.core.controler.corelisteners.PlansDumping;
//import org.matsim.core.controler.corelisteners.PlansReplanning;
//import org.matsim.core.replanning.StrategyManager;
//import org.matsim.core.replanning.StrategyManagerConfigLoader;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.scoring.PlansScoring4PC;
//import playground.yu.scoring.PlansScoringI;
//import playground.yu.scoring.withAttrRecorder.CharyparNagelScoringFunctionFactory4AttrRecorder;
//import playground.yu.scoring.withAttrRecorder.ControlerWithAttrRecorderI;
//
///**
// * "traveling", "travelingPt", "travelingWalk","performing", "constantCar",
// * "constantPt", "constantWalk", "monetaryDistanceCostRateCar",
// * "monetaryDistanceCostRatePt", "marginalUtlOfDistanceWalk"can be calibrated.
// * 
// * @author yu
// * 
// */
//public class PCCtl extends BseParamCalibrationControler implements
//		ControlerWithAttrRecorderI {
//
//	public PCCtl(Config config) {
//		super(config);
//		extension = new PCCtlListener();
//		addControlerListener(extension);
//	}
//
//	@Deprecated
//	public PCCtl(final String[] args) {
//		super(args);
//		// Config config;
//		// config = ConfigUtils.loadConfig(args[0]);
//		extension = new PCCtlListener();
//		addControlerListener(extension);
//	}
//
//	/**
//	 * please check the method in super class, when the super class
//	 * {@code org.matsim.core.controler.Controler} is changed sometimes
//	 */
//	@Override
//	protected void loadCoreListeners() {
//
//		// ******DEACTIVATE SCORING & ROADPRICING IN MATSIM******
//		// the default handling of plans
//		plansScoring4PC = new PlansScoring4PC();
//		addCoreControlerListener(plansScoring4PC);
//
//		// load road pricing, if requested
//		// if (this.config.roadpricing().getTollLinksFile() != null) {
//		// this.areaToll = new RoadPricing();
//		// this.addCoreControlerListener(areaToll);
//		// }
//		// ******************************************************
//
//		addCoreControlerListener(new PlansReplanning());
//		addCoreControlerListener(new PlansDumping());
//		// EventsHanding ... very important
//		addCoreControlerListener(new EventsHandling(events));
//	}
//
//	@Override
//	protected ScoringFunctionFactory loadScoringFunctionFactory() {
//		return new CharyparNagelScoringFunctionFactory4AttrRecorder(
//				config.planCalcScore(), network);
//	}
//
//	@Override
//	protected StrategyManager loadStrategyManager() {
//		StrategyManager manager = new PCStrMn(network, getFirstIteration(),
//				config);
//		StrategyManagerConfigLoader.load(this, manager);
//		//
//		// // deactivate generating of new Plans by plan innovation
//		// String disablePlanGeneratingAfterIterStr = config.findParam("bse",
//		// "disablePlanGeneratingAfterIter");
//		// int disablePlanGeneratingAfterIter;
//		// if (disablePlanGeneratingAfterIterStr == null) {
//		// disablePlanGeneratingAfterIter = getLastIteration() + 1;
//		// } else {
//		// disablePlanGeneratingAfterIter = Integer
//		// .parseInt(disablePlanGeneratingAfterIterStr);
//		// }
//		//
//		// String[] modules = StringUtils.explode(config.findParam(
//		// CalibrationConfig.BSE_CONFIG_MODULE_NAME, "strategyModules"),
//		// ',');
//		// String[] moduleProbs = StringUtils.explode(config.findParam(
//		// CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//		// "strategyModuleProbabilities"), ',');
//		//
//		// if (modules.length != moduleProbs.length) {
//		// throw new RuntimeException(
//		// "Length of Parameter :\tstrategyModules and Parameter :\tstrategyModuleProbabilities should be the same.");
//		// }
//		//
//		// for (int i = 0; i < modules.length; i++) {
//		// String module = modules[i].trim();
//		// double prob = Double.parseDouble(moduleProbs[i].trim());
//		//
//		// if (module.equals("ChangeExpBeta")) {
//		// // ChangeExpBeta
//		// PlanStrategy changeExpBeta = new PlanStrategyImpl(
//		// new ExpBetaPlanChanger(config.planCalcScore()
//		// .getBrainExpBeta()));
//		// manager.addStrategy(changeExpBeta, 0.0);
//		// manager.addChangeRequest(
//		// getFirstIteration() + manager.getMaxPlansPerAgent() + 1/* 505 */,
//		// changeExpBeta, prob);
//		// } else if (module.equals("SelectExpBeta")) {
//		// // SelectExpBeta
//		// PlanStrategy selectExpBeta = new PlanStrategyImpl(
//		// new ExpBetaPlanSelector(config.planCalcScore()));
//		// manager.addStrategy(selectExpBeta, 0.0);
//		// manager.addChangeRequest(
//		// getFirstIteration() + manager.getMaxPlansPerAgent() + 1/* 505 */,
//		// selectExpBeta, prob);
//		//
//		// } else if (module.equals("ReRoute")) {
//		// // ReRoute
//		// PlanStrategy reRoute = new PlanStrategyImpl(
//		// new RandomPlanSelector());
//		// reRoute.addStrategyModule(new ReRoute(this));
//		// manager.addStrategy(reRoute, 0.0);
//		// manager.addChangeRequest(
//		// getFirstIteration() + manager.getMaxPlansPerAgent() + 1,
//		// reRoute, prob);
//		// manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
//		// reRoute, 0);
//		// } else if (module.equals("TimeAllocationMutator")) {
//		// // TimeAllocationMutator
//		// PlanStrategy timeAllocationMutator = new PlanStrategyImpl(
//		// new RandomPlanSelector());
//		// timeAllocationMutator
//		// .addStrategyModule(new TimeAllocationMutator(config));
//		// manager.addStrategy(timeAllocationMutator, 0.0);
//		// manager.addChangeRequest(
//		// getFirstIteration() + manager.getMaxPlansPerAgent() + 1,
//		// timeAllocationMutator, prob);
//		// manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
//		// timeAllocationMutator, 0);
//		// } else if (module.equals("ChangeSingleLegMode")) {
//		// // TimeAllocationMutator
//		// PlanStrategy changeSingleLegMode = new PlanStrategyImpl(
//		// new RandomPlanSelector());
//		// changeSingleLegMode.addStrategyModule(new ChangeSingleLegMode(
//		// config));
//		// changeSingleLegMode.addStrategyModule(new ReRoute(this));
//		//
//		// manager.addStrategy(changeSingleLegMode, 0.0);
//		// manager.addChangeRequest(
//		// getFirstIteration() + manager.getMaxPlansPerAgent() + 1,
//		// changeSingleLegMode, prob);
//		// manager.addChangeRequest(disablePlanGeneratingAfterIter + 1,
//		// changeSingleLegMode, 0);
//		// }
//		// }
//
//		return manager;
//	}
//
//	@Override
//	public PlansScoringI getPlansScoring4AttrRecorder() {
//		return plansScoring4PC;
//	}
//}

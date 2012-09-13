///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.corelisteners.EventsHandling;
//import org.matsim.core.controler.corelisteners.PlansDumping;
//import org.matsim.core.controler.corelisteners.PlansReplanning;
//import org.matsim.core.replanning.StrategyManager;
//import org.matsim.core.replanning.StrategyManagerConfigLoader;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testLeftTurn.PlansScoringWithLeftTurnPenalty4PC;
//import playground.yu.scoring.PlansScoringI;
//import playground.yu.scoring.withAttrRecorder.ControlerWithAttrRecorderI;
//import playground.yu.scoring.withAttrRecorder.ScorAttrWriteTrigger;
//import playground.yu.scoring.withAttrRecorder.leftTurn.CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty;
//
///**
// * @author yu
// * 
// */
//public class PCCtlwithLeftTurnPenalty extends BseParamCalibrationControler
//		implements ControlerWithAttrRecorderI {
//
//	public PCCtlwithLeftTurnPenalty(Config config) {
//		super(config);
//		extension = new PCCtlListener();
//		addControlerListener(extension);
//		String writeScorAttrIntervalStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//				"writeScorAttrInterval");
//		if (writeScorAttrIntervalStr != null) {
//			addControlerListener(new ScorAttrWriteTrigger());
//		}
//	}
//
//	@Override
//	public PlansScoringI getPlansScoring4AttrRecorder() {
//		return getPlansScoring4PC();
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
//		plansScoring4PC = new PlansScoringWithLeftTurnPenalty4PC();
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
//		return new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
//				config, network);
//	}
//
//	@Override
//	protected StrategyManager loadStrategyManager() {
//		StrategyManager manager = new PCStrMn(network, getFirstIteration(),
//				config);
//		StrategyManagerConfigLoader.load(this, manager);
//
//		return manager;
//	}
//
////	@Override
////	public void shutdown(boolean unexpected) {
////		super.shutdown(unexpected);
////	}
//	// can't override this any more.  not overriding it will call the super method, which is fine.  kai, jun'12
//
//}

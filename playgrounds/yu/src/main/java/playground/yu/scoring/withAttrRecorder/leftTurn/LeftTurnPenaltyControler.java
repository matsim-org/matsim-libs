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
//package playground.yu.scoring.withAttrRecorder.leftTurn;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.corelisteners.EventsHandling;
//import org.matsim.core.controler.corelisteners.PlansDumping;
//import org.matsim.core.controler.corelisteners.PlansReplanning;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//
//import playground.yu.scoring.PlansScoringI;
//import playground.yu.scoring.withAttrRecorder.Controler4AttrRecorder;
//
///**
// * @author yu
// *
// */
//public class LeftTurnPenaltyControler extends Controler4AttrRecorder {
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		Config config;
//		if (args.length < 1) {
//			config = ConfigUtils
//					.loadConfig("test/input/2car1ptRoutes/writeScorAttrs/cfgTrav-4.44cLT-1.01.xml");
//		} else/* args.length>=1 */{
//			config = ConfigUtils.loadConfig(args[0]);
//		}
//		LeftTurnPenaltyControler controler = new LeftTurnPenaltyControler(
//				config);
//		controler.setOverwriteFiles(true);
//		controler.setCreateGraphs(false);
//		controler.run();
//	}
//
//	private PlansScoringWithLeftTurnPenalty plansScoringLTP = null;
//
//	public LeftTurnPenaltyControler(Config config) {
//		super(config);
//	}
//
//	@Override
//	public PlansScoringI getPlansScoring4AttrRecorder() {
//		return plansScoringLTP;
//	}
//
//	@Override
//	protected void loadCoreListeners() {
//
//		// ------DEACTIVATE SCORING & ROADPRICING IN MATSIM------
//		plansScoringLTP = new PlansScoringWithLeftTurnPenalty();
//		addCoreControlerListener(plansScoringLTP);
//		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//		// load road pricing, if requested
//		// if (this.config.roadpricing().getTollLinksFile() != null) {
//		// this.areaToll = new RoadPricing();
//		// this.addCoreControlerListener(areaToll);
//		// }
//		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//		addCoreControlerListener(new PlansReplanning());
//		addCoreControlerListener(new PlansDumping());
//		// EventsHandling ... very important
//		addCoreControlerListener(new EventsHandling(events));
//	}
//
//	@Override
//	protected ScoringFunctionFactory loadScoringFunctionFactory() {
//		// ---------------------------------------------------
//		return new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
//				config, network);
//		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	}
//}

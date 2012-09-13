///* *********************************************************************** *
// * project: org.matsim.*
// * Controler4AttrRecorder.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
//package playground.yu.scoring.withAttrRecorder;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.corelisteners.EventsHandling;
//import org.matsim.core.controler.corelisteners.PlansDumping;
//import org.matsim.core.controler.corelisteners.PlansReplanning;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//
//import playground.yu.scoring.PlansScoringI;
//import playground.yu.scoring.withAttrRecorder.ScorAttrReader.ScorAttrReadListener;
//
///**
// * @author yu
// * 
// */
//public class Controler4AttrRecorder extends Controler implements
//		ControlerWithAttrRecorderI {
//	public static void main(String[] args) {
//		Config config;
//		if (args.length < 1) {
//			config = ConfigUtils
//					.loadConfig("test/input/2car1ptRoutes/writeScorAttrs/cfgCar-4_0.xml");
//		} else/* args.length>=1 */{
//			config = ConfigUtils.loadConfig(args[0]);
//		}
//		Controler4AttrRecorder controler = new Controler4AttrRecorder(config);
//		controler.setOverwriteFiles(true);
//		controler.setCreateGraphs(false);
//		controler.run();
//	}
//
//	private PlansScoring4AttrRecorder planScoring4AttrRecorder = null;
//
//	public Controler4AttrRecorder(Config config) {
//		super(config);
//		// ---------------------------------------------------
//		addControlerListener(new ScorAttrWriteTrigger());
//		addControlerListener(new ScorAttrReadListener());
//		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	}
//
//	@Override
//	public PlansScoringI getPlansScoring4AttrRecorder() {
//		return planScoring4AttrRecorder;
//	}
//
//	@Override
//	protected void loadCoreListeners() {
//
//		// ------DEACTIVATE SCORING & ROADPRICING IN MATSIM------
//		planScoring4AttrRecorder = new PlansScoring4AttrRecorder();
//		addCoreControlerListener(planScoring4AttrRecorder);
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
//		// EventsHanding ... very important
//		addCoreControlerListener(new EventsHandling(events));
//	}
//
//	@Override
//	protected ScoringFunctionFactory loadScoringFunctionFactory() {
//		// ---------------------------------------------------
//		return new CharyparNagelScoringFunctionFactory4AttrRecorder(
//				config.planCalcScore(), network);
//		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	}
//}

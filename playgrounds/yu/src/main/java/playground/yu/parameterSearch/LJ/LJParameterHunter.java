///* *********************************************************************** *
// * project: org.matsim.*
// * LJParameterHunter.java
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
//
//package playground.yu.parameterSearch.LJ;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//
//import playground.yu.scoring.withAttrRecorder.leftTurn.LeftTurnPenaltyControler;
//import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;
//
//public class LJParameterHunter extends LeftTurnPenaltyControler {
//	private final SimCntLogLikelihoodCtlListener llhListener;
//
//	public LJParameterHunter(Config cfg) {
//		super(cfg);
//		llhListener = new SimCntLogLikelihoodCtlListener();
//		addControlerListener(llhListener);
//		addControlerListener(new LJParaemterSearchListener());
//		setOverwriteFiles(true);
//		setCreateGraphs(false);
//		run();
//	}
//
//	public SimCntLogLikelihoodCtlListener getLlhListener() {
//		return llhListener;
//	}
//
//	public static void main(String[] args) {
//		new LJParameterHunter(
//				ConfigUtils.loadConfig(args[0]/* configfilename */));
//	}
//}

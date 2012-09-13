///* *********************************************************************** *
// * project: org.matsim.*
// * AdditionalParameterSetter.java
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
///**
// * 
// */
//package playground.yu.scoring.withAttrRecorder.leftTurn.scoringTests;
//
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.StartupListener;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.parameterSearch.ParametersSetter;
//import playground.yu.scoring.withAttrRecorder.leftTurn.LeftTurnPenaltyControler;
//
///**
// * tests the effect of setScoringFunctionFactory in {@code Controler}
// * 
// * @author yu
// * 
// */
//public class AdditionalParameterSetter implements StartupListener {
//	@Override
//	public void notifyStartup(StartupEvent event) {
//		Controler ctl = event.getControler();
//		Map<String, Double> nameParams = new TreeMap<String, Double>();
//		double constLT = Double.parseDouble(ctl.getConfig().findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//				CalibrationConfig.CONSTANT_LEFT_TURN));
//		nameParams.put(CalibrationConfig.CONSTANT_LEFT_TURN, constLT);
//		ParametersSetter.setParameters(ctl, nameParams);
//	}
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		Config cfg = ConfigUtils.loadConfig(args[0]);
//		LeftTurnPenaltyControler ctl = new LeftTurnPenaltyControler(cfg);
//		ctl.addControlerListener(new AdditionalParameterSetter());
//		ctl.setCreateGraphs(false);
//		ctl.setOverwriteFiles(true);
//		ctl.run();
//	}
//
//}

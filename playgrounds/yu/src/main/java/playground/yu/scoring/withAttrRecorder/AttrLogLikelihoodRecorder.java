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
//
///**
// * 
// */
//package playground.yu.scoring.withAttrRecorder;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//
//import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;
//
///**
// * records attributes corresponding to scoring parameters and the log-likelihood
// * between simulated traffic and counts during a MATSim simulation
// * 
// * @author xfytb
// * 
// */
//public class AttrLogLikelihoodRecorder {
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		Config config;
//		if (args.length < 1) {
//			config = ConfigUtils
//					.loadConfig("test/input/2car1ptRoutes/writeScorAttrs/cfgCar-4_0.xml");
//		} else/* args.length>=1 */{
//			config = ConfigUtils.loadConfig(args[0]);
//		}
//		Controler4AttrRecorder controler = new Controler4AttrRecorder(config);
//		controler.addControlerListener(new SimCntLogLikelihoodCtlListener());
//		controler.setOverwriteFiles(true);
//		controler.setCreateGraphs(false);
//		controler.run();
//	}
//
//}

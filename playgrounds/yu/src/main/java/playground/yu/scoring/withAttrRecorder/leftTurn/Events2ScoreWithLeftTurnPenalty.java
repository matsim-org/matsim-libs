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
//import java.util.Map;
//
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.core.config.Config;
//import org.matsim.core.scoring.ScoringFunction;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//import org.matsim.core.utils.collections.Tuple;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.scoring.withAttrRecorder.Events2Score4AttrRecorder;
//
///**
// * @author yu
// * 
// */
//public class Events2ScoreWithLeftTurnPenalty extends Events2Score4AttrRecorder {
//
//	public Events2ScoreWithLeftTurnPenalty(Config config,
//			ScoringFunctionFactory sfFactory, Scenario scenario) {
//		super(config, sfFactory, scenario);
//		attrNameList.add(CalibrationConfig.CONSTANT_LEFT_TURN);
//	}
//
//	@Override
//	public void finish() {
//		// very important!!!
//		super.finish();
//		// very important!!!
//		for (Tuple<Plan, ScoringFunction> plansScorFunction : agentScorers
//				.values()) {
//
//			Plan plan = plansScorFunction.getFirst();
//			Map<String, Object> attrs = plan.getCustomAttributes();
//
//			ScoringFunction sf = plansScorFunction.getSecond();
//			// sf.finish();//TEST here at first do NOT do this
//			// **********************codes from {@code EventsToScore}
//			// save attributes as custom attributes.
//			// #########################################
//			if (sf instanceof ScoringFunctionAccumulatorWithLeftTurnPenalty) {
//				// leftTurn
//				attrs.put(CalibrationConfig.CONSTANT_LEFT_TURN,
//						((ScoringFunctionAccumulatorWithLeftTurnPenalty) sf)
//								.getNbOfLeftTurnAttrCar());
//			}
//		}
//	}
//}

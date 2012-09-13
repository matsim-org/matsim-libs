///* *********************************************************************** *
// * project: org.matsim.*
// * ParameterCalibrationTest.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;
//
//import org.junit.Rule;
//import org.junit.Test;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.IterationStartsEvent;
//import org.matsim.core.controler.listener.IterationStartsListener;
//import org.matsim.testcases.MatsimTestCase;
//import org.matsim.testcases.MatsimTestUtils;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//
///**
// * @author yu
// *
// */
//public class TravelingConstLeftTurnTest extends MatsimTestCase {
//	@Rule
//	MatsimTestUtils utils = new MatsimTestUtils(); // maybe not necessary in
//													// this class
//
//	private class GetCalibratedParameters implements IterationStartsListener {
//
//		@Override
//		public void notifyIterationStarts(IterationStartsEvent event) {
//			Config config = event.getControler().getConfig();
//			int iteration = event.getIteration();
//			int baseIter = config.strategy().getMaxAgentPlanMemorySize()
//					+ config.controler().getFirstIteration();
//			if (iteration > baseIter) {
//				PlanCalcScoreConfigGroup planCalcScoreCG = config
//						.planCalcScore();
//				double traveling = planCalcScoreCG.getTraveling_utils_hr();
//				//
//				String constLeftTurnStr = config.findParam(
//						CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//						CalibrationConfig.CONSTANT_LEFT_TURN);
//				double constantLeftTurn = 0;
//				if (constLeftTurnStr != null) {
//					constantLeftTurn = Double.parseDouble(constLeftTurnStr);
//				}
//
//				// compares traveling
//				double expectedTraveling = -3;// not really the expected
//												// traveling during the
//												// parameter calibration, but
//												// the expected value for the
//												// junit test
//
//				switch (iteration - baseIter) {
//				case 1:
//					expectedTraveling = -6;
//					break;
//				case 2:
//					expectedTraveling = -6;
//					break;
//				case 3:
//					expectedTraveling = -6.224864010329179;
//					break;
//				case 4:
//					expectedTraveling = -4.910974516956966;
//					break;
//				case 5:
//					expectedTraveling = -3.7406282014957446;
//					break;
//				case 6:
//					expectedTraveling = -2.4271613971127577;
//					break;
//				case 7:
//					expectedTraveling = -1.712279517913751;
//					break;
//				case 8:
//					expectedTraveling = -1.220208954146627;
//					break;
//				case 9:
//					expectedTraveling = -0.931647757513297;
//					break;
//				case 10:
//					expectedTraveling = -0.9158018072990199;
//					break;
//				}
//
//				assertEquals(
//						"The newly calibrated\t\"traveling\"\tdoes NOT correspond with the expected value by Iteration\t"
//								+ iteration, expectedTraveling, traveling,
//						MatsimTestUtils.EPSILON);
//
//				// compares constantPt
//				double expectedConstantLeftTurn = 0;
//				switch (iteration - baseIter) {
//				case 1:
//					expectedConstantLeftTurn = 0;
//					break;
//				case 2:
//					expectedConstantLeftTurn = 0;
//					break;
//				case 3:
//					expectedConstantLeftTurn = -1.7154468311127662;
//					break;
//				case 4:
//					expectedConstantLeftTurn =-3.287301770649159;
//					break;
//				case 5:
//					expectedConstantLeftTurn = -4.5138497659059835;
//					break;
//				case 6:
//					expectedConstantLeftTurn = -5.467464963420053;
//					break;
//				case 7:
//					expectedConstantLeftTurn = -6.069118477846682;
//					break;
//				case 8:
//					expectedConstantLeftTurn = -6.519746879845867;
//					break;
//				case 9:
//					expectedConstantLeftTurn = -6.772647538223071;
//					break;
//				case 10:
//					expectedConstantLeftTurn = -6.821687128260576;
//					break;
//				}
//
//				assertEquals(
//						"The newly calibrated\t\"constantLeftTurn\"\tdoes NOT correspond with the expected value by Iteration\t"
//								+ iteration, expectedConstantLeftTurn,
//						constantLeftTurn, MatsimTestUtils.EPSILON);
//			}
//		}
//	}
//
//	/**
//	 * tests the parameter calibration with pop [-6, 0] counts [-5, -0.5]
//	 */
//	@Test
//	public final void test2ParametersCalibration() {
//		Config config = super.loadConfig(getInputDirectory() + "config.xml");
//		Controler controler = new PCCtlwithLeftTurnPenalty(config);
//		controler.addControlerListener(new GetCalibratedParameters());
//		controler.setCreateGraphs(false);
//		controler.setOverwriteFiles(true);
//		controler.run();
//	}
//}

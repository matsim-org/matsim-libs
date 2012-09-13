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
//package playground.yu.parameterCalibration;
//
//import org.junit.Rule;
//import org.junit.Test;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//import org.matsim.core.controler.events.IterationStartsEvent;
//import org.matsim.core.controler.listener.IterationStartsListener;
//import org.matsim.testcases.MatsimTestCase;
//import org.matsim.testcases.MatsimTestUtils;
//
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.paramCorrection.PCCtl;
//
///**
// * @author yu
// * 
// */
//public class TravelingPtConstantPtTest extends MatsimTestCase {
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
//				double travelingPt = planCalcScoreCG.getTravelingPt_utils_hr();
//				double constantPt = planCalcScoreCG.getConstantPt();
//
//				// compares travelingPt
//				double expectedTravelingPt = -3;// not really the expected
//												// travelingPt during the
//												// parameter calibration, but
//												// the expected value for the
//												// junit test
//
//				switch (iteration - baseIter) {
//				case 1:
//					expectedTravelingPt = -3;
//					break;
//				case 2:
//					expectedTravelingPt = -3;
//					break;
//				case 3:
//					expectedTravelingPt = -3;
//					break;
//				case 4:
//					expectedTravelingPt = -3;
//					break;
//				case 5:
//					expectedTravelingPt = -3;
//					break;
//				case 6:
//					expectedTravelingPt = -3;
//					break;
//				case 7:
//					expectedTravelingPt = -3.2099265811888458;
//					break;
//				case 8:
//					expectedTravelingPt = -3.418639770640792;
//					break;
//				case 9:
//					expectedTravelingPt = -3.42151537438168;
//					break;
//				case 10:
//					expectedTravelingPt = -3.4159580649563894;
//					break;
//				}
//
//				assertEquals(
//						"The newly calibrated travelingPt does NOT correspond with the expected value by Iteration\t"
//								+ iteration, expectedTravelingPt, travelingPt,
//						MatsimTestUtils.EPSILON);
//
//				// compares constantPt
//				double expectedConstantPt = 0;
//				switch (iteration - baseIter) {
//				case 1:
//					expectedConstantPt = 0;
//					break;
//				case 2:
//					expectedConstantPt = 0;
//					break;
//				case 3:
//					expectedConstantPt = 0;
//					break;
//				case 4:
//					expectedConstantPt = 0;
//					break;
//				case 5:
//					expectedConstantPt = 0;
//					break;
//				case 6:
//					expectedConstantPt = 0;
//					break;
//				case 7:
//					expectedConstantPt = -0.8134058352940957;
//					break;
//				case 8:
//					expectedConstantPt = -0.6977157242609757;
//					break;
//				case 9:
//					expectedConstantPt = -0.42361532555929454;
//					break;
//				case 10:
//					expectedConstantPt = 0.0904433343888438;
//					break;
//				}
//
//				assertEquals(
//						"The newly calibrated constantPt does NOT correspond with the expected value by Iteration\t"
//								+ iteration, expectedConstantPt, constantPt,
//						MatsimTestUtils.EPSILON);
//			}
//		}
//	}
//
//	@Test
//	public final void test2ParametersCalibration() {
//		Config config = super.loadConfig(getInputDirectory() + "config.xml");
//		PCCtl controler = new PCCtl(config);
//		controler.addControlerListener(new GetCalibratedParameters());
//		controler.setCreateGraphs(false);
//		controler.setOverwriteFiles(true);
//		controler.run();
//	}
//}

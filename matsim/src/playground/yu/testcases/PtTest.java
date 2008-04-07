/* *********************************************************************** *
 * project: org.matsim.*
 * PtRest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.yu.testcases;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.analysis.PtCheck;

/**
 * @author ychen
 * 
 */
public class PtTest extends MatsimTestCase {
	private static class TestControler extends Controler {
		public TestControler(String configFileName) {
			super(configFileName);
		}
	}

	private static class TestControlerListener implements IterationEndsListener {
		private PtCheck pc;

		public TestControlerListener() {
			pc = new PtCheck();
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			double betaPt = Double.parseDouble(Gbl.getConfig().getParam(
					"planCalcScore", "travelingPt"));
			int idx = event.getIteration();
			if (idx % 10 == 0) {
				pc.resetCnt();
				pc.run(event.getControler().getPopulation());
				if (betaPt == -6) {
					System.out
							.println("checking results for case `beta_travel = -6'...");
					int criterion = 0;
					switch (idx) {
					case 0:
						criterion = 100;
						break;
					case 10:
						criterion = 38;
						break;
					case 20:
						criterion = 43;
						break;
					case 30:
						criterion = 40;
						break;
					case 40:
						criterion = 41;
						break;
					case 50:
						criterion = 30;
						break;
					}
					assertEquals(criterion, pc.getPtUserCnt());
				}
				if (betaPt == -3) {
					System.out
							.println("checking results for case `beta_travel = -3'...");
					int criterion = 0;
					switch (idx) {
					case 0:
						criterion = 100;
						break;
					case 10:
						criterion = 99;
						break;
					case 20:
						criterion = 98;
						break;
					case 30:
						criterion = 98;
						break;
					case 40:
						criterion = 98;
						break;
					case 50:
						criterion = 98;
						break;
					}
					assertEquals(criterion, pc.getPtUserCnt());
				}
			}
		}
	}

	/**
	 * 
	 */
	public void testbetaPt_6() {
		TestControler controler = new TestControler(
				"test/yu/testCases/testPt/config-6.xml");
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.run();
	}

	public void testbetaPt_3() {
		TestControler controler = new TestControler(
				"test/yu/testCases/testPt/config-3.xml");
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.run();
	}
}

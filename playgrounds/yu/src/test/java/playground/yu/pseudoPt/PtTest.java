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
package playground.yu.pseudoPt;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.analysis.PtCheck;

/**
 * This TestCase should ensure the correct behavior of agents when different
 * values for beta_travelingPt-parameters are used. The agents drive on the
 * equil-net, but can neither do departure-time adaptation, nor rerouting. This
 * leads to all agents driving through the same one route in the network. The
 * agents can only choose plans with type "car" or "pt".
 *
 * @author ychen
 *
 */
public class PtTest extends MatsimTestCase {

	/**
	 * Responsible for the verification of the tests. It adds a
	 * AbstractPersonAlgorithm (PtCheck) and checks their result (number of
	 * ptUsers) in some specific iterations.
	 *
	 * @author ychen
	 */
	private static class TestControlerListener implements IterationEndsListener {
		private final PtCheck pc;

		public TestControlerListener() {
			pc = new PtCheck();
		}

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			double betaPt = event.getControler().getConfig().charyparNagelScoring().getTravelingPt_utils_hr();
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				pc.resetCnt();
				pc.run(event.getControler().getPopulation());
				// AUTOMATIC VERIFICATION OF THE TESTS
				if (betaPt == -6) {
					System.out
							.println("checking results for case `beta_travel = -6'...");
					int criterion = 0;
					switch (iteration) {
					case 0:
						criterion = 0;
						break;
					case 10:
						criterion = 53;
						break;
					case 20:
						criterion = 43;
						break;
					case 30:
						criterion = 33;
						break;
					case 40:
						criterion = 29;
						break;
					case 50:
						criterion = 27;
						break;
					}
					assertEquals(criterion, pc.getPtUserCnt());
				} else if (betaPt == -3) {
					System.out
							.println("checking results for case `beta_travel = -3'...");
					int criterion = 0;
					switch (iteration) {
					case 0:
						criterion = 0;
						break;
					case 10:
						criterion = 98;
						break;
					case 20:
						criterion = 98;
						break;
					case 30:
						criterion = 96;
						break;
					case 40:
						criterion = 93;
						break;
					case 50:
						criterion = 92;
						break;
					}
					assertEquals(criterion, pc.getPtUserCnt());
				}
			}
		}
	}

	/**
	 * Runs the test with a value of -6 for beta_travelingPt.
	 */
	public void testbetaPt_6() {
		Controler controler = new Controler(getInputDirectory() + "config.xml");
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	/**
	 * Runs the test with a value of -3 for beta_travelingPt
	 */
	public void testbetaPt_3() {
		Controler controler = new Controler(getInputDirectory() + "config.xml");
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}

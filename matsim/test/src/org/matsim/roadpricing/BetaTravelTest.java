/* *********************************************************************** *
 * project: org.matsim.*
 * BetaTravelTest.java
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

package org.matsim.roadpricing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.algorithms.LinkQueueStats;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.gbl.Gbl;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.testcases.MatsimTestCase;

public class BetaTravelTest extends MatsimTestCase {

	public BetaTravelTest() {
	}

	public void testBetaTravel_6() {
		Config config = loadConfig(getInputDirectory() + "config.xml");
		TestControler controler = new TestControler(config);
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.run();
	}

	public void testBetaTravel_66() {
		Config config = loadConfig(getInputDirectory() + "config.xml");
		TestControler controler = new TestControler(config);
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.run();
	}

	public static class LinkAnalyzer implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI {
		public final String linkId;
		public double firstCarEnter = Double.POSITIVE_INFINITY;
		public double lastCarEnter = Double.NEGATIVE_INFINITY;
		public double firstCarLeave = Double.POSITIVE_INFINITY;
		public double lastCarLeave = Double.NEGATIVE_INFINITY;
		public int maxCarsOnLink = Integer.MIN_VALUE;
		public double maxCarsOnLinkTime = Double.NEGATIVE_INFINITY;
		private int iteration = -1;

		private final ArrayList<Double> enterTimes = new ArrayList<Double>(100);
		private final ArrayList<Double> leaveTimes = new ArrayList<Double>(100);

		private static final Logger log = Logger.getLogger(TestControlerListener.class);

		public LinkAnalyzer(final String linkId) {
			this.linkId = linkId;
			reset(0);
		}

		public void reset(final int iteration) {
			this.iteration = iteration;
			this.firstCarEnter = Double.POSITIVE_INFINITY;
			this.lastCarEnter = Double.NEGATIVE_INFINITY;
			this.firstCarLeave = Double.POSITIVE_INFINITY;
			this.lastCarLeave = Double.NEGATIVE_INFINITY;
			this.maxCarsOnLink = Integer.MIN_VALUE;
			this.maxCarsOnLinkTime = Double.NEGATIVE_INFINITY;

			this.enterTimes.clear();
			this.leaveTimes.clear();
		}

		public void handleEvent(final EventLinkEnter event) {
			if (event.linkId.equals(this.linkId)) {
				this.enterTimes.add(Double.valueOf(event.time));
				if (event.time < this.firstCarEnter) this.firstCarEnter = event.time;
				if (event.time > this.lastCarEnter) this.lastCarEnter = event.time;
			}
		}

		public void handleEvent(final EventLinkLeave event) {
			if (event.linkId.equals(this.linkId)) {
				this.leaveTimes.add(Double.valueOf(event.time));
				if (event.time < this.firstCarLeave) this.firstCarLeave = event.time;
				if (event.time > this.lastCarLeave) this.lastCarLeave = event.time;
			}
		}

		public void calcMaxCars() {
			Collections.sort(this.enterTimes);
			Collections.sort(this.leaveTimes);
			int idxEnter = 0;
			int idxLeave = 0;
			int cars = 0;

			double timeLeave = this.leaveTimes.get(idxLeave).doubleValue();
			double timeEnter = this.enterTimes.get(idxEnter).doubleValue();
			double time;

			while (timeLeave != Double.POSITIVE_INFINITY && timeEnter != Double.POSITIVE_INFINITY) {
				if (timeLeave <= timeEnter) {
					time = timeLeave;
					idxLeave++;
					if (idxLeave < this.leaveTimes.size()) {
						timeLeave = this.leaveTimes.get(idxLeave).doubleValue();
					} else {
						timeLeave = Double.POSITIVE_INFINITY;
					}
					cars--;
				} else {
					time = timeEnter;
					idxEnter++;
					if (idxEnter < this.enterTimes.size()) {
						timeEnter = this.enterTimes.get(idxEnter).doubleValue();
					} else {
						timeEnter = Double.POSITIVE_INFINITY;
					}
					cars++;
				}
				if (cars > this.maxCarsOnLink) {
					this.maxCarsOnLink = cars;
					this.maxCarsOnLinkTime = time;
				}
			}
		}

		public void printInfo() {
			log.info("Statistics for link " + this.linkId + " in iteration " + this.iteration);
			log.info("  first car entered: " + this.firstCarEnter);
			log.info("   last car entered: " + this.lastCarEnter);
			log.info("     first car left: " + this.firstCarLeave);
			log.info("      last car left: " + this.lastCarLeave);
			log.info(" max # cars on link: " + this.maxCarsOnLink);
			log.info(" max # cars at time: " + this.maxCarsOnLinkTime);
		}
	}

	public static class TestControler extends Controler {

		public TestControler(final Config config) {
			super(config);
		}

		@Override
		protected void setup() {
			super.setup();

			// do some test to ensure the scenario is correct
			double beta_travel = this.config.charyparNagelScoring().getTraveling();
			if ((beta_travel != -6.0) && (beta_travel != -66.0)) {
				throw new IllegalArgumentException("Unexpected value for beta_travel. Expected -6.0 or -66.0, actual value is " + beta_travel);
			}

			int lastIter = this.config.controler().getLastIteration();
			if (lastIter < 100) {
				throw new IllegalArgumentException("Controler.lastIteration must be at least 100. Current value is " + lastIter);
			}
			if (lastIter > 100) {
				System.err.println("Controler.lastIteration is currently set to " + lastIter + ". Only the first 100 iterations will be analyzed.");
			}
		}
	}

	/*default*/ class TestControlerListener implements IterationStartsListener, IterationEndsListener {

		private final LinkAnalyzer la = new LinkAnalyzer("15");
		private final LinkQueueStats queueStats = new LinkQueueStats("15");

		public void notifyIterationStarts(final IterationStartsEvent event) {
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				this.la.reset(iteration);
				event.getControler().getEvents().addHandler(this.la);
			}

			if (iteration == 0) {
				event.getControler().getEvents().addHandler(this.queueStats);
			} else {
				this.queueStats.reset(iteration);
			}
		}

		public void notifyIterationEnds(final IterationEndsEvent event) {
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				event.getControler().getEvents().removeHandler(this.la);
				this.la.calcMaxCars();
				this.la.printInfo();
			}
			BufferedWriter out = null;
			try {
				out = new BufferedWriter(new FileWriter(Controler.getIterationFilename("queueStats.txt")));
				this.queueStats.dumpStats(out);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (out != null) {
					try {
						out.close();
					}
					catch (IOException ignored) {}
				}
			}
			if (iteration == 100) {
				double beta_travel = Double.parseDouble(Gbl.getConfig().getParam(CharyparNagelScoringFunction.CONFIG_MODULE, CharyparNagelScoringFunction.CONFIG_TRAVELING));
				/* explanation to the results:
				 * the triangle spawned by (firstCarEnter,0) - (maxCarsOnLinkTime,maxCarsOnLink) - (lastCarLeave,0)
				 * should have different forms between the two runs. For beta_travel = -6, the peak at
				 * maxCarsOnLinkTime should be higher. Thus, beta_travel = -66 should appear a bit wider spread.
				 * In theory, firstCarEnter and lastCarLeave should be equal or similar. In practice (in our case)
				 * they are likely to differ slightly.<br>
				 * See the paper "Economics of a bottleneck" by Arnott, De Palma and Lindsey, 1987.
				 */
				if (beta_travel == -6.0) {
					System.out.println("checking results for case `beta_travel = -6'...");
					assertEquals(19079.0, this.la.firstCarEnter, 0.0);
					assertEquals(24006.0, this.la.lastCarEnter, 0.0);
					assertEquals(19259.0, this.la.firstCarLeave, 0.0);
					assertEquals(24186.0, this.la.lastCarLeave, 0.0);
					assertEquals(30, this.la.maxCarsOnLink);
					assertEquals(22131.0, this.la.maxCarsOnLinkTime, 0.0);
					System.out.println("all checks passed!");
				} else if (beta_travel == -66.0) {
					System.out.println("checking results for case `beta_travel = -66'...");
					assertEquals(18917.0, this.la.firstCarEnter, 0.0);
					assertEquals(23793.0, this.la.lastCarEnter, 0.0);
					assertEquals(19097.0, this.la.firstCarLeave, 0.0);
					assertEquals(23973.0, this.la.lastCarLeave, 0.0);
					assertEquals(13, this.la.maxCarsOnLink);
					assertEquals(20874.0, this.la.maxCarsOnLinkTime, 0.0);
					System.out.println("all checks passed!");
				}
			}
		}
	}
}

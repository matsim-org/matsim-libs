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

package org.matsim.examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.charts.XYScatterChart;
import org.matsim.utils.misc.Time;

/**
 * This TestCase should ensure the correct behavior of agents when different
 * values for beta_travel-parameters are used, it does so by basically
 * implementing Vickrey's bottleneck model (see "Economics of a bottleneck" by
 * Arnott, De Palma and Lindsey, 1987). The agents drive on the equil-net, but
 * can only do departure-time adaptation, no rerouting. This leads to all
 * agents driving through the one bottleneck in the network. A special
 * time-adaption module is used (TimeAllocationMutatorBottleneck, inside this
 * class), that only mutates the endtime of the very first activity, but not 
 * the duration of other activities. This is to ensure that the duration of the
 * activities do not have any influence on the score, but only the travel-time 
 * (and with that, eventually shortened activity durations) has.
 *
 * @author mrieser
 */
public class BetaTravelTest extends MatsimTestCase {

	/* This TestCase uses a custom Controler, named TestControler, to load
	 * specific strategies. The strategies make use of a test-specific
	 * TimeAllocationMutator, the TimeAllocationMutatorBottleneck.
	 * LinkAnalyzer, an event handler, collects some statistics on the 
	 * "bottleneck-link". The BottleneckTravelTimeAnalyzer, an event handler
	 * as well, creates the graphs used to manually verify the correctness
	 * of this TestCase -- it generates a plot similar to the ones from
	 * Vickrey's model.
	 * A custom TestControlerListener is responsible for integrating all these
	 * event handlers into the TestControler.
	 * 
	 * **************************************************************************
	 * 
	 *                WHAT TO DO IF THESE TESTS FAIL?
	 * 
	 * In the output directories for iteration 100, there should be a file
	 * "100.bottleneck_times.png". Open the files for the two test cases. In the
	 * case of beta_travel = -66, the red dots in the graph should form a (more
	 * or less) straight line with slope 1. In the case of beta_travel = -6, the
	 * straight line can only be seen outside of the range [5.20, 5.60]. Within 
	 * this range first a higher slope should be seen until about 5.35, and then
	 * a lower slope, leading to a "triangle" form on the straight line.
	 * If this triangle form can still be seen clearly, update the values in
	 * TestControlerListener.notifyIterationEnds() that are used to automatically
	 * verify the tests. The actual values can be found in the output (log-file) 
	 * of these tests. They still should be similar to the current values.
	 * 
	 * **************************************************************************
	 */

	/**
	 * Runs the test with a value of -6 for beta_travel.
	 *
	 *  @author mrieser
	 */
	public void testBetaTravel_6() {
		Config config = loadConfig(getInputDirectory() + "config.xml");
		TestControler controler = new TestControler(config);
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.run();
	}

	/**
	 * Runs the test with a value of -66 for beta_travel.
	 *
	 * @author mrieser
	 */
	public void testBetaTravel_66() {
		Config config = loadConfig(getInputDirectory() + "config.xml");
		TestControler controler = new TestControler(config);
		controler.addControlerListener(new TestControlerListener());
		controler.setCreateGraphs(false);
		controler.run();
	}

	/**
	 * Collects some statistics on a specific link. Used to automatically verify
	 * the TestCase still works as intended.
	 * 
	 * @author mrieser
	 */
	private static class LinkAnalyzer implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI {
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

		public void handleEvent(final LinkEnterEnter event) {
			if (event.linkId.equals(this.linkId)) {
				this.enterTimes.add(Double.valueOf(event.time));
				if (event.time < this.firstCarEnter) this.firstCarEnter = event.time;
				if (event.time > this.lastCarEnter) this.lastCarEnter = event.time;
			}
		}

		public void handleEvent(final LinkLeaveEvent event) {
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

	/**
	 * A custom Controler for this TestCase that loads special strategies and 
	 * verifies that the loaded settings from the configuration files are 
	 * suitable to test what we want.
	 *
	 * @author mrieser
	 */
	private static class TestControler extends Controler {

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

		@Override
		protected StrategyManager loadStrategyManager() {
			StrategyManager manager = new StrategyManager();
			manager.setMaxPlansPerAgent(5);

			PlanStrategy strategy1 = new PlanStrategy(new ExpBetaPlanSelector());
			manager.addStrategy(strategy1, 0.80);

			PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
			strategy2.addStrategyModule(new TimeAllocationMutatorBottleneck());
			manager.addStrategy(strategy2, 0.80);

			// reduce the replanning probabilities over the iterations
			manager.addChangeRequest(50, strategy2, 0.30);
			manager.addChangeRequest(75, strategy2, 0.10);
			manager.addChangeRequest(95, strategy2, 0.00);

			return manager;
		}
	}

	/**
	 * Responsible for the verification of the tests. It adds a few event 
	 * handlers and checks their result in a specific iteration. 
	 *
	 * @author mrieser
	 */
	private class TestControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener {

		private final LinkAnalyzer la = new LinkAnalyzer("15");
		private BottleneckTravelTimeAnalyzer ttAnalyzer = null;

		public TestControlerListener() {
			// empty public constructor for private class
		}
		
		public void notifyStartup(final StartupEvent event) {
			this.ttAnalyzer = new BottleneckTravelTimeAnalyzer(event.getControler().getPopulation().getPersons().size());
		}

		public void notifyIterationStarts(final IterationStartsEvent event) {
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				this.la.reset(iteration);
				event.getControler().getEvents().addHandler(this.la);
			}

			if (iteration % 50 == 0) {
				this.ttAnalyzer.reset(iteration);
				event.getControler().getEvents().addHandler(this.ttAnalyzer);
			}
		}

		public void notifyIterationEnds(final IterationEndsEvent event) {
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				event.getControler().getEvents().removeHandler(this.la);
				this.la.calcMaxCars();
				this.la.printInfo();
			}

			if (iteration % 50 == 0) {
				this.ttAnalyzer.plot(Controler.getIterationFilename("bottleneck_times.png"));
				event.getControler().getEvents().removeHandler(this.ttAnalyzer);
			}
			if (iteration == 100) {
				double beta_travel = event.getControler().getConfig().charyparNagelScoring().getTraveling();
				/* ***************************************************************
				 * AUTOMATIC VERIFICATION OF THE TESTS:
				 * 
				 * Explanation to the results:
				 * the triangle spawned by (firstCarEnter,0) - (maxCarsOnLinkTime,maxCarsOnLink) - (lastCarLeave,0)
				 * should have different forms between the two runs. For beta_travel = -6, the peak at
				 * maxCarsOnLinkTime should be higher. Thus, beta_travel = -66 should appear a bit wider spread.
				 * In theory, firstCarEnter and lastCarLeave should be equal or similar. In practice (in our case)
				 * they are likely to differ slightly.<br>
				 * See the paper "Economics of a bottleneck" by Arnott, De Palma and Lindsey, 1987.
				 * 
				 * Change the values below to make the test pass, if they are consistent with the desired output.
				 */
				if (beta_travel == -6.0) {
					System.out.println("checking results for case `beta_travel = -6'...");
					assertEquals(18710.0, this.la.firstCarEnter, 0.0);
					assertEquals(21961.0, this.la.lastCarEnter, 0.0);
					assertEquals(18890.0, this.la.firstCarLeave, 0.0);
					assertEquals(22141.0, this.la.lastCarLeave, 0.0);
					assertEquals(59, this.la.maxCarsOnLink);
					assertEquals(19589.0, this.la.maxCarsOnLinkTime, 0.0);
					System.out.println("all checks passed!");
				} else if (beta_travel == -66.0) {
					System.out.println("checking results for case `beta_travel = -66'...");
					assertEquals(13590.0, this.la.firstCarEnter, 0.0);
					assertEquals(21961.0, this.la.lastCarEnter, 0.0);
					assertEquals(13770.0, this.la.firstCarLeave, 0.0);
					assertEquals(22141.0, this.la.lastCarLeave, 0.0);
					assertEquals(8, this.la.maxCarsOnLink);
					assertEquals(15904.0, this.la.maxCarsOnLinkTime, 0.0);
					System.out.println("all checks passed!");
				}
				/* *************************************************************** */
			}
		}
	}

	/** A special variant of the TimeAllocationMutator, suitable for the Bottleneck Analysis */
	private static class TimeAllocationMutatorBottleneck extends MultithreadedModuleA {
		public TimeAllocationMutatorBottleneck() {
			// empty public constructor for private class
		}
		
		@Override
		public PlanAlgorithmI getPlanAlgoInstance() {
			return new PlanMutateTimeAllocationBottleneck(1800);
		}
	}

	/** A special variant of the TimeAllocationMutator, suitable for the Bottleneck Analysis */
	private static class PlanMutateTimeAllocationBottleneck implements PlanAlgorithmI {

		private final int mutationRange;

		public PlanMutateTimeAllocationBottleneck(final int mutationRange) {
			this.mutationRange = mutationRange;
		}

		public void run(final Plan plan) {
			mutatePlan(plan);
		}

		private void mutatePlan(final Plan plan) {
			int max = plan.getActsLegs().size();
			int now = 0;

			// apply mutation to all activities except the last home activity
			for (int i = 0; i < max; i++ ) {

				if (i % 2 == 0) {
					Act act = (Act)(plan.getActsLegs().get(i));
					// invalidate previous activity times because durations will change
					act.setStartTime(Time.UNDEFINED_TIME);

					// handle first activity
					if (i == 0) {
						act.setStartTime(now); // set start to midnight
						act.setEndTime(mutateTime(act.getEndTime())); // mutate the end time of the first activity
						act.setDur(act.getEndTime() - act.getStartTime()); // calculate resulting duration
						now += act.getEndTime(); // move now pointer
					} else if (i < (max - 1)) {
						// handle middle activities
						act.setStartTime(now); // assume that there will be no delay between arrival time and activity start time
						act.setDur(6*3600); // <-- This line differs from the original PlanMutateTimeAllocation, use a fix time to minimize effect of act-duration on score
						act.setEndTime(Time.UNDEFINED_TIME); // <-- This line differs from the original PlanMutateTimeAllocation
						now += act.getDur();
					} else {
						// handle last activity
						act.setStartTime(now); // assume that there will be no delay between arrival time and activity start time
						// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
						act.setDur(Time.UNDEFINED_TIME);
						act.setEndTime(Time.UNDEFINED_TIME);
					}

				} else {

					Leg leg = (Leg)(plan.getActsLegs().get(i));

					// assume that there will be no delay between end time of previous activity and departure time
					leg.setDepTime(now);
					// let duration untouched. if defined add it to now
					if (leg.getTravTime() != Time.UNDEFINED_TIME) {
						now += leg.getTravTime();
					}
					// set planned arrival time accordingly
					leg.setArrTime(now);

				}
			}
		}

		private double mutateTime(final double time) {
			double t = time;
			if (t != Time.UNDEFINED_TIME) {
				t = t + (int)((Gbl.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);
				if (t < 0) t = 0;
				if (t > 24*3600) t = 24*3600;
			} else {
				t = Gbl.random.nextInt(24*3600);
			}
			return t;
		}
	}

	/**
	 * Collects the departure and arrival times of the first leg of each agent, 
	 * and plots them in XY-Scatter-Plot for manual verification in case this
	 * test case fails.
	 *
	 * @author mrieser
	 */
	private static class BottleneckTravelTimeAnalyzer implements EventHandlerAgentDepartureI, EventHandlerAgentArrivalI {

		private final HashMap<String, Double> agentDepTimes = new HashMap<String, Double>(); // <AgentId, Time>
		private int agentCounter = 0;
		private final double[] depTimes;
		private final double[] arrTimes;

		public BottleneckTravelTimeAnalyzer(final int popSize) {
			this.depTimes = new double[popSize];
			this.arrTimes = new double[popSize];
		}

		public void handleEvent(final AgentDepartureEvent event) {
			if (event.legId == 0) {
				this.agentDepTimes.put(event.agentId, Double.valueOf(event.time));
			}
		}

		public void handleEvent(final AgentArrivalEvent event) {
			String agentId = event.agentId;
			Double depTime = this.agentDepTimes.remove(agentId);
			if (depTime != null) {
				this.depTimes[this.agentCounter] = depTime.doubleValue() / 3600.0;
				this.arrTimes[this.agentCounter] = event.time / 3600.0;
				this.agentCounter++;
			}
		}

		public void reset(final int iteration) {
			this.agentDepTimes.clear();
			this.agentCounter = 0;
		}

		public void plot(final String filename) {
			XYScatterChart graph = new XYScatterChart("Bottleneck Analysis", "departure time", "arrival time");
			graph.addSeries("", this.depTimes, this.arrTimes);
			graph.saveAsPng(filename, 800, 600);
		}
	}

}

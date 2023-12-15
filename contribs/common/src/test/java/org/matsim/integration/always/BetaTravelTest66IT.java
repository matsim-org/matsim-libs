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

package org.matsim.integration.always;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import jakarta.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.testcases.MatsimTestUtils;

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
public class BetaTravelTest66IT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/*This TestCase uses a custom Controler, named TestControler, to load
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
	 * Runs the test with a value of -66 for beta_travel.
	 *
	 * @author mrieser
	 */
	@Test
	void testBetaTravel_66() {
		Config config = utils.loadConfig("../../examples/scenarios/equil/config.xml");
		ConfigUtils.loadConfig(config, utils.getInputDirectory() + "config.xml");
		config.controller().setWritePlansInterval(0);
		// ---
		Scenario scenario = ScenarioUtils.loadScenario(config);
		/*
		 * The input plans file is not sorted. After switching from TreeMap to LinkedHashMap
		 * to store the persons in the population, we have to sort the population manually.
		 * cdobler, oct'11
		 */
		PopulationUtils.sortPersons(scenario.getPopulation());
		// ---
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(StrategyManager.class).toProvider(MyStrategyManagerProvider.class);
			}
		});
		controler.addControlerListener(new TestControlerListener());
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.run();
	}

	/**
	 * Collects some statistics on a specific link. Used to automatically verify
	 * the TestCase still works as intended.
	 *
	 * @author mrieser
	 */
	private static class LinkAnalyzer implements LinkEnterEventHandler, LinkLeaveEventHandler {
		private final String linkId;
		double firstCarEnter = Double.POSITIVE_INFINITY;
		double lastCarEnter = Double.NEGATIVE_INFINITY;
		double firstCarLeave = Double.POSITIVE_INFINITY;
		double lastCarLeave = Double.NEGATIVE_INFINITY;
		int maxCarsOnLink = Integer.MIN_VALUE;
		double maxCarsOnLinkTime = Double.NEGATIVE_INFINITY;
		private int iteration = -1;

		private final ArrayList<Double> enterTimes = new ArrayList<>(100);
		private final ArrayList<Double> leaveTimes = new ArrayList<>(100);

		private static final Logger log = LogManager.getLogger(TestControlerListener.class);

		LinkAnalyzer(final String linkId) {
			this.linkId = linkId;
			reset(0);
		}

		@Override
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

		@Override
		public void handleEvent(final LinkEnterEvent event) {
			if (event.getLinkId().toString().equals(this.linkId)) {
				this.enterTimes.add(event.getTime());
				if (event.getTime() < this.firstCarEnter) this.firstCarEnter = event.getTime();
				if (event.getTime() > this.lastCarEnter) this.lastCarEnter = event.getTime();
			}
		}

		@Override
		public void handleEvent(final LinkLeaveEvent event) {
			if (event.getLinkId().toString().equals(this.linkId)) {
				this.leaveTimes.add(event.getTime());
				if (event.getTime() < this.firstCarLeave) this.firstCarLeave = event.getTime();
				if (event.getTime() > this.lastCarLeave) this.lastCarLeave = event.getTime();
			}
		}
		@Deprecated // use from BetaTravelTest6.  kai, dec'17
		protected void calcMaxCars() {
			Collections.sort(this.enterTimes);
			Collections.sort(this.leaveTimes);
			int idxEnter = 0;
			int idxLeave = 0;
			int cars = 0;

			double timeLeave = this.leaveTimes.get(idxLeave);
			double timeEnter = this.enterTimes.get(idxEnter);
			double time;

			while ((timeLeave != Double.POSITIVE_INFINITY) && (timeEnter != Double.POSITIVE_INFINITY)) {
				if (timeLeave <= timeEnter) {
					time = timeLeave;
					idxLeave++;
					if (idxLeave < this.leaveTimes.size()) {
						timeLeave = this.leaveTimes.get(idxLeave);
					} else {
						timeLeave = Double.POSITIVE_INFINITY;
					}
					cars--;
				} else {
					time = timeEnter;
					idxEnter++;
					if (idxEnter < this.enterTimes.size()) {
						timeEnter = this.enterTimes.get(idxEnter);
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

		@Deprecated // use from BetaTravelTest6.  kai, dec'17
		void printInfo() {
			log.info("Statistics for link " + this.linkId + " in iteration " + this.iteration);
			log.info("  first car entered: " + this.firstCarEnter);
			log.info("   last car entered: " + this.lastCarEnter);
			log.info("     first car left: " + this.firstCarLeave);
			log.info("      last car left: " + this.lastCarLeave);
			log.info(" max # cars on link: " + this.maxCarsOnLink);
			log.info(" max # cars at time: " + this.maxCarsOnLinkTime);
		}
	}

	@Deprecated // use from BetaTravelTest6.  kai, dec'17
	private static class MyStrategyManagerProvider implements Provider<StrategyManager> {

		@Inject Config config;

		@Override
		public StrategyManager get() {
			StrategyManager manager = new StrategyManager();
			manager.setMaxPlansPerAgent(5);

			PlanStrategyImpl strategy1 = new PlanStrategyImpl(new ExpBetaPlanSelector<>(config.scoring()));
			manager.addStrategy( strategy1, null, 0.80 );

			PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector<>());
			strategy2.addStrategyModule(new TimeAllocationMutatorBottleneck(config.global().getNumberOfThreads()));

			// Trying to replace this by the standard mutator ...
//			double mutationRange = 1800. ;
//			boolean affectingDuration = false ;
//			strategy2.addStrategyModule( new TimeAllocationMutator(config, mutationRange, affectingDuration));
			// ... but the test result looks different. kai, sep'15

			manager.addStrategy( strategy2, null, 0.80 );

			// reduce the replanning probabilities over the iterations
			manager.addChangeRequest( 50, strategy2, null, 0.30 );
			manager.addChangeRequest( 75, strategy2, null, 0.10 );
			manager.addChangeRequest( 95, strategy2, null, 0.00 );

			return manager;
		}
	}

	/**
	 * Responsible for the verification of the tests. It adds a few event
	 * handlers and checks their result in a specific iteration.
	 *
	 * @author mrieser
	 */
	@Deprecated // use from BetaTravelTest6.  kai, dec'17
	private static class TestControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener {

		private final LinkAnalyzer la = new LinkAnalyzer("15");
		private BottleneckTravelTimeAnalyzer ttAnalyzer = null;

		TestControlerListener() {
			// empty public constructor for private class
		}

		@Override
		public void notifyStartup(final StartupEvent event) {
            // do some test to ensure the scenario is correct
			double beta_travel = event.getServices().getConfig().scoring().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling();
            if ((beta_travel != -6.0) && (beta_travel != -66.0)) {
                throw new IllegalArgumentException("Unexpected value for beta_travel. Expected -6.0 or -66.0, actual value is " + beta_travel);
            }

            int lastIter = event.getServices().getConfig().controller().getLastIteration();
            if (lastIter < 100) {
                throw new IllegalArgumentException("Controler.lastIteration must be at least 100. Current value is " + lastIter);
            }
            if (lastIter > 100) {
                System.err.println("Controler.lastIteration is currently set to " + lastIter + ". Only the first 100 iterations will be analyzed.");
            }
            this.ttAnalyzer = new BottleneckTravelTimeAnalyzer(event.getServices().getScenario().getPopulation().getPersons().size());
		}

		@Override
		public void notifyIterationStarts(final IterationStartsEvent event) {
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				this.la.reset(iteration);
				event.getServices().getEvents().addHandler(this.la);
			}

			if (iteration % 50 == 0) {
				this.ttAnalyzer.reset(iteration);
				event.getServices().getEvents().addHandler(this.ttAnalyzer);
			}
		}

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			int iteration = event.getIteration();
			if (iteration % 10 == 0) {
				event.getServices().getEvents().removeHandler(this.la);
				this.la.calcMaxCars();
				this.la.printInfo();
			}

			if (iteration % 50 == 0) {
				this.ttAnalyzer.plot(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "bottleneck_times.png"));
				event.getServices().getEvents().removeHandler(this.ttAnalyzer);
			}
			if (iteration == 100) {
				double beta_travel = event.getServices().getConfig().scoring().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling();
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
					System.out.println("firstCarEnter=" + this.la.firstCarEnter + "; lastCarEnter=" + this.la.lastCarEnter + "; firstCarLeave=" + this.la.firstCarLeave +
							"; lastCarLeave=" + this.la.lastCarLeave + "; maxCarsOnLink=" + this.la.maxCarsOnLink + "; maxCarsOnLinkTime=" + this.la.maxCarsOnLinkTime ) ;
					assertEquals(18534.0, this.la.firstCarEnter, 0.0);
					assertEquals(21961.0, this.la.lastCarEnter, 0.0);
					assertEquals(18714.0, this.la.firstCarLeave, 0.0);
					assertEquals(22141.0, this.la.lastCarLeave, 0.0);
					assertEquals(54, this.la.maxCarsOnLink);
					assertEquals(19563.0, this.la.maxCarsOnLinkTime, 0.0);
					System.out.println("all checks passed!");

					// The results changed after Michał's change of the QueueWithBuffer slow capacity update.  See 4dda004a8db98d9a2e757b1896cd3250d4e84ba5 .
					// The old results were (only where we had changes):
//					assertEquals(18710.0, this.la.firstCarEnter, 0.0);
//					assertEquals(18890.0, this.la.firstCarLeave, 0.0);
//					assertEquals(59, this.la.maxCarsOnLink);
//					assertEquals(19589.0, this.la.maxCarsOnLinkTime, 0.0);

				} else if (beta_travel == -66.0) {
					System.out.println("checking results for case `beta_travel = -66'...");
					System.out.println("firstCarEnter=" + this.la.firstCarEnter + "; lastCarEnter=" + this.la.lastCarEnter + "; firstCarLeave=" + this.la.firstCarLeave +
							"; lastCarLeave=" + this.la.lastCarLeave + "; maxCarsOnLink=" + this.la.maxCarsOnLink + "; maxCarsOnLinkTime=" + this.la.maxCarsOnLinkTime ) ;
					assertEquals(14895.0, this.la.firstCarEnter, 0.0);
					assertEquals(21961.0, this.la.lastCarEnter, 0.0);
					assertEquals(15075.0, this.la.firstCarLeave, 0.0);
					assertEquals(22141.0, this.la.lastCarLeave, 0.0);
					assertEquals(9, this.la.maxCarsOnLink);
					assertEquals(18415.0, this.la.maxCarsOnLinkTime, 0.0);
					System.out.println("all checks passed!");

					// The results changed after Michał's change of the QueueWithBuffer slow capacity update.  See 4dda004a8db98d9a2e757b1896cd3250d4e84ba5 .
					// The old results were (only where we had changes):
//					assertEquals(13590.0, this.la.firstCarEnter, 0.0);
//					assertEquals(13770.0, this.la.firstCarLeave, 0.0);
//					assertEquals(8, this.la.maxCarsOnLink);
//					assertEquals(15904.0, this.la.maxCarsOnLinkTime, 0.0);
				}
				/* *************************************************************** */
			}
		}
	}

	/** A special variant of the TimeAllocationMutator, suitable for the Bottleneck Analysis */
	@Deprecated // use from BetaTravelTest6.  kai, dec'17
	private static class TimeAllocationMutatorBottleneck extends AbstractMultithreadedModule {
		/*package*/ TimeAllocationMutatorBottleneck(final int numOfThreads) {
			super(numOfThreads);
		}

		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			return new PlanMutateTimeAllocationBottleneck(1800);
		}
	}

	/** A special variant of the TimeAllocationMutator, suitable for the Bottleneck Analysis */
	@Deprecated // use from BetaTravelTest6.  kai, dec'17
	private static class PlanMutateTimeAllocationBottleneck implements PlanAlgorithm {

		private final int mutationRange;

		PlanMutateTimeAllocationBottleneck(final int mutationRange) {
			this.mutationRange = mutationRange;
		}

		@Override
		public void run(final Plan plan) {
			mutatePlan(plan);
		}

		private void mutatePlan(final Plan plan) {
			int max = plan.getPlanElements().size();
			int now = 0;

			// apply mutation to all activities except the last home activity
			int i = -1;
			for (PlanElement pe : plan.getPlanElements()) {
				i++;
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					// invalidate previous activity times because durations will change
					act.setStartTimeUndefined();

					// handle first activity
					if (i == 0) {
						act.setStartTime(now); // set start to midnight
						act.setEndTime(mutateTime(act.getEndTime())); // mutate the end time of the first activity
						act.setMaximumDuration(act.getEndTime().seconds() - act.getStartTime().seconds()); // calculate resulting duration
						now += act.getEndTime().seconds(); // move now pointer
					} else if (i < (max - 1)) {
						// handle middle activities
						act.setStartTime(now); // assume that there will be no delay between arrival time and activity start time
						act.setMaximumDuration(6*3600); // <-- This line differs from the original PlanMutateTimeAllocation, use a fix time to minimize effect of act-duration on score
						act.setEndTimeUndefined(); // <-- This line differs from the original PlanMutateTimeAllocation
						now += 6*3600.0;
					} else {
						// handle last activity
						act.setStartTime(now); // assume that there will be no delay between arrival time and activity start time
						// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
						act.setMaximumDurationUndefined();
						act.setEndTimeUndefined();
					}

				}
				if (pe instanceof Leg) {

					Leg leg = (Leg) pe;

					// assume that there will be no delay between end time of previous activity and departure time
					leg.setDepartureTime(now);
					// let duration untouched. if defined add it to now
					if (leg.getTravelTime().isDefined()) {
						now += leg.getTravelTime().seconds();
					}
					// set planned arrival time accordingly
					final double arrTime = now;
					leg.setTravelTime( arrTime - leg.getDepartureTime().seconds());

				}
			}
		}

		private double mutateTime(final OptionalTime time) {
			if (time.isDefined()) {
				double t = time.seconds() + (int)((MatsimRandom.getRandom().nextDouble() * 2.0 - 1.0) * this.mutationRange);
				if (t < 0) t = 0;
				if (t > 24*3600) t = 24*3600;
				return t;
			} else {
				return MatsimRandom.getRandom().nextInt(24*3600);
			}
		}
	}

	/**
	 * Collects the departure and arrival times of the first leg of each agent,
	 * and plots them in XY-Scatter-Plot for manual verification in case this
	 * test case fails.
	 *
	 * @author mrieser
	 */
	@Deprecated // use from BetaTravelTest6.  kai, dec'17
	private static class BottleneckTravelTimeAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler {

		private final HashMap<Id<Person>, Double> agentDepTimes = new HashMap<>(); // <AgentId, Time>
		private final HashSet<Id<Person>> agentSeen = new HashSet<>();
		private int agentCounter = 0;
		private final double[] depTimes;
		private final double[] arrTimes;

		BottleneckTravelTimeAnalyzer(final int popSize) {
			this.depTimes = new double[popSize];
			this.arrTimes = new double[popSize];
		}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			if (!this.agentSeen.contains(event.getPersonId())) { // only store first departure
				this.agentDepTimes.put(event.getPersonId(), event.getTime());
				this.agentSeen.add(event.getPersonId());
			}
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			Double depTime = this.agentDepTimes.remove(event.getPersonId());
			if (depTime != null) {
				this.depTimes[this.agentCounter] = depTime / 3600.0;
				this.arrTimes[this.agentCounter] = event.getTime() / 3600.0;
				this.agentCounter++;
			}
		}

		@Override
		public void reset(final int iteration) {
			this.agentDepTimes.clear();
			this.agentSeen.clear();
			this.agentCounter = 0;
		}

		void plot(final String filename) {
			XYScatterChart graph = new XYScatterChart("Bottleneck Analysis", "departure time", "arrival time");
			graph.addSeries("", this.depTimes, this.arrTimes);
			graph.saveAsPng(filename, 800, 600);
		}
	}

}

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarEuclideanFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class RouterPerformanceTest {

	/*package*/ static final Logger log = Logger.getLogger(RouterPerformanceTest.class);

	public static void main(final String[] dummyArgs) {
//		args = new String[] {"/Volumes/Data/projects/speedupTransit/bvg1pct_old/network.cleaned.xml", "/Volumes/Data/projects/speedupTransit/bvg1pct_old/plans.xml"};
//		args = new String[] {"/Volumes/Data/vis/ch25pct_kti/network.c.xml.gz", "/Volumes/Data/vis/ch25pct_kti/planssample.xml"};

		String[][] argss = new String[][] {
				{"/Volumes/Data/projects/speedupTransit/bvg1pct_old/network.cleaned.xml", "/Volumes/Data/projects/speedupTransit/bvg1pct_old/plans.xml"},
				{"/Volumes/Data/vis/ch25pct_kti/network.c.xml.gz", "/Volumes/Data/vis/ch25pct_kti/planssample.xml"}
		};


		int limit = 4000;

		for (int i = 0; i < 5; i++) {

			log.info("###");
			log.info("### PASS: " + i);
			log.info("###");

			for (String[] args2 : argss) {

				log.info("###");
				log.info("### Using Network: " + args2[0]);
				log.info("### Using Population: " + args2[1]);
				log.info("###");

				Config config = ConfigUtils.createConfig();
				config.network().setInputFile(args2[0]);
				config.plans().setInputFile(args2[1]);

				doTest(new DijkstraProvider(), config, limit);
				doTest(new AStarLandmarksProvider(), config, limit);
//				doTest(new DijkstraV4Provider(), config, limit);
//				doTest(new AStarLandmarksV4Provider(), config, limit);
			}
		}
	}

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculatorFactory getFactory(Network network, TravelMinCost costCalc, TravelTime timeCalc);
	}

	private static class DijkstraProvider implements RouterProvider {
		@Override
		public String getName() {
			return "Dijkstra";
		}
		@Override
		public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
			return new DijkstraFactory();
		}
	}

	private static class DijkstraPruneDeadEndsProvider implements RouterProvider {
		@Override
		public String getName() {
			return "DijkstraPruneDeadends";
		}
		@Override
		public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
			PreProcessDijkstra preProcessData = new PreProcessDijkstra();
			preProcessData.run(network);
			return new DijkstraFactory(preProcessData);
		}
	}

	private static class AStarEuclideanProvider implements RouterProvider {
		@Override
		public String getName() {
			return "AStarEuclidean";
		}
		@Override
		public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
			return new AStarEuclideanFactory(network, costCalc);
		}
	}

	private static class AStarLandmarksProvider implements RouterProvider {
		@Override
		public String getName() {
			return "AStarLandmarks";
		}
		@Override
		public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
			return new AStarLandmarksFactory(network, costCalc);
		}
	}

	private static void doTest(final RouterProvider provider, final Config config, final int limit) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());

		String inPlansName = config.plans().getInputFile();

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(inPlansName);
		log.info("### persons: " + population.getPersons().size());
		log.info("### current limit: " + limit);

		log.info("### calcRoute with router \t" + provider.getName());

		FreespeedTravelTimeCost calculator = new FreespeedTravelTimeCost(config.planCalcScore());

		measureMemory("### before creating Router Factory");
		long start = System.currentTimeMillis();
		LeastCostPathCalculatorFactory algo = provider.getFactory(network, calculator, calculator);
		long end = System.currentTimeMillis();
		log.info("### Creating Router Factory took \t" + (end - start) + "\t ms");
		measureMemory("### before creating Router 1");
		start = System.currentTimeMillis();
		PlansCalcRoute router = new PlansCalcRoute(config.plansCalcRoute(), network, calculator, calculator, algo, ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory()) {
			int cnt = 0;
			@Override
			public void run(final Person person) {
				this.cnt++;
				if (this.cnt > limit) {
					throw new RuntimeException("Enough's enough");
				}
				super.run(person);
			}
			@Override
			public void run(final Plan plan) {
				this.cnt++;
				if (this.cnt > limit) {
					throw new RuntimeException("Enough's enough");
				}
				super.run(plan);
			}
			@Override
			public double handleLeg(final Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
				leg.setMode(TransportMode.car); // force car router
				return super.handleLeg(person, leg, fromAct, toAct, depTime);
			}
		};
		end = System.currentTimeMillis();
		log.info("### Creating Router 1 took \t" + (end - start) + "\t ms");
		measureMemory("### before creating Router 2");
		start = System.currentTimeMillis();
		PlansCalcRoute router2 = new PlansCalcRoute(config.plansCalcRoute(), network, calculator, calculator, algo, ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory()) {
			int cnt = 0;
			@Override
			public void run(final Person person) {
				this.cnt++;
				if (this.cnt > limit) {
					throw new RuntimeException("Enough's enough");
				}
				super.run(person);
			}
			@Override
			public void run(final Plan plan) {
				this.cnt++;
				if (this.cnt > limit) {
					throw new RuntimeException("Enough's enough");
				}
				super.run(plan);
			}
			@Override
			public double handleLeg(final Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
				leg.setMode(TransportMode.car); // force car router
				return super.handleLeg(person, leg, fromAct, toAct, depTime);
			}
		};
		end = System.currentTimeMillis();
		log.info("### Creating Router 2 took \t" + (end - start) + "\t ms");
		measureMemory("### before Routing 1");
		start = System.currentTimeMillis();
		try {
			router.run(population);
		} catch (RuntimeException e) {
		}
		long part1 = System.currentTimeMillis() - start;
		measureMemory("### before Routing 2");
		start = System.currentTimeMillis();
		try {
			router2.run(population);
		} catch (RuntimeException e) {
		}
		end = System.currentTimeMillis();
		measureMemory("### after Routing");
		log.info("### Routing took \t" + (part1 + end - start) + "\t ms");
		System.out.println(router.hashCode()); // just some code to ensure that the router is not gc'ed before the end
		System.out.println(router2.hashCode());
	}

	private static void measureMemory(final String message) {
		try {
			System.gc();
			Thread.sleep(100);
			System.gc();
			Thread.sleep(100);
			System.gc();
			Thread.sleep(100);
			System.gc();
			Thread.sleep(100);
			System.gc();
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		long usedMem = totalMem - freeMem;
		log.info(message + " | used RAM: \t" + usedMem + "\t B = " + (usedMem/1024) + "kB = " + (usedMem/1024/1024) + "MB" +
				"  free: " + freeMem + "B = " + (freeMem/1024/1024) + "MB  total: " + totalMem + "B = " + (totalMem/1024/1024) + "MB");
	}

}

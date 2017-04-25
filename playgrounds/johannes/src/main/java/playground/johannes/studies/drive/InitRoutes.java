/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.studies.drive;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scenario.ScenarioUtils;

import javax.inject.Provider;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author johannes
 *
 */
public class InitRoutes {

	private static final Logger logger = Logger.getLogger(InitRoutes.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		logger.info("Loading population...");
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);

		logger.info("Loading network...");
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(args[1]);
		
		int numThreads = Integer.parseInt(args[3]);
		
		RunThread wrappers[] = new RunThread[numThreads];
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		Future<?>[] futures = new Future[numThreads];

		Population pop = scenario.getPopulation();
		Collection<Person> plans = (Collection<Person>) pop.getPersons().values();
		/*
		Set links for activities.
		 */
		logger.info("Setting activity links...");
		for(Person person : plans) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					Link link = NetworkUtils.getNearestLink(scenario.getNetwork(), act.getCoord());
					act.setLinkId(link.getId());
				}
			}
		}
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(plans.size(), wrappers.length);
		List<Person>[] segments = CollectionUtils.split(plans, n);
		/*
		 * submit tasks
		 */
		logger.info("Routing...");
		for(int i = 0; i < segments.length; i++) {
			if(wrappers[i] == null) {
				wrappers[i] = new RunThread(scenario, config, segments[i]);
			}
			futures[i] = executor.submit(wrappers[i]);
		}
		/*
		 * wait for threads
		 */
		ProgressLogger.init(plans.size(), 1, 10);
		for(int i = 0; i < segments.length; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();

		logger.info("Writing population...");
		PopulationWriter writer = new PopulationWriter(pop);
		writer.writeV5(args[2]);
		logger.info("Done.");
	}

	private static class RunThread implements Runnable {

		private Scenario scenario;
		
		private Config config;
		
		private Collection<Person> persons;
		
		public RunThread(Scenario scenario, Config config, Collection<Person> persons) {
			this.scenario = scenario;
			this.config = config;
			this.persons = persons;
		}
		
		@Override
		public void run() {
			
			Network network = scenario.getNetwork();
			
			final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
			
			Provider<TripRouter> tripRouterFact = TripRouterFactoryBuilderWithDefaults.createTripRouterProvider(
					scenario, new AStarLandmarksFactory(network,
							timeCostCalc, 1), null);
			
			PlanRouter router = new PlanRouter( tripRouterFact.get() , null ) ;
		
			for(Person person : persons) {
				router.run(person);

				for(Plan plan : person.getPlans()) {
					for(int i = 2; i < plan.getPlanElements().size(); i+=2) {
						Activity act = (Activity) plan.getPlanElements().get(i);
						Activity prev = (Activity) plan.getPlanElements().get(i - 2);
						Leg leg = (Leg) plan.getPlanElements().get(i - 1);

						act.setStartTime(prev.getEndTime() + leg.getTravelTime());
						if(act.getEndTime() < act.getStartTime()) {
							act.setEndTime(act.getStartTime() + 1);
						}
					}
				}

				ProgressLogger.step();
			}
		}
		
	}
}

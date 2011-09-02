/* *********************************************************************** *
 * project: org.matsim.*
 * PlansGenerator.java
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
package playground.johannes.socialnetworks.sim.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.socialnetworks.sim.locationChoice.ActivityMover;
import playground.johannes.socialnetworks.statistics.ExponentialDistribution;
import playground.johannes.socialnetworks.statistics.LogNormalDistribution;

/**
 * @author illenberger
 *
 */
public class PlansGenerator {

	private static final Logger logger = Logger.getLogger(PlansGenerator.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(args[0]);
		
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(args[1]);
		
		Population population = scenario.getPopulation();
		
		ArrayList<Link> links = new ArrayList<Link>(scenario.getNetwork().getLinks().values());
		
		Random random = new Random();
		double p = 0;
		/*
		 * Delete persons
		 */
		Set<Id> remove = new HashSet<Id>();
		for(Person person : population.getPersons().values()) {
			if(random.nextDouble() < p) {
				remove.add(person.getId());
			}
		}
		for(Id id : remove) {
			population.getPersons().remove(id);
		}
		logger.info(String.format("%1$s persons left.", population.getPersons().size()));
		/*
		 * Delete all plans.
		 */
		logger.info("Deleting plans...");
		Map<Person, Id> homeLocs = new HashMap<Person, Id>();
		for(Person person : population.getPersons().values()) {
			homeLocs.put(person, ((Activity) person.getPlans().get(0).getPlanElements().get(0)).getLinkId());
			person.getPlans().clear();
		}
		/*
		 * Create new plans.
		 */
//		TravelTime travelTime = new FreespeedTravelTimeCost(-6/3600.0, 0, 0);
//		LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), (TravelCost) travelTime, travelTime);
		
//		ActivityMover mover = new ActivityMover(population.getFactory(), router, scenario.getNetwork());
		
		
		LogNormalDistribution startPdf = new LogNormalDistribution(0.3, 10.8, 1165);
		StartTimeAllocation startAlloc = new StartTimeAllocation(startPdf, 28800, 86400, random);
		
		ExponentialDistribution durPdf = new ExponentialDistribution(-0.0001, 0.06);
		DurationAllocation durAlloc = new DurationAllocation(durPdf, 12*60*60, random);
		
		logger.info("Creating plans...");
		
		ConcurrentLinkedQueue<Person> persons = new ConcurrentLinkedQueue<Person>(population.getPersons().values());
		
		int numThreads = 2;
		LocalThread[] threads = new LocalThread[numThreads];
		for(int i = 0; i < numThreads; i++) {
			TravelTime travelTime = new FreespeedTravelTimeCost(-6/3600.0, 0, 0);
			LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), (TravelCost) travelTime, travelTime);
			threads[i] = new LocalThread();
			threads[i].persons = persons;
			threads[i].scenario = scenario;
			threads[i].homeLocs = homeLocs;
			threads[i].random = random;
//			threads[i].mover = new ActivityMover(population.getFactory(), router, scenario.getNetwork());
			throw new RuntimeException();
//			threads[i].startAlloc = startAlloc;
//			threads[i].durAlloc = durAlloc;
//			threads[i].links = links;
		}
		
		for(LocalThread thread : threads) {
			thread.start();
		}
		
		for(LocalThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		PopulationWriter writer = new PopulationWriter(population, scenario.getNetwork());
		writer.write(String.format("%1$s/plans.init.%2$s.xml", args[2], population.getPersons().size()));
	}

	public static class LocalThread extends Thread {
		
		private ConcurrentLinkedQueue<Person> persons;
		
		private Scenario scenario;
		
		private Map<Person, Id> homeLocs;
		
		private Random random;
		
		private ActivityMover mover;
		
		private StartTimeAllocation startAlloc;
		
		private DurationAllocation durAlloc;
		
		private ArrayList<Link> links;
		
		public static int cnt;
		
		public void run() {
			PopulationFactory factory = scenario.getPopulation().getFactory();

			Person person = null;
			while ((person = persons.poll()) != null) {
				Id linkId = homeLocs.get(person);
				
				Activity home1 = factory.createActivityFromLinkId("home", linkId);
				home1.setEndTime(8 * 60 * 60);
				
				Leg toLeg = factory.createLeg("car");
				toLeg.setDepartureTime(home1.getEndTime());
				toLeg.setTravelTime(0);
				
				Activity leisure = factory.createActivityFromLinkId("leisure", linkId);
				leisure.setEndTime(18 * 60 * 60);
				
				Leg fromLeg = factory.createLeg("car");
				fromLeg.setDepartureTime(leisure.getEndTime());
				fromLeg.setTravelTime(0);
				
				Activity home2 = factory.createActivityFromLinkId("home", linkId);

				Plan plan = factory.createPlan();
				plan.addActivity(home1);
				plan.addLeg(toLeg);
				plan.addActivity(leisure);
				plan.addLeg(fromLeg);
				plan.addActivity(home2);

				mover.moveActivity(plan, 2, links.get(random.nextInt(links.size())).getId(), home1.getEndTime(), 10*60*60);

//				toLeg = (Leg) plan.getPlanElements().get(1);
//				fromLeg = (Leg) plan.getPlanElements().get(3);
//				leisure = (Activity) plan.getPlanElements().get(2);

//				leisure.setStartTime(home1.getEndTime() + toLeg.getTravelTime());
//				home2.setStartTime(leisure.getEndTime() + fromLeg.getTravelTime());

				startAlloc.handlePlan(plan);
				durAlloc.handlePlan(plan);

				person.addPlan(plan);
				
				cnt++;
				if(cnt % 100 == 0) {
					logger.info(String.format("Processed %1$s persons...", cnt));
				}
			}
		}
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * TestSpeed.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.plain.io;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.xml.sax.SAXException;

import playground.johannes.plans.plain.PlainPopulationBuilder;
import playground.johannes.plans.plain.impl.PlainPersonImpl;
import playground.johannes.plans.plain.impl.PlainPopulationBuilderImpl;
import playground.johannes.plans.plain.impl.PlainPopulationImpl;
import playground.johannes.plans.view.Leg;
import playground.johannes.plans.view.Person;
import playground.johannes.plans.view.Plan;
import playground.johannes.plans.view.PlanElement;
import playground.johannes.plans.view.Population;
import playground.johannes.plans.view.Route;
import playground.johannes.plans.view.impl.IdMapping;
import playground.johannes.plans.view.impl.PersonView;
import playground.johannes.plans.view.impl.PopulationView;

/**
 * @author illenberger
 *
 */
public class TestSpeed extends TestCase {
	
	private static Logger logger = Logger.getLogger(TestSpeed.class);

	private long baseMem;
	
	private long time;
	
	private Config config;
	
//	public TestSpeed() {
//		if(config == null)
//		   config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
//	}
	
	
	public void testNew() throws SAXException, ParserConfigurationException, IOException {
		config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadNetwork();
		IdMapping.network = loader.getScenario().getNetwork();
		
		baseMem = getMemory();
		
		PlainPopulationBuilder builder = new PlainPopulationBuilderImpl();
		PlainPopulationXMLParser parser = new PlainPopulationXMLParser(builder);
		/*
		 * Load population...
		 */
		parser.parse("/Users/fearonni/vsp-work/runs-svn/run669/it.1000/outplans.xml");
		PlainPopulationImpl population = (PlainPopulationImpl) parser.getPopulation();
		printMemoryUsage();
		/*
		 * Initialize view...
		 */
		logger.info("Initializing population view...");
		Population view = new PopulationView(population);
		printMemoryUsage();
		/*
		 * Read test 1...
		 */
		fullRead(view);
		/*
		 * Read test 2...
		 */
		fullRead(view);
		/*
		 * Insert persons on view layer...
		 */
		logger.info("Inserting 1000 persons on view layer...");
		startTime();
		for(int i = 0; i < 10000; i++) {
			Person person = new PersonView(new PlainPersonImpl(new IdImpl(10000000+i)));
			view.addPerson(person);
		}
		stopTime();
		printMemoryUsage();
		fullRead(view);
		/*
		 * Removing persons on plain layer...
		 */
		logger.info("Removing 1000 persons on view layer...");
		startTime();
		for(int i = 0; i < 10000; i++) {
			view.removePerson(new IdImpl(10000000+i));
//			Person person = view.getPersons().get(new IdImpl(10000000+i));
//			view.removePerson(person);
		}
		stopTime();
		printMemoryUsage();
		fullRead(view);
		/*
		 * Removing one plan per person on view layer...
		 */
		logger.info("Removing one plan per person on view layer...");
		startTime();
		for(Person person : view.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			person.removePlan(plan);
		}
		stopTime();
		printMemoryUsage();
		fullRead(view);
		/*
		 * Exchanging route of first leg
		 */
		logger.info("Exchanging routes...");
		startTime();
		for(Person person : view.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				Leg leg = (Leg) plan.getPlanElements().get(1);
				Route route = leg.getRoute();
				List<Link> links = route.getLinks();
				route.setLinks(links);
			}
		}
		stopTime();
		fullRead(view);
	}
	
	private void fullRead(Population view) {
		logger.info("Iteration over all population elements...");
		startTime();
		for(Person p : view.getPersons().values()) {
			for(Plan plan : p.getPlans()) {
				for(PlanElement e : plan.getPlanElements()) {
					if(e instanceof Leg) {
						Route route = ((Leg)e).getRoute();
						route.getLinks();
					}
				}
			}
		}
		stopTime();
		printMemoryUsage();
	}
	
	public void testOld() {
		baseMem = getMemory();
		config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
		ScenarioImpl scenario = loader.getScenario();
		scenario.getNetwork();
		PopulationImpl population = scenario.getPopulation();
		printMemoryUsage();
		/*
		 * Read test 1...
		 */
		fullReadOld(population);
		/*
		 * Insert persons ...
		 */
		logger.info("Inserting 10000 persons ...");
		startTime();
		for(int i = 0; i < 10000; i++) {
			org.matsim.api.core.v01.population.Person person = new PersonImpl(new IdImpl(10000000+i));
			population.addPerson(person);
		}
		stopTime();
		printMemoryUsage();
		fullReadOld(population);
		/*
		 * Removing persons on plain layer...
		 */
		logger.info("Removing 10000 persons...");
		startTime();
		for(int i = 0; i < 10000; i++) {
			population.getPersons().remove(new IdImpl(10000000+i));
		}
		stopTime();
		printMemoryUsage();
		fullReadOld(population);
		/*
		 * Removing one plan per person on view layer...
		 */
		logger.info("Removing one plan per person...");
		startTime();
		for(org.matsim.api.core.v01.population.Person person : population.getPersons().values()) {
			org.matsim.api.core.v01.population.Plan plan = person.getPlans().get(0);
//			person.getPlans().remove(0);
			person.getPlans().remove(plan);
		}
		stopTime();
		printMemoryUsage();
		fullReadOld(population);
		/*
		 * Exchanging route of first leg
		 */
		logger.info("Exchanging routes...");
		startTime();
		for(org.matsim.api.core.v01.population.Person person : population.getPersons().values()) {
			for(org.matsim.api.core.v01.population.Plan plan : person.getPlans()) {
				org.matsim.api.core.v01.population.Leg leg = (org.matsim.api.core.v01.population.Leg) plan.getPlanElements().get(1);
				org.matsim.api.core.v01.population.Route route = (org.matsim.api.core.v01.population.Route) leg.getRoute();
				// can not compare...
			}
		}
		stopTime();
		fullReadOld(population);
	}
	
	private void fullReadOld(org.matsim.api.core.v01.population.Population population) {
		logger.info("Iteration over all population elements...");
		startTime();
		for(org.matsim.api.core.v01.population.Person p : population.getPersons().values()) {
			for(org.matsim.api.core.v01.population.Plan plan : p.getPlans()) {
				for(org.matsim.api.core.v01.population.PlanElement e : plan.getPlanElements()) {
					if(e instanceof org.matsim.api.core.v01.population.Leg) {
						org.matsim.api.core.v01.population.Route route = ((org.matsim.api.core.v01.population.Leg)e).getRoute();
//						((NodeNetworkRoute)route).getLinks();
						route.getStartLinkId();
						route.getEndLinkId();
					}
				}
			}
		}
		stopTime();
		printMemoryUsage();	
	}
	
	private void startTime() {
		time = System.currentTimeMillis();
	}
	
	private void stopTime() {
		time = System.currentTimeMillis() - time;
		logger.info(String.format("Took %1$.4f secs.", time/1000.0));
	}
	
	public long getMemory() {
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		long usedMem = totalMem - freeMem;
		return usedMem;
	}
	
	public void printMemoryUsage() {
		logger.info(String.format("Memory usage: %1$.4f MB.", (getMemory()-baseMem)/1E6));
	}
}

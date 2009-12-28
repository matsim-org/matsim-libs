/* *********************************************************************** *
 * project: org.matsim.*
 * Test.java
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

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.xml.sax.SAXException;

import playground.johannes.plans.plain.PlainPopulationBuilder;
import playground.johannes.plans.plain.impl.PlainPopulationBuilderImpl;
import playground.johannes.plans.plain.impl.PlainPopulationImpl;
import playground.johannes.plans.view.Leg;
import playground.johannes.plans.view.Person;
import playground.johannes.plans.view.Plan;
import playground.johannes.plans.view.PlanElement;
import playground.johannes.plans.view.Population;
import playground.johannes.plans.view.impl.IdMapping;
import playground.johannes.plans.view.impl.PopulationView;

/**
 * @author illenberger
 *
 */
public class TestMemory extends TestCase {

	long baseMem;
	
	public void testNew() throws SAXException, ParserConfigurationException, IOException {
		
		Config config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadNetwork();
		IdMapping.network = loader.getScenario().getNetwork();
		
		baseMem = getMemory();
		
		PlainPopulationBuilder builder = new PlainPopulationBuilderImpl();
		PlainPopulationXMLParser parser = new PlainPopulationXMLParser(builder);
		
		parser.parse("/Users/fearonni/vsp-work/runs-svn/run669/it.1000/outplans.xml");
		PlainPopulationImpl population = (PlainPopulationImpl) parser.getPopulation();
		printMemoryUsage();
		System.out.println(String.format("Population consists og %1$s persons.", population.getPersons().size()));
		
		Population view = new PopulationView(population);
		int counter = 0;
		for(Person p : view.getPersons().values()) {
			for(Plan plan : p.getPlans()) {
				for(PlanElement e : plan.getPlanElements()) {
					if(e instanceof Leg) {
						((Leg)e).getRoute();
					}
				}
			}
			counter++;
			if(counter % 10000 == 0) {
				System.out.println(counter + " persons...");
				printMemoryUsage();
			}
		}
	}
	
	public void testOld() {
		baseMem = getMemory();
		Config config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/runs-svn/run669/config.xml"});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
//		ScenarioImpl data = loader.getScenario();
		ScenarioImpl scenario = loader.getScenario();
		scenario.getNetwork();
		PopulationImpl population = scenario.getPopulation();
		
		printMemoryUsage();
	}
	
	public long getMemory() {
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		long usedMem = totalMem - freeMem;
		return usedMem;
	}
	
	public void printMemoryUsage() {
		System.out.println(String.format("Memory usage: %1$s MBytes", (getMemory()-baseMem)/1000000));
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatJGAPConfigurationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.planomat;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jgap.BaseGeneticOperator;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.WeightedRouletteSelector;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatJGAPConfigurationTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(PlanomatJGAPConfigurationTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(this.getInputDirectory() + "config.xml");
	}

	public void testPlanomatJGAPConfiguration() {

		log.info("Reading facilities xml file...");
		Facilities facilities = (Facilities)Gbl.createWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");

		log.info("Reading network xml file...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		Population population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = population.getPerson(new IdImpl("100"));
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(testPlan);

		// run testee
		PlanomatJGAPConfiguration jgapConfig = new PlanomatJGAPConfiguration(testPlan, planAnalyzeSubtours);

		// test correct setting of natural selector
		assertEquals(0, jgapConfig.getNaturalSelectorsSize(false));
		assertEquals(1, jgapConfig.getNaturalSelectorsSize(true));
		assertEquals(WeightedRouletteSelector.class, jgapConfig.getNaturalSelector(true, 0).getClass());

		// test correct setting of population size
		int expectedPopSize = 14;
		assertEquals(expectedPopSize, jgapConfig.getPopulationSize());

		// test correct settings for genetic operators
		assertEquals(2, jgapConfig.getGeneticOperators().size());
		Iterator geneticOperatorsIterator = jgapConfig.getGeneticOperators().iterator();
		while (geneticOperatorsIterator.hasNext()) {
			BaseGeneticOperator bgo = (BaseGeneticOperator) geneticOperatorsIterator.next();
			if (bgo.getClass().equals(CrossoverOperator.class)) {
				assertEquals(0.6d, ((CrossoverOperator) bgo).getCrossOverRatePercent(), EPSILON);
			} else if (bgo.getClass().equals(MutationOperator.class)) {
				assertEquals(expectedPopSize, ((MutationOperator) bgo).getMutationRate());
			}
		}
	}

	public void testPlanomatJGAPConfigurationCarPt() {

		log.info("Reading facilities xml file...");
		Facilities facilities = (Facilities)Gbl.createWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");

		log.info("Reading network xml file...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		Population population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = population.getPerson(new IdImpl("100"));
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(testPlan);

		// run testee
		PlanomatJGAPConfiguration jgapConfig = new PlanomatJGAPConfiguration(testPlan, planAnalyzeSubtours);

		// test correct setting of natural selector
		assertEquals(0, jgapConfig.getNaturalSelectorsSize(false));
		assertEquals(1, jgapConfig.getNaturalSelectorsSize(true));
		assertEquals(WeightedRouletteSelector.class, jgapConfig.getNaturalSelector(true, 0).getClass());

		// test correct setting of population size
		int expectedPopSize = 16;
		assertEquals(expectedPopSize, jgapConfig.getPopulationSize());

		// test correct settings for genetic operators
		assertEquals(2, jgapConfig.getGeneticOperators().size());
		Iterator geneticOperatorsIterator = jgapConfig.getGeneticOperators().iterator();
		while (geneticOperatorsIterator.hasNext()) {
			BaseGeneticOperator bgo = (BaseGeneticOperator) geneticOperatorsIterator.next();
			if (bgo.getClass().equals(CrossoverOperator.class)) {
				assertEquals(0.6d, ((CrossoverOperator) bgo).getCrossOverRatePercent(), EPSILON);
			} else if (bgo.getClass().equals(MutationOperator.class)) {
				assertEquals(expectedPopSize, ((MutationOperator) bgo).getMutationRate());
			}
		}

	}

}

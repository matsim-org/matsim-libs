/* *********************************************************************** *
 * project: org.matsim.*
 * PlanOptimizeTimesTest.java
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

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DoubleGene;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.router.util.TravelTime;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.misc.Time;

public class PlanOptimizeTimesTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(PlanOptimizeTimesTest.class);

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";

	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(PlanOptimizeTimesTest.CONFIGFILE);
	}

	public void testRun() {
		fail("Not yet implemented");
	}

	public void testEvolveAndReturnFittest() {
		fail("Not yet implemented");
	}

	public void testGetAverageFitness() {
		fail("Not yet implemented");
	}

	public void testWriteChromosome2Plan() {

		// writeChromosome2Plan() has 3 arguments:
		Plan testPlan = null;
		IChromosome testChromosome = null;
		LegTravelTimeEstimator ltte = null;
		
		// init test Plan
		final String TEST_PERSON_ID = "100";
		final int TEST_PLAN_NR = 0;

		NetworkLayer network = null;
		Population population = null;

		log.info("Reading network xml file...");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		population = new Population(Population.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");

		// first person
		Person testPerson = population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		// init IChromosome (from JGAP)
		int numActs = 0;
		for (Object o : testPlan.getActsLegs()) {

			if (o.getClass().equals(Act.class)) {
				((Act) o).setDur(Time.UNDEFINED_TIME);
				((Act) o).setEndTime(Time.UNDEFINED_TIME);
				numActs++;
			} else if (o.getClass().equals(Leg.class)) {
				((Leg) o).setTravTime(Time.UNDEFINED_TIME);
			}

		}
		// first and last activity are assumed to be the same
		numActs -= 1;		
		
		Configuration jgapConfiguration = new Configuration();

		try {
			Gene[] testGenes = new Gene[numActs];
			
			testGenes[0] = new DoubleGene(jgapConfiguration);
			testGenes[0].setAllele(Time.parseTime("07:45:00"));
			
			for (int ii=1; ii < testGenes.length; ii++) {
				testGenes[ii] = new DoubleGene(jgapConfiguration);
				switch(ii) {
				case 1:
					testGenes[ii].setAllele(Time.parseTime("07:59:59"));
					break;
				case 2:
					testGenes[ii].setAllele(Time.parseTime("16:00:01"));
					break;
				}
				
			}

			testChromosome = new Chromosome(jgapConfiguration, testGenes);
			
		} catch (InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// init LegTravelTimeEstimator
		TravelTime tTravelEstimator = new LinearInterpolatingTTCalculator(network, 900);
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(network, 900);
		ltte = new CharyparEtAlCompatibleLegTravelTimeEstimator(tTravelEstimator, depDelayCalc);
		
		// run the method
		PlanOptimizeTimes.writeChromosome2Plan(testChromosome, testPlan, ltte);
		
		// write out the test person and the modified plan into a file
		Population outputPopulation = new Population();
		try {
			outputPopulation.addPerson(testPerson);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(outputPopulation, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		System.out.println("Writing plans file...DONE.");
		
		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Expected checksum: " + Long.toString(expectedChecksum));
		log.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);

	}

}

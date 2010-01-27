/* *********************************************************************** *
 * project: org.matsim.*
 * PrimlocValidationTest.java
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

package org.matsim.demandmodeling.primloc;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.Knowledges;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

/**
 * Test case that uses the "primloc" module in a matsim setting.
 *
 * Note that it uses a "zone" layer, which is defined in "world.xml".
 *
 * @author fabrice and wisinee
 *
 */
public class PrimlocModuleTest extends MatsimTestCase{

	// FIXME this test-case has no Assert-statement, so it will always succeed!

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	Config config;

	private static final String inputfolder = "test/scenarios/triangle/" ;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig( "test/input/org/matsim/demandmodeling/primloc/config_primloc_triangle.xml");
		String outputDirectory = super.getOutputDirectory();

		this.config.config().setOutputFile(outputDirectory + "output_config.xml");

		this.config.world().setInputFile(inputfolder + "world.xml");
		this.config.world().setOutputFile(outputDirectory + "output_world.xml");

		this.config.network().setInputFile(inputfolder + "network.xml");
		this.config.network().setOutputFile(outputDirectory + "output_network.xml");

		this.config.facilities().setInputFile(inputfolder + "facilities.xml");
		this.config.facilities().setOutputFile(outputDirectory + "output_facilities.xml");

		this.config.plans().setInputFile(inputfolder + "init_plans.xml.gz" );
		this.config.plans().setOutputFile(outputDirectory + "output_plans.xml.gz");
		this.config.plans().setOutputVersion("v4");
		this.config.plans().setOutputSample(1.0);
	}

	@Override
	protected void tearDown() throws Exception {
		this.config = null;
		super.tearDown();
	}

	public void testModule() {

		System.out.println("TEST MODULE PRIMLOC:");

		ScenarioImpl scenario = new ScenarioImpl(this.config);

		// reading all available input

		System.out.println("  reading world xml file... ");
		World world = scenario.getWorld();
		new MatsimWorldReader(scenario).readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		PopulationImpl population = scenario.getPopulation();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		Knowledges knowledges = scenario.getKnowledges();
		MatsimPopulationReader populationReader = new MatsimPopulationReader(scenario);
		populationReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		// REAL STUFF HERE
		// ***************

		System.out.println("  ** running primary location choice module (PLCM)");
		PrimlocModule plcm = new PrimlocModule(scenario.getConfig(), knowledges);
		plcm.externalTripDist = CumulativeDistribution.readDistributionFromFile("test/input/org/matsim/demandmodeling/primloc/sample_dist.txt");
		plcm.setup( world, population );
		plcm.run(population);

		// ************


		// writing all available input

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(population, scenario.getNetwork()).writeFile(config.plans().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).writeFile(config.facilities().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing network xml file... ");
		NetworkWriter network_writer = new NetworkWriter(scenario.getNetwork());
		network_writer.writeFile(config.network().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.writeFile(config.world().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		new ConfigWriter(config).writeFile(config.config().getOutputFile());
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}
}

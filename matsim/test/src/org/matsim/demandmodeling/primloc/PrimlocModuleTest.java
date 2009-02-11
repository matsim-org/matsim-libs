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

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

public class PrimlocModuleTest extends MatsimTestCase{

	// FIXME this test-case has no Assert-statement, so it will always succeed!
	
	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	Config config;
	
	private static final String inputfolder = "test/scenarios/triangle/" ;
	
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig( "test/input/org/matsim/demandmodeling/primloc/config_primloc_triangle.xml");
		String outputDirectory = super.getOutputDirectory();
		
		config.config().setOutputFile(outputDirectory + "output_config.xml");
		
		config.world().setInputFile(inputfolder + "world.xml");
		config.world().setOutputFile(outputDirectory + "output_world.xml");
		
		config.network().setInputFile(inputfolder + "network.xml");
		config.network().setOutputFile(outputDirectory + "output_network.xml");
		
		config.facilities().setInputFile(inputfolder + "facilities.xml");
		config.facilities().setOutputFile(outputDirectory + "output_facilities.xml");
				
		config.plans().setInputFile(inputfolder + "init_plans.xml.gz" );
		config.plans().setOutputFile(outputDirectory + "output_plans.xml.gz");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);
	}
	
	public void testModule() {
		
		System.out.println("TEST MODULE PRIMLOC:");

		Config config = Gbl.getConfig();
		
		// reading all available input

		System.out.println("  reading world xml file... ");
		World world = Gbl.createWorld();
		new MatsimWorldReader(world).readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating network layer... ");
		NetworkLayer network = new NetworkLayer();
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		Population population = new Population();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		MatsimPopulationReader populationReader = new MatsimPopulationReader(population, network);
		populationReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		// REAL STUFF HERE
		// ***************
		
		System.out.println("  adding plan algorithms");
		System.out.println("  ** adding primary location choice module (PLCM)");
		PrimlocModule plcm = new PrimlocModule();
		plcm.externalTripDist = CumulativeDistribution.readDistributionFromFile("test/input/org/matsim/demandmodeling/primloc/sample_dist.txt");
		plcm.setup( population );
		population.addAlgorithm( plcm );
		System.out.println("  done.");

		System.out.println("  running plan algorithms");
		population.runAlgorithms();
		
		// ************
		
		
		// writing all available input

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(population).write();
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write();
		System.out.println("  done.");

		System.out.println("  writing network xml file... ");
		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(config);
		config_writer.write();
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}
}

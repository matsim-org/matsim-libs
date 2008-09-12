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

import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.PopulationReader;
import org.matsim.population.Population;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.config.Config;

public class PrimlocModuleTest extends MatsimTestCase{

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
		
		//config.matrices().setInputFile(studyfolder + "matrices.xml");
		//config.matrices().setOutputFile(outputDirectory + "output_matrices.xml");
		
		config.plans().setInputFile(inputfolder + "init_plans.xml.gz" );
		config.plans().setOutputFile(outputDirectory + "output_plans.xml.gz");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);
	}
	
	public void testModule() {
		
		System.out.println("TEST MODULE PRIMLOC:");

		// reading all available input

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating network layer... ");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		Population population = new Population();
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		MatsimPopulationReader populationReader = new MatsimPopulationReader(population);
		populationReader.readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");

		// REAL STUFF HERE
		// ***************
		
		System.out.println("  adding plan algorithms");
		System.out.println("  ** adding primary location choice");
		PrimlocModule plcm = new PrimlocModule();
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
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}
}

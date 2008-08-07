/* *********************************************************************** *
 * project: org.matsim.*
 * TriangleScenario.java
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

package org.matsim.examples;

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

public abstract class TriangleScenario {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	public static final long CHECKSUM_WORLD_EMPTY = 565556558;
	public static final long CHECKSUM_FACILITIES_ENRICHED = 1006843383;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String studyfolder = "test/scenarios/triangle/";
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	private TriangleScenario() {
	}

	//////////////////////////////////////////////////////////////////////
	// setup
	//////////////////////////////////////////////////////////////////////

	public static final void setUpScenarioConfig(final Config config, final String outputDirectory) {
		config.config().setOutputFile(outputDirectory + "output_config.xml");
		
		config.world().setInputFile(studyfolder + "world.xml");
		config.world().setOutputFile(outputDirectory + "output_world.xml");
		
		config.network().setInputFile(studyfolder + "network.xml");
		config.network().setOutputFile(outputDirectory + "output_network.xml");
		
		config.facilities().setInputFile(studyfolder + "facilities.xml");
		config.facilities().setOutputFile(outputDirectory + "output_facilities.xml");
		
		config.matrices().setInputFile(studyfolder + "matrices.xml");
		config.matrices().setOutputFile(outputDirectory + "output_matrices.xml");
		
		config.plans().setOutputFile(outputDirectory + "output_plans.xml.gz");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);
	}

	//////////////////////////////////////////////////////////////////////
	// write output
	//////////////////////////////////////////////////////////////////////

	public static final void writePlans(Population plans) {
		System.out.println("  writing plans xml file... ");
		PopulationWriter plans_writer = new PopulationWriter(plans);
		plans_writer.write();
		System.out.println("  done.");
	}

	public static final void writeWorld(final World world) {
		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.write();
		System.out.println("  done.");
	}

	public static final void writeFacilities(Facilities facilities) {
		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(facilities);
		facilities_writer.write();
		System.out.println("  done.");
	}

	public static final void writeMatrices() {
		System.out.println("  writing matrices xml file... ");
		MatricesWriter matrices_writer = new MatricesWriter(Matrices.getSingleton());
		matrices_writer.write();
		System.out.println("  done.");
	}

	public static final void writeNetwork(NetworkLayer network) {
		System.out.println("  writing network xml file... ");
		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");
	}

	public static final void writeConfig(final Config config) {
		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(config);
		config_writer.write();
		System.out.println("  done.");
	}
}

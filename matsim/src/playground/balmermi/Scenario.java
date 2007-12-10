/* *********************************************************************** *
 * project: org.matsim.*
 * Scenario.java
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

package playground.balmermi;

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.world.WorldWriter;

public abstract class Scenario {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String output_directory = "output/";
	private static final String input_directory = "input/";

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	private Scenario() {
	}

	//////////////////////////////////////////////////////////////////////
	// setup
	//////////////////////////////////////////////////////////////////////

	public static final void setUpScenarioConfig() {
		final Config config = Gbl.createConfig(null);

		config.config().setOutputFile(output_directory + "output_config.xml");

		config.world().setInputFile(input_directory + "world.xml");
		config.world().setOutputFile(output_directory + "output_world.xml");

		config.network().setInputFile(input_directory + "network.xml.gz");
		config.network().setOutputFile(output_directory + "output_network.xml.gz");

		config.facilities().setInputFile(input_directory + "facilities.xml");
		config.facilities().setOutputFile(output_directory + "output_facilities.xml");

		config.matrices().setInputFile(input_directory + "matrices.xml");
		config.matrices().setOutputFile(output_directory + "output_matrices.xml");

		config.plans().setOutputFile(output_directory + "output_plans.xml.gz");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);
	}

	//////////////////////////////////////////////////////////////////////
	// write output
	//////////////////////////////////////////////////////////////////////

	public static final void writePlans(Plans plans) {
		System.out.println("  writing plans xml file... ");
		PlansWriter plans_writer = new PlansWriter(plans);
		plans_writer.write();
		System.out.println("  done.");
	}

	public static final void writeWorld() {
		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
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

	public static final void writeConfig() {
		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");
	}
}

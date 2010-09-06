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

package playground.ciarif;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

public abstract class Scenario {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	public static final String output_directory = "output/";
	public static final String input_directory = "input/";

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

	public static final Config setUpScenarioConfig() {
		final Config config = Gbl.createConfig(null);

		config.network().setInputFile(input_directory + "network.xml.gz");

		config.facilities().setInputFile(input_directory + "facilities.xml");
//		config.facilities().setOutputFile(output_directory + "output_facilities.xml");

		config.plans().setInputFile(input_directory + "plans.xml");
//		config.plans().setOutputFile(output_directory + "output_plans.xml");

		config.counts().setCountsFileName(input_directory + "counts.xml");

		return config;
	}

	//////////////////////////////////////////////////////////////////////
	// read input
	//////////////////////////////////////////////////////////////////////

//	public static final World readWorld() {
//		System.out.println("  reading world xml file... ");
//		new MatsimWorldReader(Gbl.getWorld()).readFile(Gbl.getConfig().world().getInputFile());
//		System.out.println("  done.");
//		return Gbl.getWorld();
//	}
//
//	public static final Facilities readFacilities() {
//		System.out.println("  reading facilities xml file... ");
//		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
//		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
//		System.out.println("  done.");
//		return facilities;
//	}
//
//	public static final NetworkLayer readNetwork() {
//		System.out.println("  reading the network xml file...");
//		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
//		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
//		System.out.println("  done.");
//		return network;
//	}
//
//	public static final Counts readCounts() {
//		System.out.println("  reading the counts...");
//		final Counts counts = new Counts();
//		new MatsimCountsReader(counts).readFile(Gbl.getConfig().counts().getCountsFileName());
//		System.out.println("  done.");
//		return counts;
//	}
//
//	public static final Matrices readMatrices() {
//		System.out.println("  reading matrices xml file... ");
//		new MatsimMatricesReader(Matrices.getSingleton(), Gbl.getWorld()).readFile(Gbl.getConfig().matrices().getInputFile());
//		System.out.println("  done.");
//		return Matrices.getSingleton();
//	}
//
//	public static final Population readPlans() {
//		System.out.println("  reding plans xml file... ");
//		Population plans = new PopulationImpl();
//		new MatsimPopulationReader(plans).readFile(Gbl.getConfig().plans().getInputFile());
//		System.out.println("  done.");
//		return plans;
//	}

	//////////////////////////////////////////////////////////////////////
	// write output
	//////////////////////////////////////////////////////////////////////

//	public static final void writePlans(final Population plans) {
//		System.out.println("  writing plans xml file... ");
//		new PopulationWriter(plans).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeMatrices(final Matrices matrices) {
//		System.out.println("  writing matrices xml file... ");
//		new MatricesWriter(matrices).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeCounts(final Counts counts) {
//		System.out.println("  writing counts xml file... ");
//		new CountsWriter(counts).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeNetwork(final NetworkLayer network) {
//		System.out.println("  writing network xml file... ");
//		new NetworkWriter(network).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeFacilities(final Facilities facilities) {
//		System.out.println("  writing facilities xml file... ");
//		new FacilitiesWriter(facilities).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeWorld(final World world) {
//		System.out.println("  writing world xml file... ");
//		new WorldWriter(world).write();
//		System.out.println("  done.");
//	}
//
//	public static final void writeConfig() {
//		System.out.println("  writing config xml file... ");
//		new ConfigWriter(Gbl.getConfig()).write();
//		System.out.println("  done.");
//	}
}

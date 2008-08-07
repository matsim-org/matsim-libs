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
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

public abstract class Scenario {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	public static String output_directory = "output/";
	public static String input_directory = "input/";

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

		config.plans().setInputFile(input_directory + "plans.xml");
		config.plans().setOutputFile(output_directory + "output_plans.xml");
		config.plans().setOutputVersion("v4");
		config.plans().setOutputSample(1.0);
		
		config.counts().setCountsFileName(input_directory + "counts.xml");
		config.counts().setOutputFile(output_directory + "output_counts.xml.gz");
	}

	//////////////////////////////////////////////////////////////////////
	// read input
	//////////////////////////////////////////////////////////////////////

	public static final World readWorld() {
		System.out.println("  reading world xml file... ");
		new MatsimWorldReader(Gbl.getWorld()).readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");
		return Gbl.getWorld();
	}

	public static final Facilities readFacilities() {
		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");
		return facilities;
	}
	
	public static final NetworkLayer readNetwork() {
		System.out.println("  reading the network xml file...");
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");
		return network;
	}
	
	public static final Counts readCounts() {
		System.out.println("  reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(Gbl.getConfig().counts().getCountsFileName());
		System.out.println("  done.");
		return counts;
	}
	
	public static final Matrices readMatrices() {
		System.out.println("  reading matrices xml file... ");
		new MatsimMatricesReader(Matrices.getSingleton()).readFile(Gbl.getConfig().matrices().getInputFile());
		System.out.println("  done.");
		return Matrices.getSingleton();
	}
	
	public static final Plans readPlans() {
		System.out.println("  reding plans xml file... ");
		Plans plans = new Plans();
		new MatsimPlansReader(plans).readFile(Gbl.getConfig().plans().getInputFile());
		System.out.println("  done.");
		return plans;
	}
	
	//////////////////////////////////////////////////////////////////////
	// write output
	//////////////////////////////////////////////////////////////////////

	public static final void writePlans(Plans plans) {
		System.out.println("  writing plans xml file... ");
		new PlansWriter(plans).write();
		System.out.println("  done.");
	}

	public static final void writeMatrices(Matrices matrices) {
		System.out.println("  writing matrices xml file... ");
		new MatricesWriter(matrices).write();
		System.out.println("  done.");
	}

	public static final void writeCounts(Counts counts) {
		System.out.println("  writing counts xml file... ");
		new CountsWriter(counts).write();
		System.out.println("  done.");
	}

	public static final void writeNetwork(NetworkLayer network) {
		System.out.println("  writing network xml file... ");
		new NetworkWriter(network).write();
		System.out.println("  done.");
	}
	
	public static final void writeFacilities(Facilities facilities) {
		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write();
		System.out.println("  done.");
	}

	public static final void writeWorld(World world) {
		System.out.println("  writing world xml file... ");
		new WorldWriter(world).write();
		System.out.println("  done.");
	}

	public static final void writeConfig() {
		System.out.println("  writing config xml file... ");
		new ConfigWriter(Gbl.getConfig()).write();
		System.out.println("  done.");
	}
}

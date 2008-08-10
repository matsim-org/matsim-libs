/* *********************************************************************** *
 * project: org.matsim.*
 * TriangleTest.java
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
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.algorithms.FacilitiesDefineCapAndOpentime;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PersonCreatePlanFromKnowledge;
import org.matsim.population.algorithms.PlansCreateFromNetwork;
import org.matsim.population.algorithms.PlansDefineKnowledge;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class TriangleTest extends MatsimTestCase {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private Config config = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public TriangleTest() {
	}

	//////////////////////////////////////////////////////////////////////
	// setup
	//////////////////////////////////////////////////////////////////////

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(null);
		TriangleScenario.setUpScenarioConfig(this.config, super.getOutputDirectory());
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void compareOutputWorld() {
		System.out.println("  comparing input and output world file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.world().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.world().getOutputFile());
		assertEquals("different world files", checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void checkEnrichedOutputFacilities() {
		System.out.println("  checksum check of enriched output facilities... ");
		long checksum_facilities = CRCChecksum.getCRCFromFile(this.config.facilities().getOutputFile());
		assertEquals("different facilities files", TriangleScenario.CHECKSUM_FACILITIES_ENRICHED,checksum_facilities);
		System.out.println("  done.");
	}

	private final void compareOutputNetwork() {
		System.out.println("  comparing input and output network file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.network().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.network().getOutputFile());
		assertEquals("different network files", checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	private final void compareOutputPlans() {
		System.out.println("  comparing reference and output plans file... ");
		long checksum_ref = CRCChecksum.getCRCFromGZFile(getInputDirectory() + "plans.xml.gz");
		long checksum_run = CRCChecksum.getCRCFromGZFile(this.config.plans().getOutputFile());
		assertEquals("different plans files", checksum_ref, checksum_run);
		System.out.println("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testInitDemand() {

		System.out.println("running testParserWriter1()...");

		final World world = Gbl.createWorld();

		System.out.println("  reading world xml file... ");
		new MatsimWorldReader(world).readFile(this.config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilites xml file as a layer of the world...");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(this.config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		System.out.println("  done.");

		System.out.println();
		System.out.println("1. VALIDATE AND COMPLETE THE WORLD");
		System.out.println();

		System.out.println("  running world modules... ");
		new WorldCheck().run(world);
		new WorldBottom2TopCompletion().run(world);
		new WorldValidation().run(world);
		new WorldCheck().run(world);
		System.out.println("  done.");

		System.out.println();
		System.out.println("2. SUMMARY INFORMATION OF THE NETWORK");
		System.out.println();

		System.out.println("  running network modules... ");
		NetworkSummary ns_algo = new NetworkSummary();
		ns_algo.run(network);
		new NetworkCalcTopoType().run(network);
		System.out.println("  done.");

		System.out.println();
		System.out.println("3. CREATING A POPULATION BASED ON THE NETWORK");
		System.out.println();

		System.out.println("  creating plans object... ");
		Population plans = new Population(false);
		System.out.println("  done.");

		System.out.println("  running plans modules... ");
		new PlansCreateFromNetwork(network,ns_algo,2.0).run(plans);
		System.out.println("  done.");

		System.out.println();
		System.out.println("DEPRECATED: 4. aggregation of facilities! THAT DOES NOT EXIST ANYMORE!");
		System.out.println();

		System.out.println();
		System.out.println("5. DEFINE CAPACITIES AND OPENTIMES FOR THE FACILITIES BASED ON THE POPULATION");
		System.out.println();

		System.out.println("  running facilities algorithms... ");
		new FacilitiesDefineCapAndOpentime(plans.getPersons().size()).run(facilities);
		System.out.println("  done.");

		System.out.println();
		System.out.println("6. DEFINE SOME KNOWLEDGE FOR THE POPULATION");
		System.out.println();

		System.out.println("  running plans algorithms... ");
		new PlansDefineKnowledge(facilities).run(plans);
		System.out.println("  done.");

		System.out.println();
		System.out.println("7. CREATE AN INITIAL DAYPLAN FOR EACH PERSON ACCORDING TO THEIR KNOWLEDGE");
		System.out.println();

		System.out.println("  running plans algorithms... ");
		new PersonCreatePlanFromKnowledge().run(plans);
		System.out.println("  done.");

		System.out.println();
		System.out.println("8. WRITING DOWN ALL DATA");
		System.out.println();

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans).write();
		System.out.println("  done.");

		System.out.println("  writing network xml file... ");
		new NetworkWriter(network).write();
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		new WorldWriter(world).write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		new ConfigWriter(this.config).write();
		System.out.println("  done.");

		this.compareOutputPlans();
		this.compareOutputNetwork();
		this.checkEnrichedOutputFacilities();
		this.compareOutputWorld();

		System.out.println("done.");
	}
}

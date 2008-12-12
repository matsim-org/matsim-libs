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

import org.apache.log4j.Logger;
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
import org.matsim.world.algorithms.WorldMappingInfo;

public class TriangleTest extends MatsimTestCase {

	private Config config = null;
	private final static Logger log = Logger.getLogger(TriangleTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(null);
		TriangleScenario.setUpScenarioConfig(this.config, super.getOutputDirectory());
	}

	@Override
	protected void tearDown() throws Exception {
		this.config = null;
		super.tearDown();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void compareOutputWorld() {
		log.info("  comparing input and output world file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.world().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.world().getOutputFile());
		assertEquals("different world files", checksum_ref, checksum_run);
		log.info("  done.");
	}

	private final void checkEnrichedOutputFacilities() {
		log.info("  checksum check of enriched output facilities... ");
		long checksum_facilities = CRCChecksum.getCRCFromFile(this.config.facilities().getOutputFile());
		assertEquals("different facilities files", TriangleScenario.CHECKSUM_FACILITIES_ENRICHED,checksum_facilities);
		log.info("  done.");
	}

	private final void compareOutputNetwork() {
		log.info("  comparing input and output network file... ");
		long checksum_ref = CRCChecksum.getCRCFromFile(this.config.network().getInputFile());
		long checksum_run = CRCChecksum.getCRCFromFile(this.config.network().getOutputFile());
		assertEquals("different network files", checksum_ref, checksum_run);
		log.info("  done.");
	}

	private final void compareOutputPlans() {
		log.info("  comparing reference and output plans file... ");
		long checksum_ref = CRCChecksum.getCRCFromGZFile(getInputDirectory() + "plans.xml.gz");
		long checksum_run = CRCChecksum.getCRCFromGZFile(this.config.plans().getOutputFile());
		assertEquals("different plans files", checksum_ref, checksum_run);
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testInitDemand() {

		log.info("running testParserWriter1()...");

		final World world = Gbl.createWorld();

		log.info("  reading world xml file... ");
		new MatsimWorldReader(world).readFile(this.config.world().getInputFile());
		log.info("  done.");

		log.info("  reading facilites xml file as a layer of the world...");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(this.config.facilities().getInputFile());
		log.info("  done.");

		log.info("  reading network xml file... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		log.info("  done.");

		log.info("\n");
		log.info("1. VALIDATE AND COMPLETE THE WORLD");
		log.info("\n");

		log.info("  running world modules... ");
		new WorldCheck().run(world);
		new WorldBottom2TopCompletion().run(world);
		new WorldMappingInfo().run(world);
		new WorldCheck().run(world);
		log.info("  done.");

		log.info("\n");
		log.info("2. SUMMARY INFORMATION OF THE NETWORK");
		log.info("\n");

		log.info("  running network modules... ");
		NetworkSummary ns_algo = new NetworkSummary();
		ns_algo.run(network);
		new NetworkCalcTopoType().run(network);
		log.info("  done.");

		log.info("\n");
		log.info("3. CREATING A POPULATION BASED ON THE NETWORK");
		log.info("\n");

		log.info("  creating plans object... ");
		Population plans = new Population(false);
		log.info("  done.");

		log.info("  running plans modules... ");
		new PlansCreateFromNetwork(network,ns_algo,2.0).run(plans);
		log.info("  done.");

		log.info("\n");
		log.info("4. DEFINE CAPACITIES AND OPENTIMES FOR THE FACILITIES BASED ON THE POPULATION");
		log.info("\n");

		log.info("  running facilities algorithms... ");
		new FacilitiesDefineCapAndOpentime(plans.getPersons().size()).run(facilities);
		log.info("  done.");

		log.info("\n");
		log.info("5. DEFINE SOME KNOWLEDGE FOR THE POPULATION");
		log.info("\n");

		log.info("  running plans algorithms... ");
		new PlansDefineKnowledge(facilities).run(plans);
		log.info("  done.");

		log.info("\n");
		log.info("6. CREATE AN INITIAL DAYPLAN FOR EACH PERSON ACCORDING TO THEIR KNOWLEDGE");
		log.info("\n");

		log.info("  running plans algorithms... ");
		new PersonCreatePlanFromKnowledge().run(plans);
		log.info("  done.");

		log.info("\n");
		log.info("7. WRITING DOWN ALL DATA");
		log.info("\n");

		log.info("  writing plans xml file... ");
		new PopulationWriter(plans).write();
		log.info("  done.");

		log.info("  writing network xml file... ");
		new NetworkWriter(network).write();
		log.info("  done.");

		log.info("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write();
		log.info("  done.");

		log.info("  writing world xml file... ");
		new WorldWriter(world).write();
		log.info("  done.");

		log.info("  writing config xml file... ");
		new ConfigWriter(this.config).write();
		log.info("  done.");

		this.compareOutputNetwork();
		this.checkEnrichedOutputFacilities();
		this.compareOutputWorld();
		this.compareOutputPlans();

		log.info("done.");
	}
}

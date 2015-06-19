/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkParserWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.pt;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.examples.TriangleScenario;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.testcases.MatsimTestCase;

/**
 *
 * @author cdobler
 */
public class TransitRouterNetworkReaderWriterTest extends MatsimTestCase {

	private Config config = null;
	private static final Logger log = Logger.getLogger(TransitRouterNetworkReaderWriterTest.class);

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
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testWriteReadTransitRouterNetwork() {
		Fixture fixture = new Fixture();
		fixture.init();
		
		TransitRouterConfig config = new TransitRouterConfig(fixture.scenario.getConfig().planCalcScore(),
				fixture.scenario.getConfig().plansCalcRoute(), fixture.scenario.getConfig().transitRouter(),
				fixture.scenario.getConfig().vspExperimental());
	
		log.info("Writing transit router network to file...");
		TransitRouterNetwork transitRouterNetwork = TransitRouterNetwork.createFromSchedule(fixture.schedule, config.getBeelineWalkConnectionDistance());
		new TransitRouterNetworkWriter(transitRouterNetwork).write(getOutputDirectory() + "output_network.xml");
		log.info("done.");
		
		log.info("Reading transit router network from file...");
		transitRouterNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReaderMatsimV1(fixture.scenario, transitRouterNetwork).parse(getOutputDirectory() + "output_network.xml");
		log.info("done.");
	}
	
}

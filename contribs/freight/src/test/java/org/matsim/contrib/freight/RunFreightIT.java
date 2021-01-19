/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight;

import org.junit.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.net.URL;
import java.util.concurrent.ExecutionException;

public class RunFreightIT {

	private static final URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
	private Scenario scenario;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void setUp() {
		//prepare config
		Config config = ConfigUtils.createConfig();

		config.global().setRandomSeed(4177 );
		config.controler().setLastIteration(0 );
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.network().setInputFile(IOUtils.extendUrl(scenarioUrl, "grid9x9.xml").getPath());

		//freight settings
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class ) ;
		freightConfigGroup.setCarriersFile(IOUtils.extendUrl(scenarioUrl, "singleCarrierFiveActivitiesWithoutRoutes.xml").getPath());
		freightConfigGroup.setCarriersVehicleTypesFile(IOUtils.extendUrl(scenarioUrl,  "vehicleTypes.xml").getPath());

		scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		FreightUtils.loadCarriersAccordingToFreightConfig( scenario );

		try {
			FreightUtils.runJsprit( scenario, ConfigUtils.addOrGetModule( scenario.getConfig(), FreightConfigGroup.class ) );
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Compare the resulting eventsFile with an already existing one.
	 * This is not the best test, but better then noting
	 */
	@Test
	public void testCompareEvents(){
				final Controler controler = new Controler( scenario ) ;
		Freight.configure( controler );
		controler.run();

		String eventsFileReference = testUtils.getClassInputDirectory() + "output_events.xml.gz";
		String eventsFileOutput = testUtils.getOutputDirectory() + "output_events.xml.gz";
		EventsFileComparator.Result result = EventsFileComparator.compare(eventsFileReference, eventsFileOutput);
		Assert.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);
	}

	/**
	 * Test if the different tours of an carrier have a unique vehicleId.
	 * This was _not_ before: It only has bin the vehicleId, which can be used more than one times, if there is an INfinite fleet.
	 * Later in the Agent creation it gets unified to have unique agents in unique vehicles.
	 * Now it should get unified already during the reconverting from jsprit to MATSim.
	 */
	@Ignore
	@Test
	public void testCarrierToursHasUniqueVehicleIds(){
		//TODO
	}

}

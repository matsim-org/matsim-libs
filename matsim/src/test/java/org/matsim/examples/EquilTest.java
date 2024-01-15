/* *********************************************************************** *
 * project: org.matsim.*
 * EquilTest.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class EquilTest  {
	private static final Logger log = LogManager.getLogger( EquilTest.class ) ;

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testEquil(boolean isUsingFastCapacityUpdate) {
		Config config = ConfigUtils.createConfig() ;
		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);
		config.facilities().setFacilitiesSource( FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile );

		String netFileName = "test/scenarios/equil/network.xml";
		String popFileName = "test/scenarios/equil/plans100.xml";

		System.out.println( utils.getInputDirectory() );
		String referenceFileName = utils.getInputDirectory() + "events.xml.gz";

		String eventsFileName = utils.getOutputDirectory() + "events.xml.gz";

		MutableScenario scenario = ScenarioUtils.createMutableScenario(config );

		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		MatsimReader plansReader = new PopulationReader(scenario);
		plansReader.readFile(popFileName);

		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML(eventsFileName);
		events.addHandler(writer);

		//		SimulationTimer.setTime(0); // I don't think this is needed. kai, may'10
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();

		writer.closeFile();

		final EventsFileComparator.Result result = new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( referenceFileName , eventsFileName );
		Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result, "different event files." );
	}
}

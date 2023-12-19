/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTestFourWay
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.signals.builder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.*;

/**
 * @author aneumann
 * @author dgrether
 */
public class TravelTimeFourWaysTest {

	private static final String EVENTSFILE = "events.xml.gz";

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testTrafficLightIntersection4arms() {
		Scenario scenario = this.createTestScenario();
		scenario.getConfig().plans().setInputFile("plans.xml.gz");
		ScenarioUtils.loadScenario(scenario);

		runQSimWithSignals(scenario);
	}

	@Test
	void testTrafficLightIntersection4armsWithUTurn() {
		Scenario scenario = this.createTestScenario();
		scenario.getConfig().plans().setInputFile("plans_uturn.xml.gz");
		ScenarioUtils.loadScenario(scenario);

		runQSimWithSignals(scenario);
	}

	private Scenario createTestScenario(){
		Config conf = ConfigUtils.createConfig(testUtils.classInputResourcePath());
		conf.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		conf.controller().setMobsim("qsim");
		conf.network().setInputFile("network.xml.gz");
		conf.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
		conf.qsim().setUseLanes(true);
	    conf.qsim().setUsingFastCapacityUpdate(false);

		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(conf, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class );
		signalsConfig.setUseSignalSystems(true);
		signalsConfig.setSignalSystemFile("testSignalSystems_v2.0.xml");
		signalsConfig.setSignalGroupsFile("testSignalGroups_v2.0.xml");
		signalsConfig.setSignalControlFile("testSignalControl_v2.0.xml");
		signalsConfig.setUseAmbertimes(true);
		signalsConfig.setAmberTimesFile("testAmberTimes_v1.0.xml");

		Scenario scenario = ScenarioUtils.createScenario(conf);
		scenario.addScenarioElement( SignalsData.ELEMENT_NAME , new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		return scenario;
	}

	private void runQSimWithSignals(final Scenario scenario) {
//		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), AbstractModule.override(Collections.singleton(new AbstractModule() {
//			@Override
//			public void install() {
//				// defaults
//				install(new NewControlerModule());
//				install(new ControlerDefaultCoreListenersModule());
//				install(new ControlerDefaultsModule());
//				install(new ScenarioByInstanceModule(scenario));
//			}
//		}), new SignalsModule()));

//		EventsManager events = injector.getInstance(EventsManager.class);
//		events.initProcessing();

		EventsManager events = EventsUtils.createEventsManager() ;

		String eventsOut = this.testUtils.getOutputDirectory() + EVENTSFILE;
		EventWriterXML eventsXmlWriter = new EventWriterXML(eventsOut);
		events.addHandler(eventsXmlWriter);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
//		Mobsim mobsim = injector.getInstance(Mobsim.class);
//		mobsim.run();

		new QSimBuilder( scenario.getConfig() )
				.useDefaults()
				.addOverridingModule( new SignalsModule() )
				.addOverridingQSimModule( new SignalsQSimModule() )
				.build( scenario, events ).run();

		eventsXmlWriter.closeFile();
//	    Assert.assertEquals("different events files", EventsFileComparator.compareAndReturnInt(this.testUtils.getInputDirectory() + EVENTSFILE, eventsOut), 0);
		Assertions.assertEquals( Result.FILES_ARE_EQUAL, new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( this.testUtils.getInputDirectory() + EVENTSFILE, eventsOut ) );
	}

}

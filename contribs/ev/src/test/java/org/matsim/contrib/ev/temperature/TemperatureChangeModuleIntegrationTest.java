/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.temperature;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/*
 * created by jbischoff, 16.08.2018
 */
public class TemperatureChangeModuleIntegrationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTemperatureChangeModule() {

		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "/config.xml",
				new TemperatureChangeConfigGroup());
		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TemperatureChangeModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TemperatureTestEventHandler.class).asEagerSingleton();
			}
		});
		controler.run();
	}

	static class TemperatureTestEventHandler implements PersonDepartureEventHandler {
		@Inject
		TemperatureService temperatureService;

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLinkId().equals(Id.createLinkId("link1"))) {
				Assertions.assertEquals(temperatureService.getCurrentTemperature(event.getLinkId()), -10.0, 0.001);
			}
			if (event.getLinkId().equals(Id.createLinkId("link2"))) {
				Assertions.assertEquals(temperatureService.getCurrentTemperature(event.getLinkId()), 30.0, 0.001);
			}
		}
	}

}


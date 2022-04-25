
/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEventsIntegrationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleUtils;

public class QSimEventsIntegrationTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Rule
	public Timeout globalTimeout = new Timeout(20000);

	@Test
	public void netsimEngineHandlesExceptionCorrectly() {
		Config config = utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();

		var vehicleId = Id.createVehicleId("v1");
		VehicleUtils.insertVehicleIdsIntoAttributes(scenario.getPopulation().getPersons().get(Id.createPersonId("1")),
				Map.of(TransportMode.car, vehicleId));
		scenario.getVehicles().addVehicleType(VehicleUtils.getDefaultVehicleType());
		scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(vehicleId, VehicleUtils.getDefaultVehicleType()));

		events.addHandler((LinkLeaveEventHandler)event -> {
			throw new RuntimeException("Haha, I hope the QSim exits cleanly.");
		});
		try {
			new QSimBuilder(config).useDefaults().build(scenario, events).run();
		} catch (RuntimeException e) {
			// That's fine. Only timeout is bad, which would mean qsim would hang on an Exception in an EventHandler.
			Assert.assertEquals("Haha, I hope the QSim exits cleanly.", e.getCause().getMessage());
		}
	}

	@Test
	public void controlerHandlesExceptionCorrectly() {
		Config config = utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);

		Controler controler = new Controler(config);
		controler.getEvents().addHandler((LinkLeaveEventHandler)event -> {
			throw new RuntimeException("Haha, I hope the QSim exits cleanly.");
		});
		try {
			controler.run();
		} catch (RuntimeException e) {
			// That's fine. Only timeout is bad, which would mean qsim would hang on an Exception in an EventHandler.
			Assert.assertEquals("Haha, I hope the QSim exits cleanly.", e.getMessage());
		}
	}

}

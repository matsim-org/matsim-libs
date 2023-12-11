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

package org.matsim.contrib.av.flow;
/*
 * created by jbischoff, 18.03.2019
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class TestAvFlowFactor {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testAvFlowFactor() throws MalformedURLException {
		URL configUrl = new File(utils.getPackageInputDirectory() + "config.xml").toURI().toURL();
		Config config = ConfigUtils.loadConfig(configUrl, new OTFVisConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		RunAvExample.addPopulation(scenario);

		VehicleType avType = VehicleUtils.createVehicleType(Id.create("autonomousVehicleType", VehicleType.class ) );
		avType.setFlowEfficiencyFactor(2.0);
		scenario.getVehicles().addVehicleType(avType);

		for (int i = 0; i < 192; i++) {
			//agents on lower route get AVs as vehicles, agents on upper route keep a standard vehicle (= default, if nothing is set)
			Id<Vehicle> vid = Id.createVehicleId("lower_" + i);
			Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, avType);
			scenario.getVehicles().addVehicle(v);
		}

		Controler controler = new Controler(scenario);
		VehicleTimeCounter vehicleTimeCounter = new VehicleTimeCounter();
		controler.getEvents().addHandler(vehicleTimeCounter);
		controler.run();

		Assertions.assertEquals(vehicleTimeCounter.lastAVEnterTime, 32598, 0.1);
		Assertions.assertEquals(vehicleTimeCounter.lastNonAVEnterTime, 36179, 0.1);

	}

	static class VehicleTimeCounter implements LinkEnterEventHandler {
		double lastNonAVEnterTime;
		double lastAVEnterTime;

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId().equals(Id.createLinkId(152))) {
				lastNonAVEnterTime = event.getTime();
			}
			if (event.getLinkId().equals(Id.createLinkId(131))) {
				lastAVEnterTime = event.getTime();
			}
		}
	}
}

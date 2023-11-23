/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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


package org.matsim.contrib.parking.parkingcost;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingcost.config.ParkingCostConfigGroup;
import org.matsim.contrib.parking.parkingcost.module.ParkingCostModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * An example class that shows how parking costs can be added to a scenario.
 */
public final class RunParkingCostExample {
	private RunParkingCostExample() {
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("parkingcosts/config.xml", new ParkingCostConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		addParkingCostToLinks(scenario.getNetwork());
		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new ParkingCostModule());

		ParkingCostTracker parkingCostTracker = new ParkingCostTracker();
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(parkingCostTracker);
			}
		});
		controller.run();

		LogManager.getLogger(RunParkingCostExample.class).info("Parking Cost charged in total: " + parkingCostTracker.parkingCostCollected);

	}

	private static void addParkingCostToLinks(Network network) {
		network.getLinks().values().forEach(link -> {
			link.getAttributes().putAttribute("pc_car", 2.50);
		});
	}

	static class ParkingCostTracker implements PersonMoneyEventHandler {
		double parkingCostCollected = 0.0;

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			if (event.getPurpose() != null) {
				if (event.getPurpose().endsWith("parking cost")) {
					parkingCostCollected += event.getAmount();
				}
			}
		}
	}
}

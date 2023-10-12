/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.parking.parkingproxy.ParkingProxyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.parking.parkingproxy.config.ParkingProxyConfigGroup;

public class RunWithParkingProxy {

	public static void main(String[] args) {

		ParkingProxyConfigGroup parkingConfig = new ParkingProxyConfigGroup();
		Config config = ConfigUtils.loadConfig(args, parkingConfig);

		//config.controler().setLastIteration(100);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);
		config.scoring().setWriteExperiencedPlans(true);

		Scenario scen = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scen);

		controler.addOverridingModule(new ParkingProxyModule(scen) );

		controler.run();
	}

}

/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.roadpricing.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;

import java.net.URL;

/**
 * Illustrative example how to first read a "base" toll file, and then make it dependent on the vehicle type.
 * <br/><br/>
 * Not tested.
 *
 * @author nagel
 */
public class RunRoadPricingUsingTollFactorExample {
	private static final String TEST_CONFIG = "./contribs/roadpricing/test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	public static void main(String[] args) {

		// load the config from file:
		Config config;
		if (args.length > 0) {
			config = ConfigUtils.loadConfig(args[0], RoadPricingUtils.createConfigGroup());
		} else {
			config = ConfigUtils.loadConfig(TEST_CONFIG, RoadPricingUtils.createConfigGroup());
		}
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		// "materialize" the road pricing config group:
		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class);

		// load the scenario from file.  This will _not_ load the road pricing file, since it is not part of the main distribution.
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// define the toll factor as an anonymous class.  If more flexibility is needed, convert to "full" class.
		TollFactor tollFactor = (personId, vehicleId, linkId, time) -> {
			VehicleType someVehicleType = null; // --> replace by something meaningful <--
			//noinspection ConstantConditions
			if (scenario.getVehicles().getVehicles().get(vehicleId).getType().equals(someVehicleType)) {
				return 2;
			} else {
				return 1;
			}
		};

		// instantiate the road pricing scheme, with the toll factor inserted:
		URL roadpricingUrl = IOUtils.newUrl(config.getContext(), rpConfig.getTollLinksFile());
		RoadPricingSchemeUsingTollFactor.createAndRegisterRoadPricingSchemeUsingTollFactor(roadpricingUrl.getFile(), tollFactor, scenario );


		// instantiate the control(l)er:
		Controler controler = new Controler(scenario);

		// add the road pricing module, with our scheme from above inserted:
		controler.addOverridingModule( new RoadPricingModule() );

		// run everything:
		controler.run();
	}

}

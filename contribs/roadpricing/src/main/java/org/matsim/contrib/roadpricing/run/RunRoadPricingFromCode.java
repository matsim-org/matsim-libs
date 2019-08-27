/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.roadpricing.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Basic 'script' to run road pricing. This example shows how to, programmatically,
 * configure the roadpricing scheme instead of reading it from an XML file.
 */
public class RunRoadPricingFromCode {
	private static final String TEST_CONFIG = "./contribs/roadpricing/test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	public static void main(String[] args) { run(args); }

	private static void run(String[] args){
		if(args.length==0){ args = new String[]{TEST_CONFIG}; }

		/* Start with a known config file (with population, network, and scoring
		parameteres specified) and just remove the road pricing file. */
		Config config = ConfigUtils.loadConfig(args[0], RoadPricingUtils.createConfigGroup());
		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class).setTollLinksFile(null);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(10);

		// prepare scenario:

		Scenario sc = ScenarioUtils.loadScenario(config);

		createCustomRoadPricingScheme(sc);

		// prepare controler:

		Controler controler = new Controler(sc);

		controler.addOverridingModule( new RoadPricingModule() );

		// run controler:

		controler.run();
	}


	private static void createCustomRoadPricingScheme( Scenario scenario){
		RoadPricingSchemeImpl scheme1 = RoadPricingUtils.createAndRegisterMutableScheme(scenario );

		/* Configure roadpricing scheme. */
		RoadPricingUtils.setName(scheme1, "custom");
		RoadPricingUtils.setType(scheme1, RoadPricingScheme.TOLL_TYPE_LINK);
		RoadPricingUtils.setDescription(scheme1, "Custom coded road pricing scheme");

		/* Add the link-specific toll. */
		RoadPricingUtils.addLinkSpecificCost(scheme1,
				Id.createLinkId("link_4_5"),
				Time.parseTime("00:00:00"),
				Time.parseTime("30:00:00"),
				100.0);

		/* Add general toll. */
		RoadPricingUtils.addLink(scheme1, Id.createLinkId("link_1_2"));
		RoadPricingUtils.addLink(scheme1, Id.createLinkId("link_2_1"));
		RoadPricingUtils.createAndAddGeneralCost(scheme1,
				Time.parseTime("06:00:00"),
				Time.parseTime("10:00:00"),
				10.0);
	}
}

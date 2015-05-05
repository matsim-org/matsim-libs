/* *********************************************************************** *
 * project: org.matsim.*
 * Run2030.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.matsim2030;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;

import playground.christoph.evacuation.pt.TransitRouterImplFactory;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;

public class Run2030 {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("/data/matsim/cdobler/2030/config.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		final TransitRouterNetwork routerNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("/data/matsim/cdobler/2030/transitRouterNetwork_thinned.xml.gz");
		
		final TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork));
			}
		});
		controler.run();
	}
}

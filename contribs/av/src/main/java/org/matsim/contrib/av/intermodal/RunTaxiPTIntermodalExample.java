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

/**
 *
 */
package org.matsim.contrib.av.intermodal;

import static org.matsim.contrib.drt.run.DrtControlerCreator.createScenarioWithDrtRouteFactory;

import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * @author jbischoff
 */
public class RunTaxiPTIntermodalExample {
	public void run(URL configUrl, boolean OTFVis) {
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);


		SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
		srrConfig.setUseIntermodalAccessEgress(true);
		srrConfig.setIntermodalAccessEgressModeSelection(IntermodalAccessEgressModeSelection.RandomSelectOneModePerRoutingRequestAndDirection);

		IntermodalAccessEgressParameterSet paramSetTaxi = new IntermodalAccessEgressParameterSet();
		paramSetTaxi.setMode(TransportMode.taxi);
		paramSetTaxi.setInitialSearchRadius(15000);
		paramSetTaxi.setMaxRadius(20000);
		paramSetTaxi.setSearchExtensionRadius(0.1);
		srrConfig.addIntermodalAccessEgress(paramSetTaxi);

		IntermodalAccessEgressParameterSet paramSetWalk = new IntermodalAccessEgressParameterSet();
		paramSetWalk.setMode(TransportMode.walk);
		paramSetWalk.setInitialSearchRadius(1000);
		paramSetWalk.setMaxRadius(1000);
		paramSetWalk.setSearchExtensionRadius(0.1);
		srrConfig.addIntermodalAccessEgress(paramSetWalk);

		config.addModule(srrConfig);

		OTFVisConfigGroup otfvis = new OTFVisConfigGroup();
		otfvis.setDrawNonMovingItems(true);
		config.addModule(otfvis);

		// ---
		Scenario scenario = createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeTaxiConfigGroup.get(config)));

		controler.addOverridingModule(new MultiModeTaxiModule());

		controler.addOverridingModule(new SwissRailRaptorModule());
		if (OTFVis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}
}

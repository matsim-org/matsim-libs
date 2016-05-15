/* *********************************************************************** *
 * project: org.matsim.*
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


package playground.polettif.publicTransitMapping.workbench.zurich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.polettif.boescpa.lib.tools.fileCreation.F2LConfigGroup;
import playground.polettif.boescpa.lib.tools.fileCreation.F2LCreator;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.Collections;

public class RunZurichScenario {

	public static void main(String[] args) {

		String base = "E:/data/zurich/";

		final Config config = ConfigUtils.loadConfig(base + "config.xml", new BlackListedTimeAllocationMutatorConfigGroup(), new F2LConfigGroup());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// create link references for facilities
		Network streetNetwork = scenario.getNetwork();
		F2LCreator.createF2L(scenario.getActivityFacilities(), NetworkTools.filterNetworkByLinkMode(streetNetwork, Collections.singleton(TransportMode.car)), base + "facilitiesLinks.f2l");
		// This allows to get a log file containing the log messages happening
		// before controler init.
		OutputDirectoryLogging.catchLogEntries();

		final Controler controler = new Controler(scenario);

		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		connectFacilitiesWithNetwork(controler);

		// We use a time allocation mutator that allows to exclude certain activities.
		controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
		// We use a specific scoring function, that uses individual preferences for activity durations.
		controler.setScoringFunctionFactory(
				new IVTBaselineScoringFunctionFactoryCopy(controler.getScenario(),
						new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)));

		controler.run();
	}

	public static void connectFacilitiesWithNetwork(MatsimServices controler) {
		ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
		NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
		WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
		wcl.connectFacilitiesWithLinks(facilities, network);
	}
}
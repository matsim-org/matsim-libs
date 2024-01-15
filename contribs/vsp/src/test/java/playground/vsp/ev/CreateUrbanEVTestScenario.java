/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsp.ev;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

class CreateUrbanEVTestScenario {
	static Scenario createTestScenario(){
		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.timeProfiles = true;
		evConfigGroup.chargersFile = "chargers.xml";
		evConfigGroup.transferFinalSoCToNextIteration = true;

		//prepare config
		Config config = ConfigUtils.loadConfig("test/input/playground/vsp/ev/chessboard-config.xml", evConfigGroup);
		UrbanEVConfigGroup evReplanningCfg = new UrbanEVConfigGroup();
		config.addModule(evReplanningCfg );
		evReplanningCfg.setCriticalSOC(0.4);

		//TODO actually, should also work with all AccessEgressTypes but we have to check (write JUnit test)
		config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.none );

		//register charging interaction activities for car
		config.scoring().addActivityParams(
				new ScoringConfigGroup.ActivityParams( TransportMode.car + UrbanEVModule.PLUGOUT_INTERACTION).setScoringThisActivityAtAll(false ) );
		config.scoring().addActivityParams(
				new ScoringConfigGroup.ActivityParams( TransportMode.car + UrbanEVModule.PLUGIN_INTERACTION).setScoringThisActivityAtAll( false ) );
		config.network().setInputFile("1pctNetwork.xml");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(5);
		config.controller().setWriteEventsInterval(1);
		//set VehicleSource
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
		config.qsim().setEndTime(20*3600);

		//load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//manually insert car vehicle type with attributes (hbefa technology, initial energy etc....)
		RunUrbanEVExample.createAndRegisterPersonalCarAndBikeVehicles(scenario);
		return scenario;
	}
}

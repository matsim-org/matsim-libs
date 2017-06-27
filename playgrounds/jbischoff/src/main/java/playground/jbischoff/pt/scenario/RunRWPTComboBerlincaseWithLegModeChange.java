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
package playground.jbischoff.pt.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.config.*;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.AbstractModule;

import playground.jbischoff.pt.strategy.ChangeSingleLegModeWithPredefinedFromModesModule;

/**
 * @author  jbischoff
 *
 */
public class RunRWPTComboBerlincaseWithLegModeChange {
	public static void main(String[] args) {

//		 if (args.length!=1){
//		 throw new RuntimeException("Wrong arguments");
//		 }
		String configfile = "C:/Users/Joschka/Documents/shared-svn/studies/jbischoff/multimodal/berlin/input/modechoice/config_modechoice.xml";

		Config config = ConfigUtils.loadConfig(configfile, new TaxiConfigGroup(), new DvrpConfigGroup(), new TaxiFareConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		DvrpConfigGroup.get(config).setMode(TaxiModule.TAXI_MODE);

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());


		VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
		walk.setDistance(1000);
		walk.setTeleported(true);
		walk.setMode("walk");

		VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
		taxi.setDistance(20000);
		taxi.setTeleported(false);
		taxi.setMode("taxi");
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		vacfg.setAccessModeGroup(taxi);
		vacfg.setAccessModeGroup(walk);

		config.addModule(vacfg);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TaxiOutputModule());
        controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new ChangeSingleLegModeWithPredefinedFromModesModule());
		controler.run();

	}
}
